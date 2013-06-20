package net.venaglia.gloo.physical.lights;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;

/**
 * User: ed
 * Date: 9/6/12
 * Time: 9:50 PM
 */
public interface Light {

    Light DISABLED_LIGHT = new AbstractLight() {
        public Point getSource() {
            return Point.ORIGIN;
        }

        @Override
        public void updateGL(int glLightId) {
            // no-op
        }

        @Override
        protected Color getSimpleColor() {
            return Color.BLACK;
        }
    };

    Integer getId();

    boolean isStatic();

    boolean isDirectional();

    boolean isFinite();

    Point getSource();

    Color getAmbient();

    Color getDiffuse();

    Color getSpecular();

    BoundingVolume<?> getBounds();

    void updateGL(int glLightId);
}
