package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
* User: ed
* Date: 5/12/13
* Time: 10:18 AM
*/
public class ColorCycle extends Material {

    private final Brush brush;
    private final int pulseDurationMS;
    private final Color[] colorCycle;

    public ColorCycle(Brush brush, Color baseColor, int pulseDurationMS) {
        this.brush = brush;
        this.pulseDurationMS = pulseDurationMS;
        this.colorCycle = new Color[Math.round(pulseDurationMS / 8.0f + 1)]; // 125fps max
        for (int i = 0, l = colorCycle.length; i < l; i++) {
            float p = (float)Math.sin(Math.PI * i / l);
            this.colorCycle[i] = new Color(baseColor.r * p, baseColor.g * p, baseColor.b * p, 1.0f);
        }
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public void apply(long nowMS, GeometryBuffer buffer) {
        buffer.applyBrush(brush);
        int now = (int)(nowMS % pulseDurationMS);
        buffer.color(colorCycle[Math.round(now / 8.0f)]);
    }
}
