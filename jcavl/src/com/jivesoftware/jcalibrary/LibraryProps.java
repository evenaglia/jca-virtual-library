package com.jivesoftware.jcalibrary;

import com.jivesoftware.jcalibrary.util.IOUtils;
import net.venaglia.common.util.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * User: ed
 * Date: 5/10/13
 * Time: 9:04 AM
 */
public class LibraryProps extends Properties {

    public static final String JCAVL_PROPERTIES_FILE = "jcavl.properties";

    public static final String NODEJS_PROXY_URL = "zenoss.nodejs.proxy.url";
    public static final String NODEJS_PROXY_AUTHENTICATION = "zenoss.nodejs.proxy.authentication";
    public static final String JCA_URL = "jca.url";
    public static final String JCA_CLIENT_VIEW_URL = "jca.client-view.url";

    public static final LibraryProps INSTANCE;

    private LibraryProps() throws IOException {
        File propsFile = new File(LibraryProps.JCAVL_PROPERTIES_FILE);
        if (!propsFile.exists()) {
            StringReader in = new StringReader(
                    "# Properties for connecting to the require servers\n" +
                    "zenoss.nodejs.proxy.url=https://jivedev-jcalibrary.nodejitsu.com/jcadata\n" +
                    "zenoss.nodejs.proxy.authentication=Basic amNhbGlicmFyeTpqMXYzcjBja3Mh\n" +
                    "#jca.url=http://jca-webapp.jcadev.eng.jiveland.com/admin/api/cloud/v1\n" +
                    "jca.url=https://cloud.jivesoftware.com/admin/api/cloud/v1\n" +
                    "jca.client-view.url=https://cloud.jivesoftware.com/admin/installation-view.jspa?customerInstallationId={id}\n"
            );
            FileWriter out = new FileWriter(propsFile);
            try {
                IOUtils.copy(in, out);
            } finally {
                IOUtils.closeQuietly(out);
                IOUtils.closeQuietly(in);
            }
        }
    }

    static {
        try {
            INSTANCE = new LibraryProps();
            INSTANCE.load(new FileReader(JCAVL_PROPERTIES_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private LoginForm loginForm = new LoginForm();

    public Pair<String,String> getJCACredentials() {
        return loginForm.getCredentials();
    }
}
