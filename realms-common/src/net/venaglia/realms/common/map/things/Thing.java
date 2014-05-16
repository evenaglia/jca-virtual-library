package net.venaglia.realms.common.map.things;

import net.venaglia.gloo.projection.Projectable;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 7:32 PM
 */
public interface Thing {

    long getId();
    String getType();
    ThingRef<?> getRef();
    boolean isMutable();

    <P> P getProperty(String name);
    <P> P getProperty(ThingMetadata.PropertyMetadata<P> property);
    <P> P setProperty(ThingMetadata.PropertyMetadata<P> property, P value);
    void delete();

    ThingStatus getStatus();
    void writeChangesTo(ThingProperties properties, ThingWriter thingWriter);

    Projectable getProjectable();
}
