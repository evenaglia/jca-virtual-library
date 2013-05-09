package net.venaglia.realms.common.physical.texture.mapping;

import static java.lang.Math.PI;
import static java.lang.Math.floor;

import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.texture.TextureCoordinate;
import net.venaglia.realms.common.physical.texture.TextureMapping;

/**
 * User: ed
 * Date: 3/8/13
 * Time: 5:37 PM
 */
public class SphericalMapping implements TextureMapping {

    private static final double PI_NEGATIVE = 0.0 - PI;
    private static final double HALF_PI = PI * 0.5;
    private static final double HALF_PI_NEGATIVE = 0.0 - HALF_PI;
    private static final double TWO_PI = PI * 2.0;

    private float lastS;
    private int baseS;
    private boolean fresh = true;

    public void newSequence() {
        fresh = true;
        lastS = 0.5f;
    }

    public TextureCoordinate unwrap(Point p) {
        float[] st = {0,0};
        unwrap(p.x, p.y, p.z, st);
        return new TextureCoordinate(st[0], st[1]);
    }

    private float applyHint(float s) {
        float newS;
        if (fresh) {
            fresh = false;
            newS = s;
        } else {
            float diffS = s - this.lastS;
            if (diffS < -0.5f) {
                this.baseS++;
            } else if (diffS > 0.5f) {
                this.baseS--;
            }
            newS = s + this.baseS;
        }
        this.lastS = s;
        this.baseS = (int)(floor(newS));
        return newS;
    }

    public void unwrap(double x, double y, double z, float[] out) {
        if (x == 0 && y == 0) {
            out[0] = 0;
            if (z > 0) {
                out[1] = 1;
            } else {
                out[1] = 0;
            }
            return;
        }
        @SuppressWarnings("SuspiciousNameCombination")
        double longitude = Math.atan2(x, y);
        if (longitude >= Math.PI) longitude -= 2.0 * Math.PI;
        if (longitude < PI_NEGATIVE) longitude = PI_NEGATIVE;
        double latitude = Math.atan2(z, Math.sqrt(x * x + y * y));
        if (latitude > HALF_PI) latitude -= Math.PI;
        if (latitude < PI_NEGATIVE) latitude = HALF_PI_NEGATIVE;
        out[1] = (float)(1.0 - (latitude + HALF_PI) / PI);
        out[0] = applyHint((float)((longitude + PI) / TWO_PI));
    }

    @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
    public SphericalMapping clone() {
        return new SphericalMapping();
    }
}
