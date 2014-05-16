package net.venaglia.realms.common.map.things;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 8:05 PM
 */
public abstract class MutableAbstractThing extends AbstractThing {

    private Set<ThingMetadata.PropertyMetadata<?>> changedProperties;

    protected double x, y, z;

    @Override
    public boolean isMutable() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P> P setProperty(ThingMetadata.PropertyMetadata<P> property, P value) {
        if (!property.isWritable()) {
            throw new UnsupportedOperationException();
        }
        if (value != null && !property.getType().isA(value)) {
            throw new ClassCastException("Cannot cast " + value.getClass().getSimpleName() + " to " + property.getType().getNativeType().getSimpleName() + " for property ");
        }
        myRef.lock(true);
        try {
            ensurePropertiesAreAccessible();
            P priorValue = getPropertyImpl(property);
            P newValue = value == null ? property.getDefaultValue() : value;
            if (newValue == null ? priorValue != null : !newValue.equals(priorValue)) {
                setPropertyImpl(newValue, property);
                markPropertyChanged(property);
            }
            return priorValue;
        } finally {
            myRef.unlock(true);
        }
    }

    @Override
    public void delete() {
        myRef.lock(true);
        try {
            ensureLoaded();
            if (status == ThingStatus.NEW) {
                status = ThingStatus.DELETED;
            } else if (status != ThingStatus.GHOST) {
                status = ThingStatus.GHOST;
            }
        } finally {
            myRef.unlock(true);
        }
    }

    @Override
    public void writeChangesTo(ThingProperties properties, ThingWriter thingWriter) {
        ensureLoaded();
        myRef.lock(true);
        try {
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
                case DIRTY:
                    for (ThingMetadata.PropertyMetadata<?> property : changedProperties) {
                        properties.set(property, getProperty(property));
                    }
                    thingWriter.updateThing(properties);
                    changedProperties.clear();
                    status = ThingStatus.CLEAN;
                    break;
                case GHOST:
                    thingWriter.deleteThing();
                    if (changedProperties != null) {
                        changedProperties = null;
                    }
                    status = ThingStatus.DELETED;
                    break;
            }
        } finally {
            myRef.unlock(true);
        }
    }

    protected void markPropertyChanged(ThingMetadata.PropertyMetadata<?> property) {
        if (status == ThingStatus.NEW) {
            return; // no-op
        }
        if (changedProperties == null) {
            changedProperties = new HashSet<ThingMetadata.PropertyMetadata<?>>(4);
        }
        changedProperties.add(property);
        if (status == ThingStatus.CLEAN) {
            status = ThingStatus.DIRTY;
        }
    }

}
