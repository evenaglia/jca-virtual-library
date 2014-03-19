package net.venaglia.realms.common.map.elements;

import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.realms.common.map.db_x.DatabaseOptions;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.MatrixXForm;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.common.util.Ref;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

import java.util.Collection;

/**
 * User: ed
 * Date: 1/28/13
 * Time: 8:44 AM
 */
@DatabaseOptions(
        filename = "graph",
        banner = "Graph of all acres in the World",
        spatial = true
)
public class GraphAcre extends WorldElement {

    private static final double HALF_PI = Math.PI * 0.5;

    private GeoPoint center;
    private GeoPoint[] vertices;
    private transient XForm toLocal;
    private transient XForm toGlobal;
    private Collection<Ref<GraphAcre>> neighbors;
    private Collection<Ref<Pocket>> pockets;

    public GraphAcre() {
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
                                                .product(Matrix_4x4.translate(Axis.Y, -GeoSpec.APPROX_RADIUS_METERS.get())));
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

    public Collection<Ref<GraphAcre>> getNeighbors() {
        return neighbors;
    }

    public void setNeighbors(Collection<Ref<GraphAcre>> neighbors) {
        this.neighbors = neighbors;
    }

    public Collection<Ref<Pocket>> getPockets() {
        return pockets;
    }

    public void setPockets(Collection<Ref<Pocket>> pockets) {
        this.pockets = pockets;
    }

    public Shape<?> getSurface(DetailLevel detailLevel) {
        // todo
        return null;
    }

    public Ref<DetailAcre> getDetailAcre() {
        // todo
        return null;
    }
}
