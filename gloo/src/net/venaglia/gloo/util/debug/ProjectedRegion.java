package net.venaglia.gloo.util.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 8/21/14
 * Time: 5:37 PM
 */
public abstract class ProjectedRegion<P> {

    private final OutputGraphProjection<P> projection;

    private boolean open = true;
    private boolean fromLastElement = false;
    private List<P> points = new ArrayList<P>();

    ProjectedRegion(OutputGraphProjection<P> projection) {
        this.projection = projection;
    }

    public ProjectedRegion<P> fromLastElement() {
        if (!open) {
            throw new IllegalStateException("Region is already closed");
        }
        if (!points.isEmpty()) {
            throw new IllegalStateException("fromLastElement() cannot be called after addPoints()");
        }
        fromLastElement = true;
        open = false;
        onClose();
        return this;
    }

    public ProjectedRegion<P> addPoints(Iterable<P> points) {
        if (!open) {
            throw new IllegalStateException("Region is already closed");
        }
        for (P point : points) {
            this.points.add(point);
        }
        return this;
    }

    public ProjectedRegion<P> addPoint(P point) {
        if (!open) {
            throw new IllegalStateException("Region is already closed");
        }
        points.add(point);
        return this;
    }

    public ProjectedRegion<P> close() {
        if (open) {
            if (size() < 3 && size() != 1) {
                throw new IllegalStateException("Too few points: " + size());
            }
            open = false;
            onClose();
        }
        return this;
    }

    public int size() {
        return points.size();
    }

    abstract void onClose();

    Region toRegion() {
        if (open) {
            throw new IllegalStateException("ProjectedRegion is still open, close it first");
        }
        if (fromLastElement) {
            return Region.fromLastElement();
        }
        if (size() == 1) {
            double[] point = {0,0};
            projection.project(points.get(0), point, 0);
            return new Region().addPoint(point[0], point[1]).close();
        }
        final double[] result = new double[points.size() * 2];
        for (int i = 0, j = 0, l = points.size(); i < l; i++, j += 2) {
            projection.project(points.get(i), result, j);
        }
        return new Region() {

            {
                open = false;
            }

            @Override
            public int size() {
                return result.length >> 1;
            }

            @Override
            double[] getBounds() {
                return result;
            }

            @Override
            public String toString() {
                StringBuilder buffer = new StringBuilder();
                for (int i = 0, l = result.length; i < l; i += 2) {
                    if (i > 0) {
                        buffer.append("-");
                    }
                    buffer.append(String.format("(%.4f,%.4f)", result[i], result[i + 1]));
                }
                return buffer.toString();
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (P point : points) {
            if (buffer.length() > 0) {
                buffer.append("-");
            }
            buffer.append(point);
        }
        return buffer.toString();
    }
}
