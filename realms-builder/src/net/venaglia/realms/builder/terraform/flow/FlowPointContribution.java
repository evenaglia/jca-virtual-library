package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.gloo.physical.geom.XForm;

/**
 * User: ed
 * Date: 11/3/14
 * Time: 7:50 AM
 */
public interface FlowPointContribution {

    /**
     * @return How much this contribution should contribute to the sum total. This value is relative to other contributions.
     */
    double getContribution();

    /**
     * @return The X component of this FlowPointContribution
     */
    double getI();

    /**
     * @return The Y component of this FlowPointContribution
     */
    double getJ();

    /**
     * @return The Z component of this FlowPointContribution
     */
    double getK();

    /**
     * @return The magnitude component of this FlowPointContribution
     */
    double getMagnitude();

    /**
     * @return The pressure component of this FlowPointContribution
     */
    double getPressure();

    /**
     * Passed the contribution (i,j,k) to {@link XForm.View#convert(double, double, double, double)}
     * @param view The transforming consumer of the vector
     * @param <V> The type to transform to
     * @return THe transformed object representation of (i,j,k)
     */
    <V> V getVectorIJK(XForm.View<V> view);

    /**
     * @return The Fragment from which this contribution is derived.
     */
    Fragment getFragment();

    FlowPointContribution immutableCopy();
}
