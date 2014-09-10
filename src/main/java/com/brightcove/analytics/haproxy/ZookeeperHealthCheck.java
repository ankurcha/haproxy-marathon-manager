package com.brightcove.analytics.haproxy;

import com.codahale.metrics.health.HealthCheck;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZookeeperHealthCheck extends HealthCheck {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperHealthCheck.class);

    CuratorFramework framework;

    public ZookeeperHealthCheck(CuratorFramework framework) {
        this.framework = framework;
    }

    @Override
    protected Result check() throws Exception {
        try {
            if (framework.getZookeeperClient().isConnected()) {
                return Result.healthy();
            }
        } catch (Exception e) {
            logger.warn("Unable to determine zookeeper connection status", e);
        }
        return Result.unhealthy("zookeeper client not connected");
    }
}
