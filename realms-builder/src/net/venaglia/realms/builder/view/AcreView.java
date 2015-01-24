package net.venaglia.realms.builder.view;

import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.projection.ColorBuffer;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.Projectable;
import net.venaglia.gloo.projection.impl.CoordinatesListBuilder;
import net.venaglia.realms.common.map.VertexStore;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.common.util.Visitor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * User: ed
 * Date: 12/10/14
 * Time: 8:35 AM
 */
public class AcreView implements Projectable {

    private final AcreViewGeometry geometry;
    private final AcreRendererImpl acreRenderer = new AcreRendererImpl();
    private final String name;

    public AcreView(AcreViewGeometry geometry, String name) {
        if (geometry == null) {
            throw new NullPointerException("geometry");
        }
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!geometry.done) {
            throw new IllegalStateException("Cannot create an AcreView with an open AcreViewGeometry object. " +
                                            "Call AcreViewGeometry.close() first.");
        }
        this.geometry = geometry;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public void project(long nowMS, GeometryBuffer buffer) {
        try {
            buffer.coordinates(geometry.zoneVertices, new GeometryBuffer.Drawable() {
                @Override
                public void draw(GeometryBuffer.CoordinateListGeometryBuffer buffer) {
                    acreRenderer.setCoordinateListGeometryBuffer(buffer);
                    for (int i = 0; i < geometry.acreVertices.length; i++) {
                        IntBuffer[] intBuffers = geometry.acreVertices[i];
                        renderAcre(i, acreRenderer);
                        for (IntBuffer intBuffer : intBuffers) {
                            buffer.draw(GeometryBuffer.GeometrySequence.TRIANGLE_STRIP, intBuffer);
                        }
                    }
                }
            });
        } finally {
            acreRenderer.setCoordinateListGeometryBuffer(null);
        }
    }

    protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
        // no-op, override to implement acre specific details
    }

    @Override
    public String toString() {
        return "AcreView['" + name + "']";
    }

    public interface AcreRenderer extends ColorBuffer {
    }

    private static class AcreRendererImpl implements AcreRenderer {

        private GeometryBuffer.CoordinateListGeometryBuffer coordinateListGeometryBuffer;

        private void setCoordinateListGeometryBuffer(GeometryBuffer.CoordinateListGeometryBuffer coordinateListGeometryBuffer) {
            this.coordinateListGeometryBuffer = coordinateListGeometryBuffer;
        }

        @Override
        public void color(Color color) {
            coordinateListGeometryBuffer.color(color);
        }

        @Override
        public void color(float r, float g, float b) {
            coordinateListGeometryBuffer.color(r, g, b);
        }

        @Override
        public void colorAndAlpha(Color color) {
            coordinateListGeometryBuffer.colorAndAlpha(color);
        }

        @Override
        public void colorAndAlpha(float r, float g, float b, float a) {
            coordinateListGeometryBuffer.colorAndAlpha(r, g, b, a);
        }
    }

    public static class AcreViewGeometry implements Visitor<AcreDetail> {

        private static final int[][] MAP_6_SIDES = {
                {4,11,17,5,18,12,6},
                {10,4,16,17,0,18,13,6,7},
                {7,1,13,14,0,15,16,3,10},
                {1,8,14,2,15,9,3}
        };

        private static final int[][] MAP_5_SIDES = {
                {6,11,5,15,10,4},
                {11,0,15,14,4,9},
                {6,1,11,12,0,13,14,3,9},
                {1,7,12,2,13,8,3}
        };

        private final CoordinateList zoneVertices;
        private final IntBuffer[][] acreVertices;
        private final int acreCount;

        private boolean done = false;

        public AcreViewGeometry(VertexStore vertexStore, int acreCount, long globalPointCount, double radius) {
            assert globalPointCount < 32500000L;
            final CoordinatesListBuilder builder =
                    new CoordinatesListBuilder(CoordinateList.Field.VERTEX, CoordinateList.Field.NORMAL);
            RangeBasedLongSet ids = new RangeBasedLongSet();
            ids.addAll(0, globalPointCount);
            System.out.println("Caching vertices...");
            vertexStore.read(ids, new VertexStore.VertexConsumer() {

                private int count = 0;

                @Override
                public void next(int rgbColor, double i, double j, double k, float elevation) {
                    if (count++ >= 16384) {
                        count -= 16384;
                        System.out.print(".");
                    }
                    builder.normal(i, j, k);
                    builder.vertex(i * radius, j * radius, k * radius);
                }

                @Override
                public void done() {
                }
            });
            System.out.println();
            this.zoneVertices = builder.build();
            this.acreVertices = new IntBuffer[acreCount][];
            this.acreCount = acreCount;
        }

        @Override
        public void visit(AcreDetail acre) throws RuntimeException {
            if (done) {
                throw new IllegalStateException();
            }
            long[] vertexIds = acre.getAcreTopographyDef();
            int[][] pointMap;
            if (vertexIds.length == 19) {
                pointMap = MAP_6_SIDES;
            } else if (vertexIds.length == 16) {
                pointMap = MAP_5_SIDES;
            } else {
                throw new IllegalArgumentException("Unexpected number of points in acreTopographyDef: " + vertexIds.length);
            }
            int id = acre.getId();
            assert id >= 0;
            assert id < acreVertices.length;
            if (acreVertices[id] != null) {
                throw new IllegalStateException("Cannot visit AcreDetail with id " + id + " more than once");
            }
            acreVertices[id] = buildBuffers(pointMap, vertexIds);
        }

        public void close() {
            if (!done) {
                for (int i = 0; i < acreVertices.length; i++) {
                    if (acreVertices[i] == null) {
                        throw new IllegalStateException("No AcreDetail was visited for id " + i);
                    }
                }
                done = true;
            }
        }

        private IntBuffer[] buildBuffers(int[][] pointMap, long[] vertexIds) {
            IntBuffer[] result = new IntBuffer[pointMap.length];
            for (int i = 0; i < pointMap.length; i++) {
                result[i] = buildBuffer(pointMap[i], vertexIds);
            }
            return result;
        }

        private IntBuffer buildBuffer(int[] pointMap, long[] vertexIds) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(pointMap.length * (Integer.SIZE >> 3));
            buffer.order(ByteOrder.nativeOrder());
            for (int i : pointMap) {
                buffer.putInt((int)vertexIds[i]);
            }
            buffer.flip();
            return buffer.asIntBuffer();
        }
    }
}
