package net.venaglia.gloo.physical.lights;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static net.venaglia.gloo.util.CallLogger.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * User: ed
 * Date: 9/27/12
 * Time: 10:14 AM
 */
public abstract class AbstractLight implements Light {

    private static final AtomicInteger SEQ = new AtomicInteger();

    public final Integer id = SEQ.getAndIncrement();

    public Integer getId() {
        return id;
    }

    public boolean isStatic() {
        return true;
    }

    public boolean isDirectional() {
        return false;
    }

    public boolean isFinite() {
        return false;
    }

    public Color getAmbient() {
        return Color.BLACK;
    }

    public Color getDiffuse() {
        return getSimpleColor();
    }

    public Color getSpecular() {
        return Color.WHITE;
    }

    public BoundingVolume<?> getBounds() {
        return BoundingBox.INFINITE;
    }

    protected Color getSimpleColor() {
        return Color.WHITE;
    }

    public void updateGL(int glLightId) {
        Color ambient = getAmbient();
        Color diffuse = getDiffuse();
        Color specular = getSpecular();
        if (ambient == diffuse) {
            glLight(glLightId, GL_AMBIENT_AND_DIFFUSE, asFloatBuffer(ambient));
            if (logCalls) logCall("glLight", glLightId, GL_AMBIENT_AND_DIFFUSE, asFloatBuffer(ambient));
            glLight(glLightId, GL_SPECULAR, asFloatBuffer(specular));
            if (logCalls) logCall("glLight", glLightId, GL_SPECULAR, asFloatBuffer(specular));
        } else {
            glLight(glLightId, GL_AMBIENT, asFloatBuffer(ambient));
            if (logCalls) logCall("glLight", glLightId, GL_SPECULAR, asFloatBuffer(specular));
            glLight(glLightId, GL_DIFFUSE, asFloatBuffer(diffuse));
            if (logCalls) logCall("glLight", glLightId, GL_DIFFUSE, asFloatBuffer(diffuse));
            glLight(glLightId, GL_SPECULAR, asFloatBuffer(specular));
            if (logCalls) logCall("glLight", glLightId, GL_SPECULAR, asFloatBuffer(specular));
        }
        glLight(glLightId, GL_POSITION, asFloatBuffer(getSource()));
        if (logCalls) logCall("glLight", glLightId, GL_POSITION, asFloatBuffer(getSource()));
        // todo: directional lights
        // todo: finite lights
    }

    protected FloatBuffer asFloatBuffer(Point point) {
        float[] values = { (float)point.x, (float)point.y, (float)point.z, 1.0f };
        return (FloatBuffer)BufferUtils.createFloatBuffer(4).put(values).flip();
    }

    protected FloatBuffer asFloatBuffer(Color color) {
        float[] values = { color.r, color.g, color.b, 1.0f };
        return (FloatBuffer)BufferUtils.createFloatBuffer(4).put(values).flip();
    }
}
