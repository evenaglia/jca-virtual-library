package net.venaglia.realms.common.map.things;

import net.venaglia.common.util.Factory;
import net.venaglia.realms.common.map.things.annotations.ThingType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 7:45 PM
 */
public abstract class ThingFactory<T extends Thing> implements Factory<T> {

    private static final Map<String,ThingFactory<? extends Thing>> KNOWN_TYPES_OF_THINGS =
            new ConcurrentHashMap<String,ThingFactory<? extends Thing>>();

    private final ThingMetadata<T> metadata;

    protected ThingFactory(ThingMetadata<T> metadata) {
        this.metadata = metadata;
    }

    @SuppressWarnings("unchecked")
    public static  <T extends Thing> ThingFactory<T> getFor (String type) {
        return (ThingFactory<T>)KNOWN_TYPES_OF_THINGS.get(type);
    }

    public static  <T extends Thing> ThingFactory<T> getFor (Class<T> type) {
        ThingType thingType = type.getAnnotation(ThingType.class);
        return getFor(thingType == null ? type.getSimpleName() : thingType.value());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Thing> ThingFactory<T> getFor (ThingMetadata<T> metadata) {
        return (ThingFactory<T>)KNOWN_TYPES_OF_THINGS.get(metadata.getType());
    }

    public static <T extends Thing> void register(ThingFactory<T> factory) {
        KNOWN_TYPES_OF_THINGS.put(factory.getMetadata().getType(), factory);
    }

    public ThingMetadata<T> getMetadata() {
        return metadata;
    }
}
