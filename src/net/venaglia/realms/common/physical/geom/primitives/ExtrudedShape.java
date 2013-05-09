package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 4/15/13
 * Time: 3:25 PM
 */
public abstract class ExtrudedShape<T extends ExtrudedShape<T>> extends AbstractShape<T> {

    protected final Vector[] normals;
    protected final DetailLevel detailLevel;

    protected ExtrudedShape(double[] controlPoints, double thickness, DetailLevel detailLevel) {
        this(generatePoints(controlPoints, thickness, detailLevel),
             generateNormals(controlPoints, detailLevel),
             detailLevel);
    }

    protected ExtrudedShape(Point[] points, Vector[] normals, DetailLevel detailLevel) {
        super(points);
        this.normals = normals;
        this.detailLevel = detailLevel;
    }

    private static Point[] generatePoints(double[] controlPoints, double thickness, DetailLevel detailLevel) {
        Point[] basePoints;
        if (detailLevel == null) {
            if (controlPoints.length % 2 != 0 || controlPoints.length < 6) {
                throw new IllegalArgumentException("controlPoints must contain at least three sets of points (x,y) that define a closed polygon");
            }
            basePoints = new Point[(controlPoints.length >> 1) + 1];
            int k = 1;
            for (int i = 0; i < controlPoints.length; i += 2) {
                basePoints[k++] = new Point(controlPoints[i], 0, controlPoints[i + 1]);
            }
        } else if (controlPoints.length % 6 != 0 || controlPoints.length < 12) {
            throw new IllegalArgumentException("controlPoints must contain at least two sets of control point (x,y) triplets that define a closed bezier curve");
        } else {
            int segments = detailLevel.steps;
            basePoints = new Point[(segments + 1) * (controlPoints.length / 6) + 1];
            int k = 1;
            for (int j = 0; j < controlPoints.length; j += 6) {
                int s = 0;
                double ax = controlPoints[(j + s++) % controlPoints.length];
                double ay = controlPoints[(j + s++) % controlPoints.length];
                double bx = controlPoints[(j + s++) % controlPoints.length];
                double by = controlPoints[(j + s++) % controlPoints.length];
                double cx = controlPoints[(j + s++) % controlPoints.length];
                double cy = controlPoints[(j + s++) % controlPoints.length];
                double dx = controlPoints[(j + s++) % controlPoints.length];
                double dy = controlPoints[(j + s  ) % controlPoints.length];
                for (int i = 0; i <= segments; i++) {
                    double p = (double)(segments - i) / segments;
                    double q = 1.0 - p;
                    double i1x = ax * p + bx * q;
                    double i1y = ay * p + by * q;
                    double i2x = bx * p + cx * q;
                    double i2y = by * p + cy * q;
                    double i3x = cx * p + dx * q;
                    double i3y = cy * p + dy * q;
                    double i4x = i1x * p + i2x * q;
                    double i4y = i1y * p + i2y * q;
                    double i5x = i2x * p + i3x * q;
                    double i5y = i2y * p + i3y * q;
                    double x = i4x * p + i5x * q;
                    double y = i4y * p + i5y * q;
                    basePoints[k++] = new Point(x, 0, y);
                }
            }
        }
        basePoints[0] = Point.ORIGIN;

        Vector[] xlateVectors = {
                new Vector(0, thickness * 0.5, 0),
                new Vector(0, thickness * -0.5, 0)
        };
        Point[] points = new Point[basePoints.length * xlateVectors.length];
        for (int i = 0, j = 0, l = basePoints.length; i < l; i++) {
            for (Vector xlate : xlateVectors) {
                points[j++] = basePoints[i].translate(xlate);
            }
        }
        return points;
    }

