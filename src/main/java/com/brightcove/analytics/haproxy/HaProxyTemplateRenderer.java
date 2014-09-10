package com.brightcove.analytics.haproxy;

import com.brightcove.analytics.haproxy.api.model.LoadbalancedApplication;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import mesosphere.marathon.client.model.v2.App;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class HaProxyTemplateRenderer {
    private static final Logger logger = LoggerFactory.getLogger(HaProxyTemplateRenderer.class);
    private static final MustacheFactory mf = new DefaultMustacheFactory();

    private String baseTemplate;
    private String sslCertsPath;
    private ZookeeperStore store;

    public HaProxyTemplateRenderer(ManagerConfiguration config, ZookeeperStore store) throws IOException {
        this.store = store;
        this.sslCertsPath = config.getSslCertsPath();
        this.baseTemplate = StringUtils.join(Files.readLines(new File(config.getHaproxyBaseTemplatePath()), Charsets.UTF_8), "\n");
        this.baseTemplate = Resources.toString(this.getClass().getResource("/haproxy_template.cfg.tmpl"), Charsets.UTF_8);
    }

    public String renderApplication(App app) {
        AppConfig config = new AppConfig(app, null);
        StringWriter writer = new StringWriter();
        // add application configs
        try {
            LoadbalancedApplication lbApp = store.get(app.getId());
            if (lbApp != null) {
                try {
                    config.certPath = writeSSLCert(app.getId() + ".pem", lbApp.getSslCertificate());
                } catch (IOException e) {
                    throw new RuntimeException("Unable to write ssl certificate", e);
                }
                mf.compile(new StringReader(lbApp.getTemplate()), app.getId() + "-template").execute(writer, config);
            }
            return writer.toString();
        } catch (Exception e) {
            logger.warn("Unable to render template for application: " + app.getId());
        }
        return null;
    }

    String writeSSLCert(String fileName, String sslCert) throws IOException {
        File file = new File(sslCertsPath, fileName);
        Files.write(sslCert, file, Charsets.UTF_8);
        return file.getAbsolutePath();
    }

    String getBaseTemplate() {
        return baseTemplate;
    }


    // Internal representation of a Marathon Application Configuration
    class AppConfig {
        String id;
        int port;
        List<Task> tasks;
        String certPath;

        class Task {
            String id;
            int port;
            String host;
        }

        public AppConfig(App app, String certPath) {
            checkArgument(app.getPorts() != null && app.getPorts().size() >= 1);

            this.id = app.getId();
            this.certPath = certPath;
            this.port = app.getPorts().get(0);
            this.tasks = new ArrayList<>();
            app.getTasks().stream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId()))
                    .forEach(task -> {
                        String id = task.getId();
                        String host = task.getHost();
                        for (Integer port : task.getPorts()) {
                            Task t = new Task();
                            t.id = id;
                            t.host = host;
                            t.port = port;
                            tasks.add(t);
                        }
                    });
        }
    }

}
