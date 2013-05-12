package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.ColorCycle;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;

/**
 * User: ed
 * Date: 5/12/13
 * Time: 12:59 PM
 */
public enum JiveInstanceState {

    EMPTY_SLOT(Color.GRAY_25, 0),
    UNKNOWN(Color.GRAY_50, 0),
    OK(Color.WHITE, 0),
    OFFLINE(new Color(0.5f,0,0,1), 0),
    PARTIAL_DOWN(Color.ORANGE, 750);

    private final Color color;
    private final Material wireframe;

    JiveInstanceState(Color color, int cycleMS) {
        this.color = color;
        this.wireframe = cycleMS > 0
                         ? new ColorCycle(Brush.WIRE_FRAME, color, cycleMS)
                         : Material.makeWireFrame(color);
    }

    public Color getColor(long nowMS) {
        return wireframe instanceof ColorCycle ? ((ColorCycle)wireframe).getColor(nowMS) : color;
    }

    public Material getWireframe() {
        return wireframe;
    }
}
