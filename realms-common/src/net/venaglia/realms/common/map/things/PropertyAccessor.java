package net.venaglia.realms.common.map.things;

/**
 * User: ed
 * Date: 4/17/14
 * Time: 8:42 AM
 */
public interface PropertyAccessor {

    void beforeUpdate();

    void afterUpdate();

    void beforeRead();

    void afterRead();

    <P> P get(ThingMetadata.PropertyMetadata<P> property, Thing thing);

    <P> void set(ThingMetadata.PropertyMetadata<P> property, Thing thing, P value);
}
