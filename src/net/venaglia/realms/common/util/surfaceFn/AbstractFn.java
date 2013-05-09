package net.venaglia.realms.common.util.surfaceFn;

import static net.venaglia.realms.common.physical.geom.ZMap.Fn;

/**
 * User: ed
 * Date: 2/12/13
 * Time: 10:01 PM
 */
public abstract class AbstractFn implements Fn {

    public AbstractFn translate(final double dx, final double dy, final double dz) {
        return new AbstractFn() {
            public double getZ(double x, double y) {
                return AbstractFn.this.getZ(x - dx, y - dy) - dz;
            }
        };
    }

    public AbstractFn scale(final double sx, final double sy, final double sz) {
        return new AbstractFn() {
            public double getZ(double x, double y) {
                return AbstractFn.this.getZ(x * sx, y * sy) * sz;
            }
        };
    }

    public AbstractFn sum(final AbstractFn other) {
        return new AbstractFn() {
            public double getZ(double x, double y) {
                return AbstractFn.this.getZ(x, y) + other.getZ(x, y);
            }
        };
    }

    public NurbsBuilder buildNurbs() {
        NurbsBuilder builder = new NurbsBuilder();
        builder.setFn(this);
        return builder;
    }
}
