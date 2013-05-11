package com.jivesoftware.jcalibrary.api.rest.manager;

import net.venaglia.realms.common.util.Ref;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 5/10/13
* Time: 10:11 PM
* To change this template use File | Settings | File Templates.
*/
public class Response {

    private final int status;
    private final Ref<String> body;

    public Response(javax.ws.rs.core.Response response) {
        this.status = response.getStatus();
        String body = "";
        try {
            body = IOUtils.toString((InputStream)response.getEntity());
        } catch (IOException e) {
            // don't care
        }
        final String b = body;
        this.body = new Ref<String>() {
            @Override
            public String get() {
                return b;
            }
        };
    }

    public Response(int status, Ref<String> body) {
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body.get();
    }
}
