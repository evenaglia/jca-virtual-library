package net.venaglia.realms.demo;

import static net.venaglia.realms.common.util.CallLogger.logCalls;

import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.projection.DisplayList;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.projection.impl.DisplayListBuffer;

/**
 * User: ed
 * Date: 3/13/13
 * Time: 12:03 PM
 */
public class SingleShapeDemo extends AbstractDemo {

    public enum Mode {
        POINTS, WIREFRAME, SHADED
    }

    private final String title;
    private final Shape<?> shape;
    private final BoundingVolume<?> bounds;
    private final double cameraDistance;

    private DisplayList displayList;

    public SingleShapeDemo(Shape<?> shape, Color color, Mode mode) {
        this(applyColor(shape, color, mode));
    }

    public SingleShapeDemo(Shape<?> shape, Texture texture, TextureMapping mapping) {
        this(applyTexture(shape, texture, mapping));
    }

    public SingleShapeDemo(Shape<?> shape) {
        this.title = shape.getClass().getSimpleName();
        this.shape = shape;
        this.bounds = shape.getBounds();
        this.cameraDistance = bounds.asSphere().radius * 6.0;
    }

    @Override
    protected String getTitle() {
        return title;
    }

    @Override
    protected void init() {
        displayList = new DisplayListBuffer("shape");
        displayList.record(new DisplayList.DisplayListRecorder() {
            public void record(GeometryBuffer buffer) {
                shape.project(0, buffer);
            }
        });
    }

    @Override
    public void afterFrame(long nowMS) {
        if (logCalls) {
            System.exit(0);
        }
    }

    @Override
    protected double getCameraDistance() {
        return cameraDistance;
    }

    @Override
    protected BoundingVolume<?> getRenderingBounds() {
        return bounds;
    }

    @Override
    protected void project(long nowMS, ProjectionBuffer buffer) {
        if (displayList != null) {
            displayList.project(nowMS, buffer);
        } else {
            shape.project(nowMS, buffer);
        }
    }

    private static Shape<?> applyColor(Shape<?> shape, Color color, Mode mode) {
        switch (mode) {
            case POINTS:
                shape.setMaterial(Material.makePoints(color));
                break;
            case WIREFRAME:
                shape.setMaterial(Material.makeWireFrame(color));
                break;
            case SHADED:
                shape.setMaterial(Material.makeFrontShaded(color));
                break;
        }
        return shape;
    }

    private static Shape<?> applyTexture(Shape<?> shape, Texture texture, TextureMapping mapping) {
        shape.setMaterial(Material.makeTexture(texture, mapping));
        return shape;
    }

}
