package net.venaglia.realms.common.map.things;

import net.venaglia.common.util.Pair;
import net.venaglia.gloo.projection.Projectable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 8:05 PM
 */
public abstract class AbstractThing implements Thing {

    protected ThingRef<?> myRef;
    protected PropertyAccessor propertyAccessor;
    protected Map<String,Object> extraProperties;

    ThingStatus status = ThingStatus.NOT_LOADED;

    public ThingRef<?> getRef() {
        myRef.lock(false);
        try {
            ensureLoaded();
            return myRef;
        } finally {
            myRef.unlock(false);
        }
    }

    public boolean isMutable() {
        return false;
    }

    public long getId() {
        myRef.lock(false);
        try {
            ensurePropertiesAreAccessible();
            return myRef.getId();
        } finally {
            myRef.unlock(false);
        }
    }

    public String getType() {
        return myRef.getType();
    }

    public <P> P setProperty(ThingMetadata.PropertyMetadata<P> property, P value) {
        ensurePropertiesAreAccessible();
        throw new UnsupportedOperationException();
    }

    public void delete() {
        ensurePropertiesAreAccessible();
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public <P> P getProperty(String name) {
        ThingMetadata.PropertyMetadata<?> property = getPropertyMetadata(name);
        ensurePropertiesAreAccessible();
        return (P)getProperty(property);
    }

    public <P> P getProperty(ThingMetadata.PropertyMetadata<P> property) {
        myRef.lock(false);
        try {
            return property != null ? getPropertyImpl(property) : null;
        } finally {
            myRef.unlock(false);
        }
    }

    public ThingStatus getStatus() {
        myRef.lock(false);
        try {
            ensureLoaded();
            return status;
        } finally {
            myRef.unlock(false);
        }
    }

    public void writeChangesTo(ThingProperties properties, ThingWriter thingWriter) {
        ensureLoaded();
        myRef.lock(true);
        try {
            if (propertyAccessor != null) {
                propertyAccessor.beforeRead();
            }
            switch (status) {
                case NEW:
                    Collection<ThingMetadata.PropertyMetadata<?>> allProperties =
                            getRef().getMetadata().getPropertyMetadata().values();
                    for (ThingMetadata.PropertyMetadata<?> property : allProperties) {
                        properties.set(property, getProperty(property));
                    }
                    thingWriter.addThing(myRef.getPoint(), myRef.getCube(), properties);
                    status = ThingStatus.CLEAN;
                    break;
                case GHOST:
                    thingWriter.deleteThing();
                    status = ThingStatus.DELETED;
                    break;
            }
        } finally {
            if (propertyAccessor != null) {
                propertyAccessor.afterRead();
            }
            myRef.unlock(true);
        }
    }

    public Projectable getProjectable() {
        return Projectable.NULL;
    }

    void ensurePropertiesAreAccessible() {
        ensureLoaded();
        if (status == ThingStatus.GHOST) {
            throw new IllegalStateException(myRef + " has been deleted");
        }
    }

    void ensureLoaded() {
        if (status == ThingStatus.NOT_LOADED) {
            throw new IllegalStateException("this thing is not loaded");
        }
    }

    private void load(ThingRef<?> myRef, ThingProperties properties) {
        myRef.lock(true);
        try {
            if (status != ThingStatus.NOT_LOADED) {
                throw new IllegalStateException("Cannot load a thing that is already loaded");
            }
            this.myRef = myRef;
            this.propertyAccessor = myRef.getMetadata().getPropertyAccessor();
            if (propertyAccessor != null) {
                propertyAccessor.beforeUpdate();
            }
            if (properties != null) {
                status = ThingStatus.CLEAN;
                for (Pair<ThingMetadata.PropertyMetadata<?>,Object> property : properties) {
                    setPropertyUnsafe(property.getB(), property.getA());
                }
                afterLoad();
            } else {
                status = ThingStatus.NEW;
                afterCreate();
            }
        } finally {
            if (propertyAccessor != null) {
                propertyAccessor.afterUpdate();
            }
            myRef.unlock(true);
        }
    }

    private void update(Iterator<Pair<String,Object>> iterator) {
        boolean updated = false;
        myRef.lock(true);
        try {
            switch (status) {
                case CLEAN:
                    status = ThingStatus.DIRTY;
                case NEW:
                case DIRTY:
                    if (propertyAccessor != null) {
                        propertyAccessor.beforeUpdate();
                        updated = true;
                    }
                    ThingMetadata<? extends Thing> metadata = myRef.getMetadata();
                    while (iterator.hasNext()) {
                        Pair<String,Object> property = iterator.next();
                        ThingMetadata.PropertyMetadata<?> prop = metadata.getPropertyMetadata(property.getA());
                        if (prop != null) {
                            setPropertyUnsafe(property.getB(), prop);
                        }
                    }
                    break;
                case GHOST:
                case DELETED:
                    // no-op, ignore this data
                    break;
                case NOT_LOADED:
                    throw new IllegalStateException("Cannot update a thing that is not loaded");
            }
        } finally {
            if (updated) {
                propertyAccessor.afterUpdate();
            }
            myRef.unlock(true);
        }
    }

    private void recycle() {
        ThingRef<?> myRef = this.myRef;
        if (myRef == null) {
            throw new IllegalStateException("Cannot recycle a thing that is already unloaded");
        }
        myRef.lock(true);
        try {
            switch (status) {
                case NEW:
                case DIRTY:
                case GHOST:
                    throw new IllegalStateException("Cannot recycle a dirty thing: " + myRef);
                case CLEAN:
                case DELETED:
                    beforeRecycle();
                    this.status = ThingStatus.NOT_LOADED;
                    this.myRef = null;
                    this.propertyAccessor = null;
                    this.extraProperties = null;
                    break;
                case NOT_LOADED:
                    // no-op
                    break;
            }
        } finally {
            myRef.unlock(true);
        }
    }

    // life-cycle methods, override these if you need to

    protected void afterLoad() {
        // no-op, lifecycle method
    }

    protected void afterCreate() {
        // no-op, lifecycle method
    }

    protected void beforeRecycle() {
        // no-op, lifecycle method
    }

    // Default property handlers

    protected ThingMetadata.PropertyMetadata<?> getPropertyMetadata(String name) {
        return getRef().getMetadata().getPropertyMetadata(name);
    }

    @SuppressWarnings("unchecked")
    protected <P> P getPropertyImpl(ThingMetadata.PropertyMetadata<P> metadata) {
        if (propertyAccessor != null) {
            return propertyAccessor.get(metadata, this);
        }
        String name = metadata.getName();
        return extraProperties != null && extraProperties.containsKey(name)
               ? (P)extraProperties.get(name)
               : metadata.getDefaultValue();
    }

    protected <P> void setPropertyImpl(P value, ThingMetadata.PropertyMetadata<P> metadata) {
        if (propertyAccessor != null) {
            propertyAccessor.set(metadata, this, value);
            return;
        }
        String name = metadata.getName();
        if (extraProperties == null) {
            extraProperties = new HashMap<String,Object>(4);
        }
        extraProperties.put(name, value);
    }

    private <P> void setPropertyUnsafe(Object value, ThingMetadata.PropertyMetadata<P> metadata) {
        setPropertyImpl(metadata.getType().cast(value), metadata);
    }

    public static <T extends AbstractThing> void load(ThingRef<T> ref, T thing, ThingProperties properties) {
        thing.load(ref, properties);
    }

    public static void update(AbstractThing thing, Iterator<Pair<String,Object>> iterator) {
        thing.update(iterator);
    }

    public static void unload(AbstractThing thing) {
        thing.recycle();
    }

}
