package net.venaglia.realms.common.physical.lights;

import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Point;

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
