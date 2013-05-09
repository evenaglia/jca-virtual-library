package net.venaglia.realms.common.physical.geom.base;

import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Faceted;
import net.venaglia.realms.common.physical.geom.Point;

/**
 * User: ed
 * Date: 9/1/12
 * Time: 5:07 PM
 */
public abstract class AbstractFacetedShape<T extends AbstractFacetedShape<T>> extends AbstractShape<T> implements Faceted {

    protected static final ThreadLocal<FacetBuilder> FACET_BUILDER = new ThreadLocal<FacetBuilder>() {
        @Override
        protected FacetBuilder initialValue() {
            return new FacetBuilder();
        }
    };

    protected AbstractFacetedShape(Point[] points) {
        super(points);
    }

    public Facet getFacet(int index) {
        if (index >= 0 && index < facetCount()) {
            FacetBuilder facetBuilder = FACET_BUILDER.get();
            facetBuilder.reset(getFacetType());
            findFacetPoints(index, facetBuilder);
            return facetBuilder.apply(this);
        }
        throw new IllegalArgumentException("Facet index must be between 0 and " + facetCount() + ", inclusive: " + index);
    }

    protected abstract void findFacetPoints(int index, FacetBuilder facetBuilder);

    protected static class FacetBuilder {

        private Facet.Type type = null;
        private int a, b, c, d;
        private int[] more;
        private boolean set = false;

        private void reset(Facet.Type type) {
            if (type == null) {
                throw new NullPointerException("type");
            }
            a = -1;
            b = -1;
            c = -1;
            d = -1;
            more = null;
            set = false;
            this.type = type;
        }

        private void assertType(Facet.Type type) {
            if (type == Facet.Type.MIXED) {
                throw new UnsupportedOperationException("This FacetBuilder does not support " + type);
            } else if (this.type == Facet.Type.MIXED) {
                this.type = type;
            } else if (type != this.type) {
                throw new UnsupportedOperationException("This FacetBuilder does not support " + type);
            }
        }

        public void usePoints(int a, int b, int c) {
            assertType(Facet.Type.TRIANGLE);
            this.a = a;
            this.b = b;
            this.c = c;
            set = true;
        }

        public void usePoints(int a, int b, int c, int d) {
            assertType(Facet.Type.QUAD);
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            set = true;
        }

        public void usePoints(int a, int b, int c, int d, int... more) {
            assertType(Facet.Type.POLY);
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.more = more;
            set = true;
        }

        public Facet apply(AbstractFacetedShape<?> shape) {
            if (!set) {
                return null;
            }
            set = false;
            Point[] all = shape.points;
            if (type == Facet.Type.POLY) {
                Point[] more = new Point[this.more.length];
                for (int i = 0; i < this.more.length; i++) {
                    more[i] = all[this.more[i]];
                }
                return new Facet(all[a], all[b], all[c], all[d], more);
            }
            return type == Facet.Type.TRIANGLE
                   ? new Facet(all[a], all[b], all[c])
                   : new Facet(all[a], all[b], all[c], all[d]);
        }
    }
}
