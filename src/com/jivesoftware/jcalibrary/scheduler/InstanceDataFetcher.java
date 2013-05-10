package com.jivesoftware.jcalibrary.scheduler;

import com.jivesoftware.jcalibrary.JiveInstancesRegistry;
import com.jivesoftware.jcalibrary.LibraryProps;
import com.jivesoftware.jcalibrary.api.JCAManager;
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
import java.util.Map;

public class InstanceDataFetcher implements Runnable{

    private Map<String,Pair<Long,String>> instanceMapping;
    private boolean firstRun = true;

    public InstanceDataFetcher() {
        
    }

    public Map<String,Pair<Long,String>> getMapping() {
        if (this.instanceMapping == null) {
            Map<String,Pair<Long,String>> instanceMapping = new HashMap<String,Pair<Long,String>>();
            final URL url = Thread.currentThread().getContextClassLoader().getResource("customerinstallationToHostMap.csv");
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
        try {
            for (JiveInstance jiveInstance : JCAManager.INSTANCE.fetchInstallations()) {
                JiveInstance registeredInstance = registry.getJiveInstance(jiveInstance.getCustomerInstallationId());
                if (registeredInstance == null) {
                    registry.addJiveInstance(jiveInstance);
                } else {
                    registeredInstance.importFrom(jiveInstance);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Map<String,Pair<Long,String>> instanceMapping = getMapping();
        if (firstRun) {
            firstRun = false;
//            doFirstRun(registry, instanceMapping);
        }
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

//    private void doFirstRun(JiveInstancesRegistry registry, Map<String,Pair<Long,String>> instanceMapping) {
//        for (Map.Entry<String,Pair<Long,String>> entry : instanceMapping.entrySet()) {
//            Pair<Long,String> pair = entry.getValue();
//            JiveInstance jiveInstance = registry.getJiveInstance(pair.getA());
//            if (jiveInstance == null) {
//                jiveInstance = new JiveInstance(pair.getA());
//                registry.addJiveInstance(jiveInstance);
//            }
//            jiveInstance.getNodeDetails(entry.getKey());
//        }
//    }

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
