package net.venaglia.realms.common.map.world;

import static net.venaglia.realms.common.map.things.annotations.AnnotationDrivenThingProcessor.generateSerializer;

import net.venaglia.common.util.Factory;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.realms.common.map.data.binaries.BinaryTypeDefinition;
import net.venaglia.realms.common.map.data.binaries.BinaryTypeRegistry;
import net.venaglia.realms.common.map.things.annotations.Property;
import net.venaglia.realms.common.map.world.ref.AcreDetailRef;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.MatrixXForm;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

import java.util.ArrayList;
import java.util.Collection;

/**
 * User: ed
 * Date: 1/28/13
 * Time: 8:44 AM
 */
public class AcreDetail extends WorldElement {

    public static final String MIMETYPE = "world/acre";
    public static final BinaryTypeDefinition<AcreDetail> DEFINITION;

    static {
        Factory<AcreDetail> factory = new Factory<AcreDetail>() {
            public AcreDetail createEmpty() {
                return new AcreDetail();
            }
        };
        SerializerStrategy<AcreDetail> serializer = generateSerializer(AcreDetail.class, factory);
        DEFINITION = BinaryTypeDefinition.build(AcreDetail.class, MIMETYPE, serializer);
        BinaryTypeRegistry.add(DEFINITION);
    }

    private static final double HALF_PI = Math.PI * 0.5;

    @Property
    private GeoPoint center;
    @Property
    private GeoPoint[] vertices;
    private transient XForm toLocal;
    private transient XForm toGlobal;
    @Property
    int[] neighborIds;
    private Collection<AcreDetailRef> neighbors;
    @Property
    private float[] altitude;
    @Property
    private float ruggedness = 1.0f; // 1 = plains; 10 = rolling hills; 100 = mountains

    public AcreDetail() {
    }

    public GeoPoint getCenter() {
        return center;
    }

    public void setCenter(GeoPoint center) {
        this.center = center;
        this.toGlobal = null;
        this.toLocal = null;
    }

    public GeoPoint[] getVertices() {
        return vertices;
    }

    public void setVertices(GeoPoint[] vertices) {
        this.vertices = vertices;
    }

    public XForm getToLocal() {
        if (toLocal == null && center != null) {
            toLocal = new MatrixXForm(Matrix_4x4.identity()
                                                .product(Matrix_4x4.rotate(Axis.Z, -center.longitude))
                                                .product(Matrix_4x4.rotate(Axis.X, center.latitude + HALF_PI))
                                                .product(Matrix_4x4.translate(Axis.Y,
                                                                              -GeoSpec.APPROX_RADIUS_METERS.get())));
        }
        return toLocal;
    }

    public XForm getToGlobal() {
        if (toGlobal == null && center != null) {
            toGlobal = new MatrixXForm(Matrix_4x4.identity()
                                                 .product(Matrix_4x4.translate(Axis.Y, GeoSpec.APPROX_RADIUS_METERS.get()))
                                                 .product(Matrix_4x4.rotate(Axis.X, -center.latitude - HALF_PI))
                                                 .product(Matrix_4x4.rotate(Axis.Z, center.longitude)));
        }
        return toGlobal;
    }

    public Collection<AcreDetailRef> getNeighbors() {
        if (neighbors == null) {
            WorldMap worldMap = WorldMap.INSTANCE.get();
            neighbors = new ArrayList<AcreDetailRef>(neighborIds.length);
            for (int neighborId : neighborIds) {
                neighbors.add(new AcreDetailRef(neighborId, worldMap.getBinaryStore()));
            }
        }
        return neighbors;
    }

    public void setNeighborIds(int[] neighborIds) {
        this.neighborIds = neighborIds;
    }

    public void setAltitude(float[] altitude) {
        this.altitude = altitude;
    }

    public Shape<?> getSurface(DetailLevel detailLevel) {
        // todo
        return null;
    }

    public static void init() {
        // no-op
    }
}
