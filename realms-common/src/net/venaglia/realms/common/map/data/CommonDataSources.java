package net.venaglia.realms.common.map.data;

import net.venaglia.realms.common.map.PropertyStore;
import net.venaglia.realms.common.map.UniqueIdSource;
import net.venaglia.realms.common.map.data.binaries.BinaryCache;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * User: ed
 * Date: 4/1/14
 * Time: 8:15 AM
 */
public class CommonDataSources {

    private final ThingCache thingCache;
    private final CubeCache cubeCache;
    private final BinaryCache binaryCache;
    private final PropertyStore propertyStore;
    private final DirtyThingQueue dirtyThingQueue;
    private final Map<Sequence,UniqueIdSource> idSources;

    public CommonDataSources(ThingCache thingCache,
                             CubeCache cubeCache,
                             BinaryCache binaryCache,
                             PropertyStore propertyStore,
                             DirtyThingQueue dirtyThingQueue,
                             IdSourceProvider idSourceProvider) {
        this.thingCache = thingCache;
        this.cubeCache = cubeCache;
        this.binaryCache = binaryCache;
        this.propertyStore = propertyStore;
        this.dirtyThingQueue = dirtyThingQueue;

        Map<Sequence,UniqueIdSource> buffer = new EnumMap<Sequence,UniqueIdSource>(Sequence.class);
        for (Sequence sequence : Sequence.values()) {
            UniqueIdSource source = idSourceProvider.getIdSource(sequence);
            if (source == null) throw new NullPointerException("source");
            buffer.put(sequence, source);
        }
        this.idSources = Collections.unmodifiableMap(buffer);
    }

    public ThingCache getThingCache() {
        return thingCache;
    }

    public CubeCache getCubeCache() {
        return cubeCache;
    }

    public BinaryCache getBinaryCache() {
        return binaryCache;
    }

    public PropertyStore getPropertyStore() {
        return propertyStore;
    }

    public DirtyThingQueue getDirtyThingQueue() {
        return dirtyThingQueue;
    }

    public Long nextId(Sequence sequence) {
        return idSources.get(sequence).next();
    }
}
