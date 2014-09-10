package com.brightcove.analytics.haproxy;

import com.brightcove.analytics.haproxy.api.model.LoadbalancedApplication;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Task;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HaProxyTemplateRendererTest {

    HaProxyTemplateRenderer renderer;
    ManagerConfiguration config;
    String filePath;
    LoadbalancedApplication lb;

    @Before
    public void setUp() throws Exception {
        config = new ManagerConfiguration();
        File baseTemplate = new File(Files.createTempDir(), "haproxy_base_template.cfg.tmpl");
        Files.write("global\n" +
                "  daemon\n" +
                "  log 127.0.0.1 local0\n" +
                "  log 127.0.0.1 local1 notice\n" +
                "  maxconn 16384\n" +
                "\n" +
                "defaults\n" +
                "  log              global\n" +
                "  retries          3\n" +
                "  maxconn          10000\n" +
                "  timeout connect  50000\n" +
                "  timeout client   50000\n" +
                "  timeout server   50000\n" +
                "\n" +
                "listen stats\n" +
                "  bind 127.0.0.1:9090\n" +
                "  balance\n" +
                "  mode http\n" +
                "  stats enable\n" +
                "  stats auth admin:admin\n" +
                "\n", baseTemplate, Charsets.UTF_8);
        config.setHaproxyBaseTemplatePath(baseTemplate.getAbsolutePath());
        config.setSslCertsPath(Files.createTempDir().getAbsolutePath());
        ZookeeperStore store = new ZookeeperStore(null, null) {
            @Override
            public String put(LoadbalancedApplication app) throws Exception {
                return null;
            }

            @Override
            public LoadbalancedApplication get(String appId) throws Exception {
                return lb;
            }
        };

        renderer = new HaProxyTemplateRenderer(config, store) {
            @Override
            String writeSSLCert(String fileName, String sslCert) throws IOException {
                filePath = super.writeSSLCert(fileName, sslCert);
                return filePath;
            }
        };
    }

    @Test
    public void testRenderApplication_unknown() throws Exception {
        App app = new App();
        app.setId("rolling_mongos");
        app.setPorts(new ArrayList<Integer>() {{
            add(1234);
        }});
        List<Task> tasks = new ArrayList<Task>() {{
            add(new Task() {{
                setId("task-rm-0");
                setHost("host1");
                setPorts(new ArrayList<Integer>() {{
                    add(4567);
                }});
                setAppId("rolling-mongos");
            }});
        }};
        app.setTasks(tasks);
        String renderedConfig = renderer.renderApplication(app);
        Assert.assertEquals("Unknown applications should not result in any configuration", "", renderedConfig);
    }

    @Test
    public void testRenderApplication_known() throws Exception {
        lb = new LoadbalancedApplication();
        lb.setId("api");
        lb.setTemplate(
                "backend api-backend\n" +
                        "  mode http\n" +
                        "  balance roundrobin\n" +
                        "  option httpchk GET /analytics-apu/private/status\n" +
                        "  option httplog\n" +
                        "  {{#tasks}}\n" +
                        "  server {{id}} {{host}}:{{port}} weight 1 maxconn 4096 check\n" +
                        "  {{/tasks}}\n" +
                        "frontend api-http\n" +
                        "  bind data.brightcove.com:80\n" +
                        "  reqadd X-Forwarded-Proto:\\ http\n" +
                        "  default_backend api-backend\n" +
                        "frontend api-https\n" +
                        "  bind data.brightcove.com:443 ssl crt {{certPath}}\n" +
                        "  reqadd X-Forwarded-Proto:\\ https\n" +
                        "  default_backend api-backend\n");
        lb.setSslCertificate("FOOBAR_CERTIFICATE");
        App app = new App();
        app.setId("api");
        app.setPorts(new ArrayList<Integer>() {{
            add(1234);
        }});
        List<Task> tasks = new ArrayList<Task>() {{
            add(new Task() {{
                setId("task-api-0");
                setHost("host1");
                setPorts(new ArrayList<Integer>() {{
                    add(4567);
                }});
                setAppId("api");
            }});
        }};
        app.setTasks(tasks);
        String renderedConfig = renderer.renderApplication(app);
        Assert.assertEquals("Known application should produce rendered template",
                "backend api-backend\n" +
                        "  mode http\n" +
                        "  balance roundrobin\n" +
                        "  option httpchk GET /analytics-apu/private/status\n" +
                        "  option httplog\n" +
                        "  server task-api-0 host1:4567 weight 1 maxconn 4096 check\n" +
                        "frontend api-http\n" +
                        "  bind data.brightcove.com:80\n" +
                        "  reqadd X-Forwarded-Proto:\\ http\n" +
                        "  default_backend api-backend\n" +
                        "frontend api-https\n" +
                        "  bind data.brightcove.com:443 ssl crt " + filePath + "\n" +
                        "  reqadd X-Forwarded-Proto:\\ https\n" +
                        "  default_backend api-backend\n", renderedConfig);
        Assert.assertEquals("FOOBAR_CERTIFICATE", Files.readFirstLine(new File(filePath), Charsets.UTF_8));
    }
}