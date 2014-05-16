package net.venaglia.realms.common.map.things;

import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 3/19/14
 * Time: 10:05 PM
 */
public interface ThingMetadata<T extends Thing> {

    String getType();
    T cast(Thing thing);
    Set<String> getPropertyNames();
    Map<String,PropertyMetadata<?>> getPropertyMetadata();
    PropertyMetadata<?> getPropertyMetadata(String name);
    PropertyAccessor getPropertyAccessor();
    boolean isMutableThing();

    interface PropertyMetadata<P> {
        ThingMetadata<?> getThingMetadata();
        String getName();
        PropertyType<P> getType();
        P getDefaultValue();
        boolean isRequired();
        boolean isWritable();
    }
}
