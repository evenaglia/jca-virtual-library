package net.venaglia.realms.common;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 2/27/13
 * Time: 7:29 PM
 */
public enum Configuration {

    DATABASE_DIRECTORY("database.directory");

    private static final Properties PROPERTIES;
    private static final Pattern MATCH_TRUE = Pattern.compile("true|t|yes|y|-1|1", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATCH_FALSE = Pattern.compile("false|f|no|n|0", Pattern.CASE_INSENSITIVE);

    static {
        Properties props = new Properties();
        try {
            props.load(Configuration.class.getResourceAsStream("default.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Configuration c : values()) {
            String value = System.getProperty(c.propName);
            if (value != null) {
                props.setProperty(c.propName, value);
            }
        }
        PROPERTIES = new Properties(props);
    }

    private final String propName;

    private Configuration(String propName) {
        this.propName = propName;
    }

    public Boolean getBoolean() {
        String value = PROPERTIES.getProperty(propName);
        if (value != null) {
            if (MATCH_TRUE.matcher(value).matches()) {
                return true;
            }
            if (MATCH_FALSE.matcher(value).matches()) {
                return false;
            }
        }
        return null;
    }

    public boolean getBoolean(boolean defaultValue) {
        String value = PROPERTIES.getProperty(propName);
        if (value != null) {
            if (MATCH_TRUE.matcher(value).matches()) {
                return true;
            }
            if (MATCH_FALSE.matcher(value).matches()) {
                return false;
            }
        }
        return defaultValue;
    }

    public Integer getInteger() {
        try {
            String value = PROPERTIES.getProperty(propName);
            return value == null ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    public int getInteger(int defaultValue) {
        try {
            return Integer.parseInt(PROPERTIES.getProperty(propName));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getString() {
        return PROPERTIES.getProperty(propName);
    }

    public String getString(String defaultValue) {
        return PROPERTIES.getProperty(propName, defaultValue);
    }

    public void override(String value) {
        if (value == null) {
            PROPERTIES.remove(propName);
        } else {
            PROPERTIES.setProperty(propName, value);
        }
    }

    @Override
    public String toString() {
        String value = getString();
        return value == null ? String.format("%s=null", propName) : String.format("%s=\"%s\"", propName, value);
    }
}
