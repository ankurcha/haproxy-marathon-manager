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
        Assert.assertEquals(
                "listen rolling_mongos\n" +
                        "  bind 0.0.0.0:1234\n" +
                        "  mode tcp\n" +
                        "  option tcplog\n" +
                        "  balance leastconn\n" +
                        "  server task-rm-0 host1:4567 check\n", renderedConfig);
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
        Assert.assertEquals(
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