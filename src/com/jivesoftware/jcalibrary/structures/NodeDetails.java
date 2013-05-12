package com.jivesoftware.jcalibrary.structures;

import java.util.Date;

/**
* User: ed
* Date: 5/10/13
* Time: 2:45 PM
*/
public class NodeDetails {
    private Date timestamp;
    private String details;
    private String type; // thunder, dbvirtual, cache, dedicatedsearch, dbanalytics, webapp, dbeae, eaeservice
    private String status;
    private String url;

    private long activeConnections;
    private long activeSessions;
    private long loadAverage;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(long activeConnections) {
        this.activeConnections = activeConnections;
    }

    public long getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(long activeSessions) {
        this.activeSessions = activeSessions;
    }

    public long getLoadAverage() {
        return loadAverage;
    }

    public void setLoadAverage(long loadAverage) {
        this.loadAverage = loadAverage;
    }
}
