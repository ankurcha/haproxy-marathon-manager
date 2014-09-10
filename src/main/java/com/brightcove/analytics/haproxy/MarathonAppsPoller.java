package com.brightcove.analytics.haproxy;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import io.dropwizard.lifecycle.Managed;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class MarathonAppsPoller implements Managed {
    private static final Logger logger = LoggerFactory.getLogger(MarathonAppsPoller.class);
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Marathon client;
    private HaProxyTemplateRenderer renderer;
    private ManagerConfiguration config;
    private Long lastFileHash = null;
    private static final HashFunction hf = Hashing.sha256();

    public MarathonAppsPoller(ManagerConfiguration config, Marathon client, HaProxyTemplateRenderer renderer) {
        this.renderer = renderer;
        this.client = client;
        this.config = config;
    }

    @Override
    public void start() throws Exception {
        executor.scheduleWithFixedDelay(() -> {
            try {
                PortsAndFileHash portsAndFileHash = refreshConfig();
                Long hash = portsAndFileHash.hash;
                if (!hash.equals(lastFileHash)) {
                    // file has changed, reload config
                    reloadProxyConfig(StringUtils.join(portsAndFileHash.ports, ","));
                }
                lastFileHash = hash;
            } catch (IOException e) {
                logger.warn("Unable to refresh configuration", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    public void stop() throws Exception {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    void reloadProxyConfig(String ports) throws IOException {
        String cmd = config.getHaproxyConfigPath();
        Runtime.getRuntime().exec(cmd.replace("@PORTS@", ports));
    }

    /**
     * This method regenerated the haproxy config file based on the apps / templates but also
     *
     * @return Hash code of the current set of application
     * @throws IOException
     */
    PortsAndFileHash refreshConfig() throws IOException {
        String haproxyConfig = renderer.getBaseTemplate();
        // make list somewhat predictable by sorting the apps
        Stream<App> sortedAppsStream = client.getApps().getApps().stream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId()));
        // create a set of all possible ports that we may be touching
        Set<Integer> ports = Sets.newHashSet();
        sortedAppsStream.forEach(a -> {
            ports.addAll(a.getPorts());
            a.getTasks().forEach(t -> ports.addAll(t.getPorts()));
        });
        sortedAppsStream.map(renderer::renderApplication).forEach(haproxyConfig::concat);
        File configFile = new File(config.getHaproxyConfigPath());
        Long currentHash = hf.newHasher().putString(haproxyConfig, Charsets.UTF_8).hash().asLong();
        Files.write(haproxyConfig, configFile, Charsets.UTF_8);
        return new PortsAndFileHash(currentHash, ports);
    }

    private class PortsAndFileHash {
        Long hash;
        Set<Integer> ports;

        private PortsAndFileHash(Long hash, Set<Integer> ports) {
            this.hash = hash;
            this.ports = ports;
        }
    }

}
