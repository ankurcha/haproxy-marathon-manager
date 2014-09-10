package com.brightcove.analytics.haproxy;

import com.brightcove.analytics.haproxy.api.AppsResource;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;

public class ManagerApplication extends Application<ManagerConfiguration> {

    public static void main(String[] args) throws Exception {
        new ManagerApplication().run(args);
    }

    @Override
    public String getName() {
        return "haproxy-marathon-manager";
    }

    @Override
    public void initialize(Bootstrap<ManagerConfiguration> managerConfigurationBootstrap) {
        // TODO: Complete me ...
    }

    @Override
    public void run(ManagerConfiguration config, Environment environment) throws Exception {
        Marathon client = MarathonClient.getInstance(config.getMarathonEndPoint());
        CuratorFramework framework = CuratorFrameworkFactory.newClient(
                config.getZookeeperConnection(),
                new BoundedExponentialBackoffRetry(5000, 30000, 20));

        ZookeeperStore store = new ZookeeperStore(framework, config.getZookeeperBasePath());
        HaProxyTemplateRenderer renderer = new HaProxyTemplateRenderer(config, store);
        MarathonAppsPoller poller = new MarathonAppsPoller(config, client, renderer);
        AppsResource appsResource = new AppsResource(store);

        // register all managed objects
        environment.healthChecks().register("zookeeper", new ZookeeperHealthCheck(framework));
        environment.lifecycle().manage(store);
        environment.lifecycle().manage(poller);
        environment.jersey().register(appsResource);

    }
}
