package net.venaglia.realms.common.physical.decorators;

import static org.lwjgl.opengl.GL11.*;

/**
 * User: ed
 * Date: 9/6/12
 * Time: 9:49 PM
 *
 * A Brush describes everything but the color and texture of what is to be drawn.
 */
public class Brush {

    public static final Brush GL_DEFAULTS;
    public static final Brush NO_LIGHTING;
    public static final Brush WIRE_FRAME;
    public static final Brush POINTS;
    public static final Brush FRONT_SHADED;
    public static final Brush TEXTURED;

    static {
        Brush glDefaults = new Brush();
        glDefaults.setLighting(false);
        glDefaults.setColor(false);
        glDefaults.setTexturing(false);
        GL_DEFAULTS = glDefaults.immutable();
        Brush noLighting = new Brush();
        noLighting.setLighting(false);
        noLighting.setTexturing(false);
        NO_LIGHTING = noLighting.immutable();
        Brush wireframe = new Brush();
        wireframe.setLighting(false);
        wireframe.setTexturing(false);
        wireframe.setPolygonFrontFace(Brush.PolygonMode.LINE);
        wireframe.setPolygonBackFace(Brush.PolygonMode.LINE);
        WIRE_FRAME = wireframe.immutable();
        Brush frontShaded = new Brush();
        frontShaded.setCulling(Brush.PolygonSide.BACK);
        frontShaded.setTexturing(false);
        FRONT_SHADED = frontShaded.immutable();
        Brush textured = new Brush();
        textured.setCulling(Brush.PolygonSide.BACK);
        textured.setTexturing(true);
        TEXTURED = textured.immutable();
        Brush points = new Brush();
        points.setLighting(false);
        points.setTexturing(false);
        points.setPolygonFrontFace(Brush.PolygonMode.POINT);
        points.setPolygonBackFace(Brush.PolygonMode.POINT);
        points.setCulling(null);
        POINTS = points.immutable();
    }

    public enum DepthMode {

        LESS(GL_LESS),
        LESS_OR_EQUAL(GL_LEQUAL),
        GREATER(GL_GREATER),
        GREATER_OR_EQUAL(GL_GEQUAL);

        public final int glCode;

        private DepthMode(int glCode) {
            this.glCode = glCode;
        }
    }

    public enum PolygonMode {
        FILL(GL_FILL), LINE(GL_LINE), POINT(GL_POINT);

        public final int glCode;

        private PolygonMode(int glCode) {
            this.glCode = glCode;
        }
    }

    public enum PolygonSide {
        FRONT(GL_FRONT), BACK(GL_BACK);

        public final int glCode;

        private PolygonSide(int glCode) {
            this.glCode = glCode;
        }
    }

    public Brush() {
    }

    public Brush(Brush that) {
        this.lighting = that.lighting;
        this.color = that.color;
        this.texturing = that.texturing;
        this.depth = that.depth;
        this.polygonFrontFace = that.polygonFrontFace;
        this.polygonBackFace = that.polygonBackFace;
        this.culling =  that.culling;
    }

    protected boolean lighting = true;
    protected boolean color = true;
    protected boolean texturing = true;
    protected DepthMode depth = DepthMode.LESS;
    protected PolygonMode polygonFrontFace = PolygonMode.FILL;
    protected PolygonMode polygonBackFace = PolygonMode.FILL;
    protected PolygonSide culling = null;

    public void copyFrom(Brush source) {
        setLighting(source.isLighting());
        setColor(source.isColor());
        setTexturing(source.isTexturing());
        setDepth(source.getDepth());
        setCulling(source.getCulling());
        setPolygonBackFace(source.getPolygonBackFace());
        setPolygonFrontFace(source.getPolygonFrontFace());
    }

    public Brush copy() {
        return new Brush(this);
    }

    public boolean isLighting() {
        return lighting;
    }

    public void setLighting(boolean lighting) {
        this.lighting = lighting;
    }

    public boolean isColor() {
        return color;
    }

    public void setColor(boolean color) {
        this.color = color;
    }

    public boolean isTexturing() {
        return texturing;
    }

    public void setTexturing(boolean textured) {
        this.texturing = textured;
    }

    public DepthMode getDepth() {
        return depth;
    }

    public void setDepth(DepthMode depth) {
        this.depth = depth;
    }

    public PolygonMode getPolygonFrontFace() {
        return polygonFrontFace;
    }

    public void setPolygonFrontFace(PolygonMode polygonFrontFace) {
        this.polygonFrontFace = polygonFrontFace;
    }

    public PolygonMode getPolygonBackFace() {
        return polygonBackFace;
    }

    public void setPolygonBackFace(PolygonMode polygonBackFace) {
        this.polygonBackFace = polygonBackFace;
    }

    public PolygonSide getCulling() {
        return culling;
    }

    public void setCulling(PolygonSide culling) {
        this.culling = culling;
    }

    public Brush immutable() {
        return new Immutable(this);
    }

    private static class Immutable extends Brush {

        private Immutable(Brush that) {
            super.setLighting(that.isLighting());
            super.setColor(that.isColor());
            super.setTexturing(that.isTexturing());
            super.setDepth(that.getDepth());
            super.setCulling(that.getCulling());
            super.setPolygonBackFace(that.getPolygonBackFace());
            super.setPolygonFrontFace(that.getPolygonFrontFace());
        }

        @Override
        public void copyFrom(Brush source) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLighting(boolean lighting) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setColor(boolean color) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setTexturing(boolean textured) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDepth(DepthMode depth) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPolygonFrontFace(PolygonMode polygonFrontFace) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setPolygonBackFace(PolygonMode polygonBackFace) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCulling(PolygonSide culling) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Brush immutable() {
            return this;
        }
    }
}
