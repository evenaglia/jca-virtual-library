package net.venaglia.gloo.physical.texture;

import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.Projectable;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 12:56 AM
 */
public class TextureCoordinate implements Projectable {

    public static final TextureCoordinate UNDEFINED = new TextureCoordinate(Float.NaN, Float.NaN) {
        @Override
        public String toString() {
            return "TexCoord(?,?)";
        }
    };

    public static final XForm.View<TextureCoordinate> TEXTURE_COORDINATE_XFORM_VIEW = new XForm.View<TextureCoordinate>() {
        public TextureCoordinate convert(double x, double y, double z, double w) {
            return new TextureCoordinate((float)x, (float)y);
        }
    };

    public final float s;
    public final float t;

    public TextureCoordinate(float s, float t) {
        this.s = s;
        this.t = t;
    }

    public float getS() {
        return s;
    }

    public float getT() {
        return t;
    }

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TextureCoordinate that = (TextureCoordinate)o;

        if (Float.compare(that.s, s) != 0) return false;
        if (Float.compare(that.t, t) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (s != +0.0f ? Float.floatToIntBits(s) : 0);
        result = 31 * result + (t != +0.0f ? Float.floatToIntBits(t) : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("x[%.3f,%.3f]", s, t);
    }
}
