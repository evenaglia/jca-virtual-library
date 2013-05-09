package net.venaglia.realms.common.projection;

import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.shaders.ShaderProgram;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 9:27 PM
 */
public interface ProjectionBuffer extends GeometryBuffer {

    public enum Mode {
        NORMAL, CAMERA_SELECT, OVERLAY_2D
    }

    Mode getMode();

    void useCamera(Camera camera);

    Point getCameraViewPoint();

    float getCameraFOV();

    GeometryBuffer beginCameraSelect(Camera camera, float x, float y);

    void loadName(int glName);

    void endCameraSelect(SelectObserver selectObserver);

    GeometryBuffer beginOverlay();

    void endOverlay();

    void useLights(Light[] lights);

    void resetAllStacks();

    void useShader(ShaderProgram shaderProgram);

    void pushShader();

    void popShader();
}
