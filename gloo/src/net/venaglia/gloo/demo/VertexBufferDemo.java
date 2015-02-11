package net.venaglia.gloo.demo;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.lights.FixedPointSourceLight;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.camera.PerspectiveCamera;
import net.venaglia.gloo.projection.impl.CoordinateListBuilder;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;
import net.venaglia.gloo.view.ViewEventHandler;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * User: ed
 * Date: 1/7/15
 * Time: 5:37 PM
 */
public class VertexBufferDemo {

    private CoordinateList coordinates;
    private ShortBuffer[] drawIndices;
    private double scale;
    private double waveVelocity;

    public void start(final int size, boolean wireFrame, double waveVelocity) {
//        final double timescale = 0.0667;
        final double timescale = 0.667;
        scale = 1.0 / ((double)size);
        this.waveVelocity = waveVelocity;
        Vector center = new Vector(-0.5, -0.5, 0.0);

        final View3D view3D = new View3D(1024,768);
        CoordinateListBuilder builder = new CoordinateListBuilder(CoordinateList.Field.VERTEX, CoordinateList.Field.NORMAL);
        builder.setMutable(true);
        if (size > 180) {
            throw new IllegalArgumentException("size is too big, cannot be greater than 180: " + size);
        }
        for (int i = 0; i <= size; i++) {
            for (int j = 0; j <= size; j++) {
                builder.normal(Vector.Z);
                builder.vertex(new Point(i, j, 0).scale(scale).translate(center));
            }
        }
        coordinates = builder.build();

        drawIndices = new ShortBuffer[size];
        for (int i = 0; i < size; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder());
            for (int j = 0; j < size; j++) {
                buffer.putShort((short)(j + i * (size + 1)));
                buffer.putShort((short)(j + (i + 1) * (size + 1)));
            }
            buffer.position(0);
            drawIndices[i] = buffer.asShortBuffer();
        }

        final Camera camera = new PerspectiveCamera();
        camera.setPosition(new Point(0,0,4));
        camera.setDirection(new Vector(0,0,-4));
        camera.setRight(new Vector(-2,0,0));
        camera.setClippingDistance(0.1f,40.0f);

        Brush brush = new Brush();
        brush.setLighting(!wireFrame);
        brush.setColor(true);
        brush.setPolygonFrontFace(wireFrame ? Brush.PolygonMode.LINE : Brush.PolygonMode.FILL);
        brush.setPolygonBackFace(wireFrame ? Brush.PolygonMode.LINE : Brush.PolygonMode.FILL);
        brush.setCulling(wireFrame ? null : Brush.PolygonSide.BACK);

        final Light[] lights = {
                new FixedPointSourceLight(new Point(1.1f, 5.0f, 3.5f)),
                new FixedPointSourceLight(new Point(-2.1f, 0.0f, 1.5f)),
                new FixedPointSourceLight(new Point(-0.1f, -4.0f, -2.5f))
        };

        view3D.setCamera(camera);
        view3D.setDefaultBrush(brush);
        view3D.setMainLoop(new View3DMainLoop() {

            private int frameCounter = 0;
            private int l = size + 1;
            private double a = Math.PI, b = 0.0;
            private double[] z = new double[(size + 3) * (size + 3)];

            @Override
            public boolean beforeFrame(long nowMS) {
//                if (frameCounter++ > 1) {
//                    return false;
//                }
                ByteBuffer data = coordinates.data(CoordinateList.Field.VERTEX);
                updateZ(nowMS);
                int step = coordinates.recordSize(CoordinateList.Field.VERTEX);
                int position = 16; // skip to z coordinate
                for (int i = 0, k = 0; i <= size; i++) {
                    for (int j = 0; j <= size; j++) {
                        data.putDouble(position, z[k++]);
                        position += step;
                    }
                }
                for (int i = 0, k = 0; i <= size; i++) {
                    for (int j = 0; j <= size; j++) {
                        coordinates.set(k++, calculateNormal(i, j));
                    }
                }
                double z = Math.sin(b);
                double x = Math.sin(a) * (1.0 - z);
                double y = Math.cos(a) * (1.0 - z);
                Point c = new Point(x,y,z).scale(4);
                camera.setPosition(c);
                Vector d = DemoObjects.toVector(c, new Point(0,0,-0.25)).normalize();
                camera.setDirection(d);
                camera.setRight(new Vector(0,0,1.0).cross(d).normalize(0.4));
                a -= 0.015 * timescale;
                b += 0.0003 * timescale;
                return true;
            }

            private void updateZ(long nowMS) {
                for (int i = -1, k = 0; i <= l; i++) {
                    for (int j = -1; j <= l; j++) {
                        z[k++] = calculateZ(i, j, nowMS);
                    }
                }
            }

            private double getZ(int x, int y) {
                return z[(x + 1) * l + (y + 1)];
            }

            private Vector calculateNormal(int x, int y) {
                double z0 = getZ(x - 1, y);
                double z1 = getZ(x + 1, y);
                double z2 = getZ(x, y - 1);
                double z3 = getZ(x, y + 1);
                Vector v1 = new Vector(scale, 0.0, z1 - z0);
                Vector v2 = new Vector(0.0, scale, z3 - z2);
                return v1.cross(v2).normalize();
            }

            @Override
            public MouseTargets getMouseTargets(long nowMS) {
                return null;
            }

            @Override
            public void renderFrame(long nowMS, ProjectionBuffer buffer) {
                buffer.useLights(lights);
                buffer.color(Color.BLUE);
                buffer.pushTransform();
                buffer.rotate(Axis.Y, 0.18);
                buffer.coordinates(coordinates, new GeometryBuffer.Drawable() {
                    @Override
                    public void draw(GeometryBuffer.CoordinateListGeometryBuffer buffer) {
                        for (ShortBuffer indices : drawIndices) {
                            buffer.draw(GeometryBuffer.GeometrySequence.QUAD_STRIP, indices);
                        }
                    }
                });

//                buffer.applyBrush(Brush.SELF_ILLUMINATED);
//                buffer.color(Color.GREEN);
//                buffer.start(GeometryBuffer.GeometrySequence.LINES);
//                for (int i = 0, l = coordinates.size(); i < l; i++) {
//                    Coordinate coordinate = coordinates.get(i);
//                    buffer.vertex(coordinate.getVertex());
//                    buffer.vertex(coordinate.getVertex().translate(coordinate.getNormal()));
//                }
//                buffer.end();

                buffer.popTransform();
            }

            @Override
            public void renderOverlay(long nowMS, GeometryBuffer buffer) {
            }

            @Override
            public void afterFrame(long nowMS) {
            }
        });

        view3D.addViewEventHandler(new ViewEventHandler() {
            public void handleInit() {
            }

            public void handleClose() {
                System.exit(0);
            }

            public void handleNewFrame(long now) {
                // no-op
            }
        });
        view3D.start();
    }

    private double calculateZ(int i, int j, long nowMS) {
        double x = i * scale * 32.0;
        double y = j * scale * 32.0;
        int now = (int)(nowMS & 0x3FFF);
        double theta1 = (x * 0.15 + y * 0.03 + now * waveVelocity) * Math.PI * 2.0;
        double theta2 = (x * -0.08 + y * 0.07 + now * waveVelocity) * Math.PI * 2.0;
        return (Math.sin(theta1) + Math.sin(theta2)) * 0.015625 - 0.25;
    }

    public static void main(String[] args) {
        new VertexBufferDemo().start(128, false, 1.0 / 1024.0);
    }
}
