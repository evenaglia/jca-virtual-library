package net.venaglia.gloo.physical.geom;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 10:40 PM
 */
public interface XForm {

    XForm IDENTITY = new XForm() {
        public Vector apply(Vector vector) {
            return vector;
        }

        public Point apply(Point point) {
            return point;
        }

        public Vector[] apply(Vector[] vectors) {
            return vectors.clone();
        }

        public Point[] apply(Point[] points) {
            return points.clone();
        }

        public boolean isSymmetric() {
            return true;
        }

        public boolean isOrthogonal() {
            return true;
        }
    };

    Vector apply(Vector vector);

    Point apply(Point point);

    Vector[] apply(Vector[] vectors);

    Point[] apply(Point[] points);

    boolean isSymmetric();

    boolean isOrthogonal();
}
