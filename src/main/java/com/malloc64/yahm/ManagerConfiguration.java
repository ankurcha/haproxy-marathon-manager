package com.malloc64.yahm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;


public class ManagerConfiguration extends Configuration {

    @NotEmpty
    private String marathonEndPoint = "http://localhost:8080";

    // this command is borrowed from https://medium.com/@Drew_Stokes/actual-zero-downtime-with-haproxy-18318578fde6
    @NotEmpty
    private String haproxyReloadCommand = "iptables -I INPUT -p tcp -m multiport —dports @PORTS@ —syn -j DROP && " +
            "sleep 0.5 && " +
            "service haproxy reload && " +
            "iptables -D INPUT -p -tcp -m multiport —dports @PORTS@ —syn -j DROP";

    @NotEmpty
    private String haproxyConfigPath = "/etc/haproxy/haproxy.cfg";

    @NotEmpty
    private String sslCertsPath = "/etc/haproxy/certs";

    @NotEmpty
    private String zookeeperConnection = "zk://localhost:2818";

    @NotEmpty
    private String zookeeperBasePath = "/marathon-haproxy/state";

    @NotEmpty
    String haproxyBaseTemplatePath = "/etc/haproxy_template.cfg.tmpl";

    @JsonProperty
    public String getMarathonEndPoint() {
        return marathonEndPoint;
    }

    @JsonProperty
    public void setMarathonEndPoint(String marathonEndPoint) {
        this.marathonEndPoint = marathonEndPoint;
    }

    @JsonProperty
    public String getHaproxyReloadCommand() {
        return haproxyReloadCommand;
    }

    @JsonProperty
    public void setHaproxyReloadCommand(String haproxyReloadCommand) {
        this.haproxyReloadCommand = haproxyReloadCommand;
    }

    @JsonProperty
    public String getHaproxyConfigPath() {
        return haproxyConfigPath;
    }

    @JsonProperty
    public void setHaproxyConfigPath(String haproxyConfigPath) {
        this.haproxyConfigPath = haproxyConfigPath;
    }

    @JsonProperty
    public String getSslCertsPath() {
        return sslCertsPath;
    }

    @JsonProperty
    public void setSslCertsPath(String sslCertsPath) {
        this.sslCertsPath = sslCertsPath;
    }

    @JsonProperty
    public String getZookeeperConnection() {
        return zookeeperConnection;
    }

    @JsonProperty
    public void setZookeeperConnection(String zookeeperConnection) {
        this.zookeeperConnection = zookeeperConnection;
    }

    @JsonProperty
    public String getHaproxyBaseTemplatePath() {
        return haproxyBaseTemplatePath;
    }

    @JsonProperty
    public void setHaproxyBaseTemplatePath(String haproxyBaseTemplatePath) {
        this.haproxyBaseTemplatePath = haproxyBaseTemplatePath;
    }

    @JsonProperty
    public String getZookeeperBasePath() {
        return zookeeperBasePath;
    }

    @JsonProperty
    public void setZookeeperBasePath(String zookeeperBasePath) {
        this.zookeeperBasePath = zookeeperBasePath;
    }
}
