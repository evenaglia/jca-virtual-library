package com.jivesoftware.jcalibrary.api.rest.manager;

import com.jivesoftware.jcalibrary.LibraryProps;
import net.venaglia.realms.common.util.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * RestClientManager
 */
public class RestClientManager {

    private WebClient getWebClient() {
        LibraryProps libraryProps = new LibraryProps();
        Pair<String,String> jcaCredentials = libraryProps.getJCACredentials();

        String serverUrl = LibraryProps.INSTANCE.getProperty(LibraryProps.JCA_URL);
        String user = jcaCredentials.getA();
        String password = jcaCredentials.getB();
        return WebClient.create(serverUrl, user, password, null);
    }

    public Response get(String endpoint) {
        return get(endpoint, null, null);
    }

    public Response get(String endpoint, String queryName, String queryValue) {
        // timeout default is 30 seconds
        WebClient webClient = getWebClient();
        webClient.path(endpoint);
        if (null != queryName && null != queryValue) {
            webClient.query(queryName, queryValue);
        }
        webClient.accept(MediaType.APPLICATION_JSON);
        webClient.type(MediaType.APPLICATION_JSON);
        Response response = webClient.get();
        return response;
    }

    public Response post(String endpoint, String jsonRepresentation) {
         // timeout default is 30 seconds
        WebClient webClient = getWebClient();
        webClient.path(endpoint);
        webClient.accept(MediaType.APPLICATION_JSON);
        webClient.type(MediaType.APPLICATION_JSON);
        Response response = webClient.post(jsonRepresentation);
        return response;
    }
}
