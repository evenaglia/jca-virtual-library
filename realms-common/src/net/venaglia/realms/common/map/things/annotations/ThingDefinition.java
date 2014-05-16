package net.venaglia.realms.common.map.things.annotations;

import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.realms.common.map.things.MutableAbstractThing;
import net.venaglia.realms.common.map.things.PropertyAccessor;
import net.venaglia.realms.common.map.things.PropertyType;
import net.venaglia.realms.common.map.things.Thing;
import net.venaglia.realms.common.map.things.ThingFactory;
import net.venaglia.realms.common.map.things.ThingMetadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 3/19/14
 * Time: 9:11 PM
 *
 * Utility class used to define types. This
 */
public class ThingDefinition<THING extends AbstractThing> {

    private String type;
    private Class<THING> thingClass;
    private Map<String,ThingMetadata.PropertyMetadata<?>> properties = new HashMap<String,ThingMetadata.PropertyMetadata<?>>();
    private THING prototype;
    private PropertyAccessor propertyAccessor;

    private boolean done;

    private ThingDefinition(String type, Class<THING> thingClass) {
        if (type == null) throw new NullPointerException("type");
        if (thingClass == null) throw new NullPointerException("thingClass");
        this.type = type;
        this.thingClass = thingClass;
    }

    public <P> ThingDefinition<THING> addProperty(String name, PropertyType<P> type, P defaultValue, boolean required, boolean writable) {
        if (done) {
            throw new IllegalStateException("Type definition is already done");
        }
        if (!name.matches("[a-zA-Z_$][a-zA-Z0-9_$]*([.-][a-zA-Z_$][a-zA-Z0-9_$]*)*")) {
            throw new IllegalArgumentException("Invalid identifier: \"" + name + "\"");
        }
        properties.put(name, new PropertyDefinition<P>(name, type, defaultValue, required, writable));
        return this;
    }

    public ThingDefinition<THING> setPropertyAccessor(PropertyAccessor propertyAccessor) {
        if (done) {
            throw new IllegalStateException("Type definition is already done");
        }
        if (propertyAccessor == null) {
            throw new NullPointerException("propertyAccessor");
        }
        if (this.propertyAccessor != null && this.propertyAccessor != propertyAccessor) {
            throw new IllegalStateException("PropertyAccessor has already been set");
        }
        this.propertyAccessor = propertyAccessor;
        return this;
    }

    public void done() {
        if (!done) {
            if (propertyAccessor == null) {
                throw new IllegalStateException("No property accessor has been set");
            }
            done = true;
            properties = Collections.unmodifiableMap(properties);
            ThingFactory.register(getFactory());
        }
    }

    private ThingFactory<THING> getFactory() {
        try {
            prototype = thingClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to construct a new, empty, " + thingClass.getSimpleName(), e);
        }
        final ThingMetadata<THING> metadata = new ThingMetadata<THING>() {

            private final boolean mutable = MutableAbstractThing.class.isAssignableFrom(thingClass);
            private final int hashCode;

            {
                int hash = 31 * type.hashCode() + thingClass.hashCode();
                int propHash = 0;
                for (Map.Entry<String, PropertyMetadata<?>> entry : properties.entrySet()) {
                    propHash += 31 * entry.getKey().hashCode() + entry.getValue().getType().hashCode();
                }
                hashCode = hash * 31 + propHash;
            }

            public String getType() {
                return type;
            }

            public THING cast(Thing thing) {
                return thingClass.cast(thing);
            }

            public Set<String> getPropertyNames() {
                return properties.keySet();
            }

            public Map<String, PropertyMetadata<?>> getPropertyMetadata() {
                return properties;
            }

            public PropertyMetadata<?> getPropertyMetadata(String name) {
                return properties.get(name);
            }

            public PropertyAccessor getPropertyAccessor() {
                return propertyAccessor;
            }

            public boolean isMutableThing() {
                return mutable;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ThingMetadata)) return false;
                if (getClass() == o.getClass() && hashCode != o.hashCode()) return false;

                ThingMetadata<?> that = (ThingMetadata<?>)o;

                if (!type.equals(that.getType()) || properties.size() != that.getPropertyMetadata().size()) {
                    return false;
                }
                for (Map.Entry<String, PropertyMetadata<?>> entry : properties.entrySet()) {
                    PropertyMetadata<?> propertyMetadata = that.getPropertyMetadata(entry.getKey());
                    if (propertyMetadata == null || !entry.getValue().getType().equals(propertyMetadata.getType())) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public int hashCode() {
                return hashCode;
            }

            @Override
            public String toString() {
                return type + " <" + thingClass.getSimpleName() + ">";
            }
        };
        for (ThingMetadata.PropertyMetadata<?> m : properties.values()) {
            ((PropertyDefinition<?>)m).setThingMetadata(metadata);
        }
        return new ThingFactory<THING>(metadata) {
            public THING createEmpty() {
                try {
                    return thingClass.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    protected static class PropertyDefinition<P> implements ThingMetadata.PropertyMetadata<P> {

        private final String name;
        private final PropertyType<P> type;
        private final P defaultValue;
        private final boolean required;
        private final boolean writable;
        private final int hashCode;

        private ThingMetadata<?> thingMetadata;

        PropertyDefinition(String name,
                           PropertyType<P> type,
                           P defaultValue,
                           boolean required,
                           boolean writable) {
            if (name == null) throw new NullPointerException("name");
            if (type == null) throw new NullPointerException("type");
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.required = required;
            this.writable = writable;

            int hash = name.hashCode();
            hash = 31 * hash + type.hashCode();
            hash = 31 * hash + (thingMetadata != null ? thingMetadata.hashCode() : 0);
            hashCode = hash;
        }

        public ThingMetadata<?> getThingMetadata() {
            thingMetadata = null;
            return thingMetadata;
        }

        void setThingMetadata(ThingMetadata<?> thingMetadata) {
            this.thingMetadata = thingMetadata;
        }

        public String getName() {
            return name;
        }

        public PropertyType<P> getType() {
            return type;
        }

        public P getDefaultValue() {
            return defaultValue;
        }

        public boolean isRequired() {
            return required;
        }

        public boolean isWritable() {
            return writable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ThingMetadata.PropertyMetadata)) return false;
            if (getClass() == o.getClass() && hashCode != o.hashCode()) return false;

            ThingMetadata.PropertyMetadata<?> that = (ThingMetadata.PropertyMetadata<?>)o;

            if (!name.equals(that.getName())) return false;
            if (!type.equals(that.getType())) return false;
            if (thingMetadata != null ? !thingMetadata.equals(that.getThingMetadata()) : that.getThingMetadata() != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + (thingMetadata != null ? thingMetadata.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return thingMetadata.getType() + "." + name + " <" + type + ">";
        }
    }

    public static <T extends AbstractThing> ThingDefinition<T> define (String type, Class<T> thingClass) {
        return new ThingDefinition<T>(type, thingClass);
    }

    public static <P> ThingMetadata.PropertyMetadata<P> defineStaticProperty(String name, PropertyType<P> type) {
        return new PropertyDefinition<P>(name, type, null, true, true);
    }
}
