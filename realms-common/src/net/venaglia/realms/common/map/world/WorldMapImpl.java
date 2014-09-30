package net.venaglia.realms.common.map.world;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.BinaryStore;
import net.venaglia.realms.common.map.CubeUtils;
import net.venaglia.realms.common.map.DataStore;
import net.venaglia.realms.common.map.PropertyStore;
import net.venaglia.realms.common.map.VertexStore;
import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.realms.common.map.data.CommonDataSources;
import net.venaglia.realms.common.map.data.CubeImpl;
import net.venaglia.realms.common.map.data.Sequence;
import net.venaglia.realms.common.map.data.ThingRefImpl;
import net.venaglia.realms.common.map.serializers.ColorSerializerStrategy;
import net.venaglia.realms.common.map.serializers.GeoPointSerializerStrategy;
import net.venaglia.realms.common.map.serializers.PointSerializerStrategy;
import net.venaglia.realms.common.map.serializers.TextureCoordinateSerializerStrategy;
import net.venaglia.realms.common.map.serializers.VectorSerializerStrategy;
import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.realms.common.map.things.Thing;
import net.venaglia.realms.common.map.things.ThingFactory;
import net.venaglia.realms.common.map.things.ThingMetadata;
import net.venaglia.realms.common.map.things.ThingRef;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.data.binaries.BinaryType;
import net.venaglia.realms.common.util.Visitor;

import java.util.UUID;

/**
 * User: ed
 * Date: 4/14/14
 * Time: 7:15 AM
 */
public class WorldMapImpl implements WorldMap {

    private final DataStore dataStore;
    private final CommonDataSources commonDataSources;
    private final BinaryStore binaryStore;
    private final VertexStore vertexStore = null; // todo

    static {
        ColorSerializerStrategy.init();
        GeoPointSerializerStrategy.init();
        PointSerializerStrategy.init();
        TextureCoordinateSerializerStrategy.init();
        VectorSerializerStrategy.init();
        AcreDetail.init();
    }

    public WorldMapImpl() {
        try {
            this.dataStore = Configuration.DATA_STORE_CLASS.getBean();
            this.dataStore.init();
            this.commonDataSources = this.dataStore.getCommonDataSources();
            this.binaryStore = new BinaryStoreImpl();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getInstanceUuid() {
        return dataStore.getInstanceUuid();
    }

    @SuppressWarnings("unchecked")
    public <T extends Thing> ThingRef<T> getThing(Long thingId) {
        return (ThingRef<T>)commonDataSources.getThingCache().get(thingId);
    }

    public <T extends Thing> ThingRef<T> createThing(ThingMetadata<T> metadata,
                                                     Point position,
                                                     Visitor<ThingRef<T>> initializer) {
        @SuppressWarnings({ "unchecked", "RedundantCast" })
        ThingRef<T> ref = (ThingRef<T>)(ThingRef)createThingImpl(metadata, position);
        initializer.visit(ref);
        commonDataSources.getDirtyThingQueue().add((ThingRefImpl<?>)ref);
        return ref;
    }

    public BinaryStore getBinaryStore() {
        return binaryStore;
    }

    public VertexStore getVertexStore() {
        return vertexStore;
    }

    public PropertyStore getPropertyStore() {
        return commonDataSources.getPropertyStore();
    }

    private <T extends AbstractThing> ThingRefImpl<T> createThingImpl(ThingMetadata<?> rawMetadata, Point p) {
        Long id = commonDataSources.nextId(Sequence.THING);
        @SuppressWarnings("unchecked")
        ThingMetadata<T> metadata = (ThingMetadata<T>)rawMetadata;
        Long cubeID = CubeUtils.getCubeID(p.x, p.y, p.z);
        CubeImpl cube = commonDataSources.getCubeCache().get(cubeID);
        Thing thing = ThingFactory.getFor(metadata).createEmpty();
        ThingRefImpl<T> ref = ThingRefImpl.getUnused();
        ref.load(id, p.x, p.y, p.z, metadata, cube, null);
        //noinspection unchecked
        AbstractThing.load(ref, (T)thing, null);
        return ref;
    }

    private class BinaryStoreImpl implements BinaryStore {

        public BinaryResource getBinaryResource(Long id) {
            return commonDataSources.getBinaryCache().get(id);
        }

        public BinaryResource getBinaryResource(String mimetype, long locatorId) {
            return commonDataSources.getBinaryCache().lookupByLocator(mimetype, locatorId);
        }

        public BinaryResource createBinaryResource(BinaryType type, long locatorId, byte[] data) {
            BinaryResource resource = new BinaryResource();
            resource.init(null, type, type.generateMetadata(data), null, data);
            return commonDataSources.getBinaryCache().insert(resource, locatorId);
        }

        public BinaryResource updateBinaryResource(BinaryResource resource, long locatorId, byte[] data) {
            return commonDataSources.getBinaryCache().update(resource, locatorId, data);
        }

        public void destroyBinaryResource(BinaryResource resource, long locatorId) {
            commonDataSources.getBinaryCache().delete(resource, locatorId);
        }

        public void freeBinaryResource(BinaryResource resource) {
            commonDataSources.getBinaryCache().evict(resource);
        }

    }

    public static void main(String[] args) {
        WorldMap worldMap = new WorldMapImpl();
//        worldMap.createThing();
    }
}