    private static Vector[] generateNormals(double[] controlPoints, DetailLevel detailLevel) {
        Vector[] normals;
        if (detailLevel == null) {
            int l = controlPoints.length;
            normals = new Vector[l + 2];
            int k = 2;
            for (int j = 0; j < l; j += 2) {
                double ax = ele(controlPoints, j);
                double ay = ele(controlPoints, j + 1);
                double bx = ele(controlPoints, j + 2);
                double by = ele(controlPoints, j + 3);
                double x = by - ay;
                double y = ax - bx;
                Vector normal = new Vector(x, 0, y).normalize();
                normals[k++] = normal;
                normals[k++] = normal;
            }
        } else {
            normals = new Vector[(controlPoints.length / 6 + 1) * (detailLevel.steps + 1) * 2 + 2];
            int segments = detailLevel.steps;
            int k = 2;
            for (int j = 0; j <= controlPoints.length; j += 6) {
                int s = 0;
                double ax = ele(controlPoints, j + s++);
                double ay = ele(controlPoints, j + s++);
                double bx = ele(controlPoints, j + s++);
                double by = ele(controlPoints, j + s++);
                double cx = ele(controlPoints, j + s++);
                double cy = ele(controlPoints, j + s++);
                double dx = ele(controlPoints, j + s++);
                double dy = ele(controlPoints, j + s  );
                {
                    double x = ay - by;
                    double y = bx - ax;
                    Vector normal = new Vector(x, 0, y).normalize();
                    normals[k++] = normal;
                    normals[k++] = normal;
                }
                for (int i = 1; i < segments; i++) {
                    double p = (double)(segments - i) / segments;
                    double q = 1.0 - p;
                    double i1x = ax * p + bx * q;
                    double i1y = ay * p + by * q;
                    double i2x = bx * p + cx * q;
                    double i2y = by * p + cy * q;
                    double i3x = cx * p + dx * q;
                    double i3y = cy * p + dy * q;
                    double i4x = i1x * p + i2x * q;
                    double i4y = i1y * p + i2y * q;
                    double i5x = i2x * p + i3x * q;
                    double i5y = i2y * p + i3y * q;
                    double x = i4y - i5y;
                    double y = i5x - i4x;
                    Vector normal = new Vector(x, 0, y).normalize();
                    normals[k++] = normal;
                    normals[k++] = normal;
                }
                {
                    double x = cy - dy;
                    double y = dx - cx;
                    Vector normal = new Vector(x, 0, y).normalize();
                    normals[k++] = normal;
                    normals[k++] = normal;
                }
            }
        }
        normals[0] = Vector.Y.reverse();
        normals[1] = Vector.Y;
        return normals;
    }

    public Vector getNormal(int index) {
        return normals[index];
    }

    @Override
    protected final T build(Point[] points, XForm xForm) {
        return build(points,
                     xForm.apply(normals),
                     xForm
        );
    }

    protected abstract T build(Point[] points,
                               Vector[] normals,
                               XForm xForm);

    @Override
    protected void project(GeometryBuffer buffer) {
        projectFaces(buffer);
        int l = points.length;
        if (detailLevel == null) {
            buffer.start(GeometryBuffer.GeometrySequence.QUADS);
            for (int i = 2; i < l; i += 2) {
                buffer.normal(ele(normals, i));
                buffer.vertex(ele(points, i));
                buffer.vertex(ele(points, i + 1));
                buffer.vertex(ele(points, i + 3));
                buffer.vertex(ele(points, i + 2));
            }
            buffer.end();
        } else {
            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            for (int i = 0; i < l; i += 2) {
                buffer.normal(ele(normals, i));
                buffer.vertex(ele(points, i));
                buffer.vertex(ele(points, i + 1));
            }
            buffer.end();
        }
    }

    protected static <T> T ele(T[] elements, int index) {
        int l = elements.length;
        int i = (index + l) % (l - 2);
        return elements[i + 2];
    }

    private static double ele(double[] elements, int index) {
        int i = index % elements.length;
        return elements[i];
    }

    protected void projectFaces(GeometryBuffer buffer) {
        int l = points.length;
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_FAN);
        buffer.normal(normals[0]);
        for (int i = 0; i < l; i += 2) {
            buffer.vertex(points[i]);
        }
        buffer.vertex(points[2]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_FAN);
        buffer.normal(normals[1]);
        buffer.vertex(points[1]);
        for (int i = l - 1; i > 2; i -= 2) {
            buffer.vertex(points[i]);
        }
        buffer.vertex(points[l - 1]);
        buffer.end();
    }
}
