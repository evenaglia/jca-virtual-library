package net.venaglia.realms.common;

import net.venaglia.realms.common.map.PropertyStore;
import net.venaglia.realms.common.map.WorldMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 2/27/13
 * Time: 7:29 PM
 */
public enum Configuration {

    // GEOSPEC must be first, it drives many others and has special handling
    GEOSPEC("geospec", Storage.PERSISTENT) {
        @Override
        public boolean isWritable() {
            return false;
        }
    },

    // persistence properties, read from a file
    DATA_STORE_CLASS("datastore.class", Storage.IMMUTABLE),
    DATABASE_DIRECTORY("database.directory", Storage.IMMUTABLE),
    DATABASE_HARMLESS("database.harmless", Storage.IMMUTABLE),
    JDBC_DRIVER("database.jdbc.driver", Storage.IMMUTABLE),
    JDBC_URL("database.jdbc.url", Storage.IMMUTABLE),
    JDBC_USERNAME("database.jdbc.username", Storage.IMMUTABLE),
    JDBC_PASSWORD("database.jdbc.password", Storage.IMMUTABLE),
    JDBC_POOL_SIZE("database.jdbc.poolSize", Storage.IMMUTABLE),

    // regular properties
    THING_CHECKPOINT_SIZE("things.dirty.checkpoint.size", Storage.PERSISTENT),
    THING_CHECKPOINT_WAIT("things.dirty.checkpoint.wait", Storage.PERSISTENT);

    private enum Storage {
        IMMUTABLE, // read from a file at startup, not editable at runtime
        TRANSIENT, // mutable at runtime, reset upon restart
        PERSISTENT // mutable at runtime, persisted across restarts
    }

    private static final Properties PROPERTIES;
    private static final Properties MEM_PROPERTIES;
    private static final Properties JVM_PROPERTIES;
    private static final Properties DB_PROPERTIES;
    private static final Properties FILE_PROPERTIES;

    private static final Pattern MATCH_TRUE = Pattern.compile("true|t|yes|y|-1|1", Pattern.CASE_INSENSITIVE);
    private static final Pattern MATCH_FALSE = Pattern.compile("false|f|no|n|0", Pattern.CASE_INSENSITIVE);
    private static final AtomicReference<PropertyStore> PROPERTY_STORE = new AtomicReference<PropertyStore>();

    static {
        FILE_PROPERTIES = new Properties();
        DB_PROPERTIES = new Properties(FILE_PROPERTIES);
        JVM_PROPERTIES = new Properties(DB_PROPERTIES);
        MEM_PROPERTIES = new Properties(JVM_PROPERTIES);
        PROPERTIES = new Properties(MEM_PROPERTIES);
        try {
            FILE_PROPERTIES.load(Configuration.class.getResourceAsStream("default.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (Configuration c : values()) {
            String value = System.getProperty(c.propName);
            if (value != null) {
                JVM_PROPERTIES.setProperty(c.propName, value);
            }
        }
    }

    private final String propName;
    private final Storage storage;

    private Configuration(String propName, Storage storage) {
        this.propName = propName;
        this.storage = storage;
    }

    public boolean isWritable() {
        return storage != Storage.IMMUTABLE && !JVM_PROPERTIES.containsKey(propName);
    }

    public boolean isPersistent() {
        return storage == Storage.PERSISTENT;
    }

    public boolean isRequired() {
        return FILE_PROPERTIES.containsKey(propName);
    }

    public Boolean getBoolean() {
        ensureLoaded();
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

    private void ensureLoaded() {
        if (PROPERTY_STORE.get() == null && storage == Storage.PERSISTENT) {
            DeferredPropertyStore deferredPropertyStore = new DeferredPropertyStore();
            if (!PROPERTY_STORE.compareAndSet(null, deferredPropertyStore)) {
                return;
            }
            PropertyStore propertyStore = WorldMap.INSTANCE.get().getPropertyStore();
            PROPERTY_STORE.set(propertyStore);
            deferredPropertyStore.setRealPropertyStore(propertyStore);
            int count = 0;
            for (Configuration configuration : values()) {
                if (configuration.isPersistent() && configuration.isWritable() || configuration == GEOSPEC) {
                    String value = propertyStore.get(configuration.propName);
                    if (value != null) {
                        DB_PROPERTIES.put(configuration.propName, value);
                        count++;
                    }
                }
            }
            System.out.println("Loaded " + count + " configuration properties from persistent storage");
        }
    }

    public boolean getBoolean(boolean defaultValue) {
        ensureLoaded();
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

    public void setBoolean(boolean value) {
        setString(value ? "t" : "f");
    }

    public Integer getInteger() {
        ensureLoaded();
        try {
            String value = PROPERTIES.getProperty(propName);
            return value == null ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    public int getInteger(int defaultValue) {
        ensureLoaded();
        try {
            return Integer.parseInt(PROPERTIES.getProperty(propName));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void setInteger(int value) {
        setString(Integer.toString(value));
    }

    public String getString() {
        ensureLoaded();
        return PROPERTIES.getProperty(propName);
    }

    public String getString(String defaultValue) {
        ensureLoaded();
        return PROPERTIES.getProperty(propName, defaultValue);
    }

    public void setString(String value) {
        ensureLoaded();
        if (!isWritable()) {
            throw new UnsupportedOperationException();
        }
        if (storage == Storage.PERSISTENT) {
            if (value != null) {
                if (value.length() > 256) {
                    throw new IllegalArgumentException("Value too long, must be less than 256 characters: length=" + value.length());
                }
                PROPERTY_STORE.get().set(propName, value);
                DB_PROPERTIES.put(propName, value);
            } else if (isRequired()) {
                throw new IllegalArgumentException("Value is required for " + name());
            } else {
                PROPERTY_STORE.get().remove(value);
                DB_PROPERTIES.remove(propName);
            }
        } else {
            if (value != null) {
                MEM_PROPERTIES.put(propName, value);
            } else if (isRequired()) {
                throw new IllegalArgumentException("Value is required for " + name());
            } else {
                MEM_PROPERTIES.remove(propName);
            }
        }
    }

    public void override(String value) {
        ensureLoaded();
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

    private static class DeferredPropertyStore implements PropertyStore {

        private List<Runnable> deferredOperations = new ArrayList<Runnable>(4);
        private PropertyStore realPropertyStore;

        public String get(String name) {
            throw new UnsupportedOperationException("Cannot read property values during initialization");
        }

        public void set(final String name, final String value) {
            deferredOperations.add(new Runnable() {
                public void run() {
                    realPropertyStore.set(name, value);
                }
            });
        }

        public void remove(final String name) {
            throw new UnsupportedOperationException("Cannot remove property values during initialization");
        }

        private void setRealPropertyStore(PropertyStore realPropertyStore) {
            this.realPropertyStore = realPropertyStore;
            Thread thread = Thread.currentThread();
            for (Runnable runnable : deferredOperations) {
                try {
                    runnable.run();
                } catch (Exception e) {
                    thread.getUncaughtExceptionHandler().uncaughtException(thread, e);
                }
            }
        }
    }
}
