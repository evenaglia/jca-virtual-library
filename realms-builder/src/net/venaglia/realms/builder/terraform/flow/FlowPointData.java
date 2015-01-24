package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.gloo.physical.geom.MatrixXForm;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.realms.spec.map.GeoPoint;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:09 AM
 */
public interface FlowPointData {

    /**
     * @return The GeoPoint where this flow point data is to be sampled
     */
    GeoPoint getGeoPoint();

    /**
     * @param view The view representing the Point data type you wish to consume.
     * @param <P> The Point data type you wish to consume.
     * @return A Point
     */
    <P> P getPoint(XForm.View<P> view);

    /**
     * @param view The view representing the Magnitude Vector data type you wish to consume.
     * @param <V> The Magnitude Vector data type you wish to consume.
     * @return A Magnitude Vector
     */
    <V> V getMagnitudeVector(MatrixXForm.View<V> view);

    /**
     * @param view The view representing the Vector data type you wish to consume.
     * @param <V> The Vector data type you wish to consume.
     * @return A Vector
     */
    <V> V getDirection(MatrixXForm.View<V> view);

    /**
     * @return The magnitude of the sampled plow point data.
     */
    double getMagnitude();

    /**
     * @return pressure as an exponent, 0 represents average pressure across the globe
     */
    double getPressure();

    /**
     * @return An Iterable of FlowPointContribution objects that were used to compute the
     *     values of this FlowPointData object.
     * @throws UnsupportedOperationException if this FlowPointData object does not support
     *     this operation or it did not record individual FlowPointContributions made to it.
     */
    Iterable<FlowPointContribution> getFlowPointContributions();

    /**
     * @return A copy of this FlowPointData that will not change
     */
    FlowPointData immutableCopy();
}
