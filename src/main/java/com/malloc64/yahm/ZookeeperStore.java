package com.malloc64.yahm;

import com.malloc64.yahm.api.model.LoadbalancedApplication;
import com.google.common.base.Charsets;
import io.dropwizard.lifecycle.Managed;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.PathUtils;
import org.apache.zookeeper.CreateMode;

public class ZookeeperStore implements Managed {

    CuratorFramework framework;
    String basePath;

    public ZookeeperStore(CuratorFramework framework, String basePath) {
        this.framework = framework;
        this.basePath = basePath;
    }

    public String put(LoadbalancedApplication app) throws Exception {
        String nodePath = basePath + app.getId();
        PathUtils.validatePath(nodePath);
        String templatePath = nodePath + "/template";
        String sslCertPath = nodePath + "/sslCertificate";
        PathUtils.validatePath(templatePath);
        PathUtils.validatePath(sslCertPath);

        // new node create, else update
        if (framework.checkExists().forPath(nodePath) == null) {
            framework.inTransaction()
                    .create().withMode(CreateMode.PERSISTENT).forPath(nodePath).and()
                    .create().withMode(CreateMode.PERSISTENT).forPath(templatePath, app.getTemplate().getBytes(Charsets.UTF_8)).and()
                    .create().withMode(CreateMode.PERSISTENT).forPath(sslCertPath, app.getSslCertificate().getBytes(Charsets.UTF_8)).and()
                    .commit();
        } else {
            framework.inTransaction()
                    .setData().forPath(templatePath, app.getTemplate().getBytes(Charsets.UTF_8)).and()
                    .setData().forPath(sslCertPath, app.getSslCertificate().getBytes(Charsets.UTF_8)).and()
                    .commit();
        }
        // create the node in a single transaction

        return app.getId();
    }

    public LoadbalancedApplication get(String appId) throws Exception {
        String nodePath = basePath + appId;
        PathUtils.validatePath(nodePath);
        String templatePath = nodePath + "/template";
        String sslCertPath = nodePath + "/sslCertificate";
        LoadbalancedApplication app = new LoadbalancedApplication();
        if (framework.checkExists().forPath(nodePath) == null) {
            return null;
        }
        app.setId(appId);
        if (framework.checkExists().forPath(templatePath) != null) {
            app.setTemplate(new String(framework.getData().forPath(templatePath)));
        }
        if (framework.checkExists().forPath(sslCertPath) != null) {
            app.setTemplate(new String(framework.getData().forPath(sslCertPath)));
        }
        return app;
    }

    public void delete(String appId) throws Exception {
        String nodePath = basePath + appId;
        PathUtils.validatePath(nodePath);
        framework.delete().forPath(nodePath);
    }

    @Override
    public void start() throws Exception {
        framework.start();
    }

    @Override
    public void stop() throws Exception {
        framework.close();
    }
}
