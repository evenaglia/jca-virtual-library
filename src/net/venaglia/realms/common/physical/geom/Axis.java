package net.venaglia.realms.common.physical.geom;

/**
 * User: ed
 * Date: 9/15/12
 * Time: 12:53 PM
 */
public enum Axis {
    X {
        @Override
        public double of(double x, double y, double z) {
            return x;
        }
    },
    Y {
        @Override
        public double of(double x, double y, double z) {
            return y;
        }
    },
    Z {
        @Override
        public double of(double x, double y, double z) {
            return z;
        }
    };

    public abstract double of(double x, double y, double z);

    public Axis next() {
        switch (this) {
            case X:
                return Y;
            case Y:
                return Z;
            case Z:
                return X;
        }
        throw new IllegalStateException();
    }

    public Axis prev() {
        switch (this) {
            case X:
                return Z;
            case Y:
                return X;
            case Z:
                return Y;
        }
        throw new IllegalStateException();
    }

    public Vector vector() {
        switch (this) {
            case X:
                return Vector.X;
            case Y:
                return Vector.Y;
            case Z:
                return Vector.Z;
        }
        throw new IllegalStateException();
    }
}
