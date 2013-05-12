package com.jivesoftware.jcalibrary.objects;

import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.NodeDetails;
import com.jivesoftware.jcalibrary.structures.ServerSlot;
import net.venaglia.realms.common.physical.bounds.Bounded;
import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.physical.geom.primitives.QuadSequence;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;

import java.util.Map;

/**
 * User: ed
 * Date: 5/11/13
 * Time: 9:16 PM
 */
public class JiveInstancePresentation implements Projectable, Bounded {

    private static final Shape<?> FRAME = new Box().setMaterial(Material.INHERIT);
    private static final Shape<?> FRAME_LOW = new QuadSequence(new Point(-1,-1,-1), new Point(-1,-1,1), new Point(1,-1,1), new Point(1,-1,-1)).scale(0.5).setMaterial(Material.INHERIT);

    private final DetailLevel detailLevel;
    private final BoundingVolume<?> bounds = new BoundingBox(new Point(-0.5,-0.5,-0.5), new Point(0.5,0.5,0.5));

    public JiveInstancePresentation(DetailLevel detailLevel) {
        this.detailLevel = detailLevel;
    }

    @Override
    public BoundingVolume<?> getBounds() {
        return bounds;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    private boolean canShow(DetailLevel detailLevel) {
        return this.detailLevel.compareTo(detailLevel) >= 0;
    }

    @Override
    public void project(long nowMS, GeometryBuffer buffer) {
        ServerSlot serverSlot = ServerSlot.ACTIVE_SERVER_SLOT.get();
        JiveInstance instance = serverSlot == null ? null : serverSlot.getJiveInstance();
        if (instance == null) {
            STATE_EMPTY_SLOT.apply(nowMS, buffer);
            if (canShow(DetailLevel.MEDIUM)) {
                FRAME.project(nowMS, buffer);
            } else {
                FRAME_LOW.project(nowMS, buffer);
            }
        } else {
            Map<String,NodeDetails> nodeDetails = instance.getAllNodeDetails();
            projectFrame(nodeDetails, nowMS, buffer);
            if (canShow(DetailLevel.MEDIUM_LOW)) {
                project(VisualObjects.ICONS.get(instance), nowMS, buffer);
            } else {
                project(VisualObjects.BLANK_ICONS.get(instance), nowMS, buffer);
            }
        }
    }

    private void project(Projectable projectable, long nowMS, GeometryBuffer buffer) {
        if (projectable != null) {
            projectable.project(nowMS, buffer);
        }
    }

    private static final Material STATE_EMPTY_SLOT = Material.makeWireFrame(Color.GRAY_25);
    private static final Material STATE_UNKNOWN = Material.makeWireFrame(Color.GRAY_50);
    private static final Material STATE_OK = Material.makeWireFrame(Color.WHITE);
    private static final Material STATE_OFFLINE = Material.makeWireFrame(new Color(0.5f,0,0,1));
    private static final Material STATE_PARTIAL_DOWN = new ColorCycle(Brush.WIRE_FRAME, Color.ORANGE, 750);

    private void projectFrame(Map<String, NodeDetails> detailsMap, long nowMS, GeometryBuffer buffer) {
        int up = 0, down = 0;
        for (NodeDetails details : detailsMap.values()) {
            String status = details.getStatus();
            if ("up".equals(status)) up++;
            else if ("down".equals(status)) down++;
        }
        Material material;
        if (up == 0 && down == 0) {
            material = STATE_UNKNOWN;
        } else if (up == 0) {
            material = STATE_OFFLINE;
        } else if (down == 0) {
            material = STATE_OK;
        } else {
            material = STATE_PARTIAL_DOWN;
        }
        material.apply(nowMS, buffer);
        if (canShow(DetailLevel.MEDIUM_HIGH)) {
            FRAME.project(nowMS, buffer);
        } else {
            FRAME_LOW.project(nowMS, buffer);
        }
    }

    private static class ColorCycle extends Material {

        private final Brush brush;
        private final int pulseDurationMS;
        private final Color[] colorCycle;

        private ColorCycle(Brush brush, Color baseColor, int pulseDurationMS) {
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
}
