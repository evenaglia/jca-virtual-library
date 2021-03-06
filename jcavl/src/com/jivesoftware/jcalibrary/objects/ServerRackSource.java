package com.jivesoftware.jcalibrary.objects;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.CompositeShape;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.detail.DynamicDetailSource;
import net.venaglia.gloo.physical.geom.primitives.Box;
import net.venaglia.gloo.projection.DisplayList;
import net.venaglia.gloo.projection.GeometryRecorder;
import net.venaglia.gloo.projection.RecordingBuffer;
import net.venaglia.gloo.projection.impl.DisplayListBuffer;

/**
 * User: ed
 * Date: 4/23/13
 * Time: 5:42 PM
 */
public class ServerRackSource implements DynamicDetailSource<DisplayList> {

    public static final float HEIGHT = 11.4166f;
    public static final float WIDTH = 10.0f;
    public static final float DEPTH = 1.6666f;
    public static final float LOWEST_SHELF = 0.4166f;
    public static final int SHELVES = 11;

    private Shape<?> target;
//    private Material material = Material.makeSelfIlluminating(Color.CYAN);
    private Material material = Material.INHERIT;
//    private Material material = Material.makeWireFrame(Color.CYAN);

    public ServerRackSource() {
        target = new Box(new BoundingBox(new Point(WIDTH * -0.5, DEPTH * 0.4, HEIGHT + (HEIGHT - LOWEST_SHELF) / SHELVES),
                                         new Point(WIDTH * 0.5, DEPTH * -0.4, LOWEST_SHELF)));
    }

    public float getSizeFactor() {
        return 0.125f;
    }

    public DisplayList produceAt(DetailLevel detailLevel) {
        final CompositeShape delegate = new CompositeShape();
//        Shape<?> end = new RoundedRectangle3D(HEIGHT, DEPTH, 0.125, 0.0125, 0, detailLevel);
//        end = end.rotate(Axis.Y, Math.PI * 0.5).translate(Vector.Z.scale(HEIGHT * 0.5));
//        delegate.addShape(end.translate(Vector.X.scale(WIDTH * -0.5)));
//        delegate.addShape(end.translate(Vector.X.scale(WIDTH * 0.5)));

        Shape<?> shelf = new RoundedRectangle3D(WIDTH - 0.25, DEPTH, 0.125, 0.025, 0, detailLevel);
        for (int i = 0; i < SHELVES; i++) {
            double z = i * (HEIGHT - LOWEST_SHELF) / (SHELVES - 1) + LOWEST_SHELF;
            delegate.addShape(shelf.translate(Vector.Z.scale(z)));
        }
        delegate.setMaterial(material);
        delegate.inheritMaterialToContainedShapes();

        DisplayList displayList = new DisplayListBuffer("Shelf - " + detailLevel);
        displayList.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                buffer.setBrush(Brush.SELF_ILLUMINATED);
                delegate.project(0, buffer);
            }
        });
        return displayList;
    }

    public boolean isStatic() {
        return true;
    }

    public Shape<?> getTarget() {
        return target;
    }
}
