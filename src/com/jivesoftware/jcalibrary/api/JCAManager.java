package com.jivesoftware.jcalibrary.api;

import com.jivesoftware.jcalibrary.api.rest.InstallationPageViewBean;
import com.jivesoftware.jcalibrary.api.rest.manager.Response;
import com.jivesoftware.jcalibrary.api.rest.manager.RestClientManager;
import com.jivesoftware.jcalibrary.api.rest.manager.Status;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JCAManager
 */
public class JCAManager {

    private static final String GET_ALL_INSTALLATIONS = "/users/installations";
    private static final String GET_PAGEVIEWAS_BY_HOUR_FOR_LAST_DAY = "/reporting/pageviews/installations";

    public static final JCAManager INSTANCE = new JCAManager();

    private JCAManager() {
    }

    public List<InstallationPageViewBean> fetchPageViews(long customerInstallationId) throws Exception {

        RestClientManager restClientManager = RestClientManager.INSTANCE;
        List<InstallationPageViewBean> pageViews = new ArrayList<InstallationPageViewBean>();

        Response response = restClientManager.get(GET_PAGEVIEWAS_BY_HOUR_FOR_LAST_DAY + "/" + customerInstallationId);

        if (Status.OK.getCode() == response.getStatus()) {
            pageViews = processGetPageViewsByDay(response);
        } else {
            System.out.println("SignupManager getCloudStatus response status: " + response.getStatus());
        }
        return pageViews;
    }

    public List<JiveInstance> fetchInstallations() throws Exception {

        RestClientManager restClientManager = RestClientManager.INSTANCE;
        List<JiveInstance> jiveInstances = new ArrayList<JiveInstance>();

        Response response = restClientManager.get(GET_ALL_INSTALLATIONS, "timePeriod", "DAY");

        if (Status.OK.getCode() == response.getStatus()) {
            jiveInstances = processGetAllInstallationsResponse(response);
        } else {
            System.out.println("JCAManager getCloudStatus response status: " + response.getStatus());
        }
        return jiveInstances;
    }


    private List<JiveInstance> processGetAllInstallationsResponse(Response response) throws Exception {
        String responseBody = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JiveInstance[] jiveInstanceArray = mapper.readValue(responseBody, JiveInstance[].class);
        List<JiveInstance> jiveInstances = Arrays.asList(jiveInstanceArray);

        for (JiveInstance jiveInstance : jiveInstances) {
            jiveInstance.setCustomerInstallationId(jiveInstance.getInstallation().getCustomerInstallationId());
        }

        return jiveInstances;
    }

    private List<InstallationPageViewBean> processGetPageViewsByDay(Response response) throws Exception {
        String responseBody = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InstallationPageViewBean[] jivePageViewArray = mapper.readValue(responseBody, InstallationPageViewBean[].class);
        List<InstallationPageViewBean> pageViews = Arrays.asList(jivePageViewArray);

        return pageViews;
    }
    
    private Map<Long,int[]> customerIcons;

    public int[] getCustomerIcons(long customerId) {
        if (customerIcons == null) {
            customerIcons = new HashMap<Long,int[]>();
            URL url = Thread.currentThread().getContextClassLoader().getResource("customerInstallationToIconsMap.csv");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                String line = reader.readLine();
                line = reader.readLine();
                while (line != null) {
                    String[] cols = line.split(",");
                    int icon1 = Integer.parseInt(cols[1]);
                    int icon2 = Integer.parseInt(cols[2]);
                    customerIcons.put(Long.parseLong(cols[0]), new int[]{ icon1, icon2 });
                    line = reader.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return customerIcons.get(customerId);
    }
}
