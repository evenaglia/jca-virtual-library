package com.jivesoftware.jcalibrary.api;

import com.jivesoftware.jcalibrary.JiveInstancesRegistry;
import com.jivesoftware.jcalibrary.api.rest.CustomerInstallationResponse;
import com.jivesoftware.jcalibrary.api.rest.manager.RestClientManager;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JCAManager
 */
public class JCAManager {

    private static final String GET_ALL_INSTALLATIONS = "/users/installations";

    public List<JiveInstance> fetchInstallations() throws Exception{
        //TODO Check registry if exists

        RestClientManager restClientManager = new RestClientManager();
        List<JiveInstance> jiveInstances = new ArrayList<JiveInstance>();

        Response response = restClientManager.get(GET_ALL_INSTALLATIONS);

        if (Response.Status.OK.getStatusCode() == response.getStatus()) {
            jiveInstances = processGetAllInstallationsResponse(response);
        } else {
            System.out.println("SignupManager getCloudStatus response status: " + response.getStatus());
        }
        return jiveInstances;
    }


    private List<JiveInstance> processGetAllInstallationsResponse(Response response) throws Exception {
        String responseBody = IOUtils.toString((InputStream) response.getEntity());
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JiveInstance[] jiveInstanceArray= mapper.readValue(responseBody, JiveInstance[].class);

        List<JiveInstance> jiveInstances = Arrays.asList(jiveInstanceArray);

        for (JiveInstance jiveInstance : jiveInstances) {
            jiveInstance.setCustomerInstallationId(jiveInstance.getInstallation().getCustomerInstallationId());
        }

        return jiveInstances;
    }
}
