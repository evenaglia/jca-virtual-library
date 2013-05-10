package com.jivesoftware.jcalibrary.api.rest;

import com.jivesoftware.jcalibrary.structures.JiveInstance;

import java.util.List;

/**
 * CustomerInstallationResponse
 */
public class CustomerInstallationResponse {
    private List<JiveInstance> jiveInstances;

    public List<JiveInstance> getJiveInstances() {
        return jiveInstances;
    }

    public void setJiveInstances(List<JiveInstance> jiveInstances) {
        this.jiveInstances = jiveInstances;
    }
}
