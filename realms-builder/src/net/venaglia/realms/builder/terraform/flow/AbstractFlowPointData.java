package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.common.util.Consumer;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.realms.spec.map.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 10/17/14
 * Time: 6:02 PM
 */
public abstract class AbstractFlowPointData implements FlowPointData {

    protected final double nominalFragmentRadius;
    protected final double nominalExponentMultiplier;
    protected final Consumer<Fragment> fragmentConsumer;
    protected final List<FlowPointContribution> contributions;

    private FlowQuery query;

    protected GeoPoint geoPoint;
    protected double x, y, z;
    protected double i = 0.0, j = 0.0, k = 0.0;
    protected double m = 0.0, p = 0.0;
    protected double s = 1.0;
    protected double contributorSum = 0.0;
    protected double avg = 1.0;

    public AbstractFlowPointData(double nominalFragmentRadius, boolean captureContributions) {
        this.nominalFragmentRadius = nominalFragmentRadius;
        this.nominalExponentMultiplier = -0.5 / (nominalFragmentRadius * nominalFragmentRadius * nominalFragmentRadius);
        this.fragmentConsumer = new FragmentConsumer();
        this.contributions = captureContributions ? new ArrayList<FlowPointContribution>() : null;
    }

    protected void load(double flowRadius, FlowQuery query) {
        this.geoPoint = query.getPoint();
        this.query = query;
        this.s = query.getScale() * (query.getRadius() / flowRadius);
    }

    protected void begin(double radius) {
        double u = Math.cos(geoPoint.latitude);
        x = Math.sin(geoPoint.longitude) * u * radius;
        y = Math.cos(geoPoint.longitude) * u * radius;
        z = Math.sin(geoPoint.latitude) * radius;
        i = 0;
        j = 0;
        k = 0;
        m = 0;
        p = 0;
        if (contributions != null) {
            contributions.clear();
        }
    }

    protected void consumeContribution(ContributionImpl flowPointContribution) {
        // processing vector data
        double contribution = flowPointContribution.getContribution();
        i += flowPointContribution.getI() * contribution;
        j += flowPointContribution.getJ() * contribution;
        k += flowPointContribution.getK() * contribution;
        m += flowPointContribution.getMagnitude() * contribution;
        p += flowPointContribution.getPressure() * contribution;
        contributorSum += contribution;
        if (contributions != null) {
            contributions.add(flowPointContribution.immutableCopy());
        }
    }

    protected void processData() {
        if (contributorSum > 0) {
            avg = 1.0 / contributorSum;
            i *= avg;
            j *= avg;
            k *= avg;
            p *= avg;
            m *= avg;
            query.processDataForPoint(this);
        } else {
            avg = 0;
            p = Double.NEGATIVE_INFINITY;
        }
    }

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public <P> P getPoint(XForm.View<P> view) {
        return view.convert(x * s, y * s, z * s, 1);
    }

    public <V> V getMagnitudeVector(XForm.View<V> view) {
        return view.convert(i * s, j * s, k * s, 1);
    }

    public double getPressure() {
        return p;
    }

    public <V> V getDirection(XForm.View<V> view) {
        return view.convert(i * m * s, j * m * s, k * m * s, 1);
    }

    public double getMagnitude() {
        return m;
    }

    public Iterable<FlowPointContribution> getFlowPointContributions() {
        if (contributions == null) {
            throw new UnsupportedOperationException();
        }
        return contributions;
    }

    public FlowPointData immutableCopy() {
        final GeoPoint geoPoint = this.geoPoint;
        final double x = this.x * s, y = this.y * s, z = this.z * s;
        final double i = this.i * s, j = this.j * s, k = this.k * s;
        final double m = this.m * s, p = this.p;
        final List<FlowPointContribution> contributions;
        if (this.contributions != null) {
            contributions = new ArrayList<FlowPointContribution>(this.contributions.size());
            for (FlowPointContribution contribution : AbstractFlowPointData.this.contributions) {
                contributions.add(contribution.immutableCopy());
            }
        } else {
            contributions = null;
        }
        return new FlowPointData() {
            public GeoPoint getGeoPoint() {
                return geoPoint;
            }

            public <P> P getPoint(XForm.View<P> view) {
                return view.convert(x, y, z, 1);
            }

            public <V> V getMagnitudeVector(XForm.View<V> view) {
                return view.convert(i, j, k, 1);
            }

            public <V> V getDirection(XForm.View<V> view) {
                return view.convert(i * m, j * m, k * m, 1);
            }

            public double getMagnitude() {
                return m;
            }

            public double getPressure() {
                return p;
            }

            public Iterable<FlowPointContribution> getFlowPointContributions() {
                if (contributions == null) {
                    throw new UnsupportedOperationException();
                }
                return contributions;
            }

            public FlowPointData immutableCopy() {
                return this;
            }
        };
    }

    private class FragmentConsumer implements XForm.View<Void>, Consumer<Fragment> {

        private double contribution = Double.NaN;
        private ContributionImpl contributionImpl = new ContributionImpl();

        public void consume(Fragment value) {
            contribution = Double.NaN;
            contributionImpl.fragment = value;
            value.getCenterXYZ(this);
            value.getVectorXYZ(this);
        }

        public Void convert(double i, double j, double k, double w) {
            if (Double.isNaN(contribution)) {
                // processing center point
                double l = Vector.computeDistance(x - i, y - j, z - k);
                contribution = calcContribution(l);
            } else {
                contributionImpl.load(contribution, i, j, k);
                consumeContribution(contributionImpl);
                contributionImpl.reset();
            }
            return null;
        }

        protected double calcContribution(double distance) {
            return Math.exp(distance * distance * nominalExponentMultiplier);
        }
    }

    protected class ContributionImpl implements FlowPointContribution {

        private double contribution;
        private double magnitude;
        private double pressure;
        private double i, j, k;
        private Fragment fragment;

        private FlowPointContribution load(double contribution,
                                           double i,
                                           double j,
                                           double k) {
            this.contribution = contribution;
            this.magnitude = Vector.computeDistance(i, j, k);
            this.pressure = fragment.getPressure();
            this.i = i;
            this.j = j;
            this.k = k;
            return this;
        }

        private void reset() {
            contribution = magnitude = pressure = i = j = k = 0;
            fragment = null;
        }

        public double getContribution() {
            return contribution;
        }

        public double getI() {
            return i;
        }

        public double getJ() {
            return j;
        }

        public double getK() {
            return k;
        }

        public double getMagnitude() {
            return magnitude;
        }

        public double getPressure() {
            return pressure;
        }

        public <V> V getVectorIJK(XForm.View<V> view) {
            return view.convert(i, j, k, 1);
        }

        public Fragment getFragment() {
            return fragment;
        }

        public FlowPointContribution immutableCopy() {
            final double contribution = this.contribution;
            final double magnitude = this.magnitude;
            final double pressure = this.pressure;
            final double i = this.i, j= this.j, k = this.k;
            final Fragment fragment = this.fragment;
            return new FlowPointContribution() {
                public double getContribution() {
                    return contribution;
                }

                public double getI() {
                    return i;
                }

                public double getJ() {
                    return j;
                }

                public double getK() {
                    return k;
                }

                public double getMagnitude() {
                    return magnitude;
                }

                public double getPressure() {
                    return pressure;
                }

                public <V> V getVectorIJK(XForm.View<V> view) {
                    return view.convert(i, j, k, 1);
                }

                public Fragment getFragment() {
                    return fragment;
                }

                public FlowPointContribution immutableCopy() {
                    return this;
                }
            };
        }
    }
}
