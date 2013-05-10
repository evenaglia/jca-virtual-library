package com.jivesoftware.jcalibrary;

import net.venaglia.realms.common.util.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * User: ed
 * Date: 5/10/13
 * Time: 9:04 AM
 */
public class LibraryProps extends Properties {

    public static final String NODEJS_PROXY_URL = "zenoss.nodejs.proxy.url";
    public static final String NODEJS_PROXY_AUTHENTICATION = "zenoss.nodejs.proxy.authentication";
    public static final String JCA_URL = "jca.url";

    public static final LibraryProps INSTANCE;

    static {
        INSTANCE = new LibraryProps();
        try {
            INSTANCE.load(new FileReader("jcavl.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private LoginForm loginForm = new LoginForm();

    public Pair<String,String> getJCACredentials() {
        return loginForm.getCredentials();
    }
}
