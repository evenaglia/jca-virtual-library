package com.jivesoftware.jcalibrary.scheduler;

import com.jivesoftware.jcalibrary.JiveInstancesRegistry;
import com.jivesoftware.jcalibrary.LibraryProps;
import com.jivesoftware.jcalibrary.api.JCAManager;
import com.jivesoftware.jcalibrary.api.rest.InstallationPageViewBean;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.NodeDetails;
import net.venaglia.realms.common.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class InstanceDataFetcher implements Runnable{

    private Map<String,Pair<Long,String>> instanceMapping;
    private boolean firstRun = true;
    private boolean firstRunCompleted = false;

    public InstanceDataFetcher() {
        
    }

    public Map<String,Pair<Long,String>> getMapping() {
        if (this.instanceMapping == null) {
            Map<String,Pair<Long,String>> instanceMapping = new HashMap<String,Pair<Long,String>>();
            final URL url = Thread.currentThread().getContextClassLoader().getResource("customerInstallationToHostMap.csv");
            BufferedReader rd;
            URLConnection conn;
            String line;
            boolean skipFirst=true;
            try {
                conn = url.openConnection();
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = rd.readLine()) != null) {
                    if (skipFirst) {
                        skipFirst = false;
                    } else {
                        String[] mappingData = line.split(",");
                        Long id = Long.valueOf(mappingData[0]);
                        String host = mappingData[1];
                        String type = mappingData[2];
                        instanceMapping.put(host,new Pair<Long,String>(id, type));
                    }
                }
                rd.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.instanceMapping = instanceMapping;
        }
        return instanceMapping;
    }

    @Override
    public void run() {
        JiveInstancesRegistry registry = JiveInstancesRegistry.getInstance();
        if (firstRun) {
            firstRun = false;
            doFirstRun(registry);
            firstRunCompleted = true;
            System.out.println("First run completed");
        } else if (!firstRunCompleted) {
            return;
        }
        Map<String,Pair<Long,String>> instanceMapping = getMapping();
        String nodeJSUrl = LibraryProps.INSTANCE.getProperty(LibraryProps.NODEJS_PROXY_URL);
        String nodeJSAuth = LibraryProps.INSTANCE.getProperty(LibraryProps.NODEJS_PROXY_AUTHENTICATION);
        String fetchedData = fetchData(nodeJSUrl, nodeJSAuth);
        try {
            JSONObject fetchedDataJSONObject = new JSONObject("{list:"+fetchedData+"}");
            JSONArray jcaInstancesList = (JSONArray)fetchedDataJSONObject.get("list");
            for (int i = 0; i< jcaInstancesList.length(); i++) {
                try {
                    JSONObject jcaInstanceJSONObject = (JSONObject)jcaInstancesList.get(i);
                    String stringId = (String)jcaInstanceJSONObject.get("id");
                    Date timestamp = Timestamp.valueOf(((String)jcaInstanceJSONObject.get("timestamp")).replace("T"," "));
                    String detail = (String)jcaInstanceJSONObject.get("detail");
//                    String id_type = (String)jcaInstanceJSONObject.get("id_type");
                    String status = (String)jcaInstanceJSONObject.get("status");
                    Pair<Long,String> pair = instanceMapping.get(stringId);
                    Long id = pair.getA();
                    String type = pair.getB();

                    if (id != null) {
                        JiveInstance jiveInstance = registry.getJiveInstance(id);
                        if (jiveInstance != null) {
                            NodeDetails nodeDetails = jiveInstance.getNodeDetails(stringId);
                            nodeDetails.setTimestamp(timestamp);
                            nodeDetails.setDetails(detail);
                            nodeDetails.setType(type);
                            nodeDetails.setStatus(status);
                            nodeDetails.setUrl(stringId);

                            // TODO Replace fake data with real data
                            Random r = new Random();
                            nodeDetails.setActiveConnections(Math.abs((int)Math.round(r.nextGaussian() * 25)));
                            nodeDetails.setActiveSessions(Math.abs((int)Math.round(r.nextGaussian() * 25)));
                            nodeDetails.setLoadAverage(Math.abs((float)(r.nextGaussian() * 2)));
                        }
                    }

                } catch (IllegalArgumentException e) {
                    // problem with timestamp -just skip it
                } catch (JSONException e) {
                    // problem with getting attribute? - just skip it
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void doFirstRun(JiveInstancesRegistry registry) {
        try {
            for (JiveInstance jiveInstance : JCAManager.INSTANCE.fetchInstallations()) {
                long installationId = jiveInstance.getCustomerInstallationId();
                JiveInstance registeredInstance = registry.getJiveInstance(installationId);
                if (registeredInstance == null) {
                    registeredInstance = jiveInstance;
                    registry.addJiveInstance(jiveInstance);
                } else {
                    registeredInstance.importFrom(jiveInstance);
                }
                long sum = 0;
                for (InstallationPageViewBean view : JCAManager.INSTANCE.fetchPageViews(installationId)) {
                    for (Long c : view.getPageViews()) {
                        sum += c;
                    }
                }
                registeredInstance.setPageViews(sum);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String fetchData(String serverUrl, String auth) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        StringBuilder result = new StringBuilder();
        try {
            url = new URL(serverUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty  ("Authorization", auth);
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static void main(String[] args) {
        InstanceDataFetcher instanceDataFetcher = new InstanceDataFetcher();
        instanceDataFetcher.run();
    }
}
