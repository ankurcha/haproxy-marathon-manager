package com.brightcove.analytics.haproxy.api.model;

public class LoadbalancedApplication {

    String id;
    String template;
    String sslCertificate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getSslCertificate() {
        return sslCertificate;
    }

    public void setSslCertificate(String sslCertificate) {
        this.sslCertificate = sslCertificate;
    }

    @Override
    public String toString() {
        return "LoadbalancedApplication{" +
                "id='" + id + '\'' +
                ", template='" + template + '\'' +
                ", sslCertificate='" + sslCertificate + '\'' +
                '}';
    }
}