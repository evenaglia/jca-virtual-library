package net.venaglia.realms.common.map;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Point;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 2:05 PM
 */
public class CubeUtils {

    private static final double cubeSize = 64.0;
    private static final double cubeVolume = cubeSize * cubeSize * cubeSize;
    private static final long[] map;

    static {
        map = new long[256];
        for (int i = 0; i < 256; i++) {
            long v = 0;
            for (int j = 0; j < 7; j++) {
                if ((i & (1 << j)) != 0) {
                    v |= (i << j * 3);
                }
            }
            map[i] = v;
        }
    }

    private CubeUtils() {}

    public static long getCubeID(double x, double y, double z) {
        return toBits(x, toBits(y, toBits(z, 0L)));
    }

    private static long toBits(double a, long merge) {
        return toBits((int)Math.round(a / cubeSize), merge);
    }

    private static long toBits(int a, long merge) {
        int a0 = a & 0xFF;
        int a1 = (a & 0xFF00) >> 8;
        return merge << 1 | map[a1] << 24 | map[a0];
    }

    public static Point getCubeCenter(double x, double y, double z) {
        double cx = Math.round(x / cubeSize) * cubeSize;
        double cy = Math.round(y / cubeSize) * cubeSize;
        double cz = Math.round(z / cubeSize) * cubeSize;
        return new Point(cx, cy, cz);
    }

    public static Point getCubeCenter(long id) {
        int x = 0;
        int y = 0;
        int z = 0;
        for (int i = 0; i < 48; i++) {
            if ((id & (1 << i)) != 0) {
                switch (i % 3) {
                    case 0:
                        x |= 1 << (i / 3);
                        break;
                    case 1:
                        y |= 1 << (i / 3);
                        break;
                    case 2:
                        z |= 1 << (i / 3);
                        break;
                }
            }
        }
        double cx = x * cubeSize;
        double cy = y * cubeSize;
        double cz = z * cubeSize;
        return new Point(cx, cy, cz);
    }

    public static BoundingBox getCubeBounds(double x, double y, double z) {
        double cx = Math.round(x / cubeSize) * cubeSize;
        double cy = Math.round(y / cubeSize) * cubeSize;
        double cz = Math.round(z / cubeSize) * cubeSize;
        double h = cubeSize * 0.5;
        return new BoundingBox(new Point(cx - h, cy- h, cz - h), new Point(cx + h, cy + h, cz + h));
    }

    public static BoundingBox getCubeBounds(long id) {
        int x = 0;
        int y = 0;
        int z = 0;
        for (int i = 0; i < 48; i++) {
            if ((id & (1 << i)) != 0) {
                switch (i % 3) {
                    case 0:
                        x |= 1 << (i / 3);
                        break;
                    case 1:
                        y |= 1 << (i / 3);
                        break;
                    case 2:
                        z |= 1 << (i / 3);
                        break;
                }
            }
        }
        double cx = x * cubeSize;
        double cy = y * cubeSize;
        double cz = z * cubeSize;
        double h = cubeSize * 0.5;
        return new BoundingBox(new Point(cx - h, cy- h, cz - h), new Point(cx + h, cy + h, cz + h));
    }

    public static CubeIterator intersectionIterator(final BoundingVolume<?> bounds) {
        BoundingBox box = bounds.asBox();
        final int ax = (int)Math.round(box.corner1.x / cubeSize);
        final int ay = (int)Math.round(box.corner1.y / cubeSize);
        final int az = (int)Math.round(box.corner1.z / cubeSize);
        final int bx = (int)Math.round(box.corner2.x / cubeSize);
        final int by = (int)Math.round(box.corner2.y / cubeSize);
        final int bz = (int)Math.round(box.corner2.z / cubeSize);
        return new CubeIterator() {

            private boolean valid = false;
            private boolean end = false;
            private long cubeId = Long.MIN_VALUE;
            private BoundingBox box;
            private int cx = ax - 1;
            private int cy = ay;
            private int cz = az;

            public boolean next() {
                if (end) {
                    return false;
                }
                cubeId = Long.MIN_VALUE;
                box = null;
                do {
                    cx++;
                    if (cx > bx) {
                        cx = ax;
                        cy++;
                        if (cy > by) {
                            cy = ay;
                            cz++;
                            if (cz > bz) {
                                end = true;
                                valid = false;
                                return false;
                            }
                        }
                    }
                } while (!intersects(bounds));
                valid = true;
                return true;
            }

            public long getCubeId() {
                ensureValid();
                if (cubeId == Long.MIN_VALUE) {
                    cubeId = toBits(cx, toBits(cy, toBits(cz, 0L)));
                }
                return cubeId;
            }

            public BoundingBox getCubeBounds() {
                ensureValid();
                if (box == null) {
                    double h = cubeSize * 0.5;
                    double x = cx * cubeSize;
                    double y = cy * cubeSize;
                    double z = cz * cubeSize;
                    box = new BoundingBox(new Point(x - h, y- h, z - h), new Point(x + h, y + h, z + h));
                }
                return box;
            }

            private boolean intersects(BoundingVolume<?> bounds) {
                Point p = bounds.center();
                double h = cubeSize * 0.5;
                double x = cx * cubeSize;
                double y = cy * cubeSize;
                double z = cz * cubeSize;
                x = Math.max(x - h, Math.min(x + h, p.x));
                y = Math.max(y - h, Math.min(y + h, p.y));
                z = Math.max(z - h, Math.min(z + h, p.z));
                return bounds.includes(x, y, z);
            }

            private void ensureValid() {
                if (!valid) {
                    throw new IllegalStateException();
                }
            }
        };
    }

    public static String toString(long id) {
        return String.format("%012x", id);
    }

    public static double getCubeVolume() {
        return cubeVolume;
    }

    public static interface CubeIterator {
        boolean next();
        long getCubeId();
        BoundingBox getCubeBounds();
    }
}
