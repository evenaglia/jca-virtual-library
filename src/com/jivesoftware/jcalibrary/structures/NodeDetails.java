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
    private String type;
    private String status;
    private String url;

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
}
