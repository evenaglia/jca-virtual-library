package net.venaglia.gloo.projection;

import net.venaglia.gloo.physical.decorators.Color;

/**
 * User: ed
 * Date: 1/12/15
 * Time: 8:36 AM
 */
public interface ColorBuffer {

    void color(Color color);

    void color(float r, float g, float b);

    void colorAndAlpha(Color color);

    void colorAndAlpha(float r, float g, float b, float a);
}
