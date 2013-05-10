package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.ServerRackSource;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.projection.Decorator;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 5/10/13
 * Time: 11:51 AM
 */
public class SlotTransformation implements Decorator {

    private final double homeX;
    private final double homeY;
    private final double homeZ;
    private final double homeRadius;
    private final double homeAngle;
    private final Vector homeAngleVector;
    private final Vector homeZVector;

    private long lastMS = -1;
    private double currentScale = 1;
    private double targetScale = 1;
    private double currentTelescope = 0;
    private double targetTelescope = 0;

    public SlotTransformation(ServerSlot slot) {
        int rackNum = slot.getServerRack().getSeq();
        double rackAngle = Math.PI * 2.0 * rackNum / 40.0;
        double sin = Math.sin(rackAngle);
        double cos = Math.cos(rackAngle);

        int slotsPerShelf = (int)(ServerRackSource.WIDTH / 1.1111f);
        double slotWidth = ServerRackSource.WIDTH / slotsPerShelf;
        double slotX = ServerRackSource.WIDTH * -0.5 + slotWidth * ((slot.getSeq() % slotsPerShelf) + 0.5);
        double slotY = 19.625;
        double slotZ = (slot.getSeq() % slotsPerShelf) * (ServerRackSource.HEIGHT - ServerRackSource.LOWEST_SHELF) / (ServerRackSource.SHELVES - 1) + ServerRackSource.LOWEST_SHELF;

        homeX = cos * slotX + sin * slotY;
        homeY = cos * slotY - sin * slotX;
        homeZ = slotZ;
        homeRadius = Math.sqrt(homeX * homeX + homeY * homeY);
        homeAngle = Math.atan2(homeY, homeX);
        homeAngleVector = new Vector(homeX / homeRadius, homeY / homeRadius, 0);
        homeZVector = Vector.Z.scale(homeZ);
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public void apply(long nowMS, GeometryBuffer buffer) {
        currentScale = animate(nowMS, currentScale, targetScale, 0.25);
        currentTelescope = animate(nowMS, currentTelescope, targetTelescope, 4.0);
        buffer.rotate(Axis.Z, homeAngle);
        buffer.translate(homeZVector);
        buffer.scale(currentScale);
        buffer.translate(homeAngleVector.scale(homeRadius - currentTelescope));
        lastMS = nowMS;
    }

    private double animate(long nowMS, double current, double target, double perSecond) {
        double delta = Math.abs(target - current);
        if (delta == 0) {
            return target;
        }
        double move = (nowMS - lastMS) / (perSecond * 1000.0);
        if (move > delta) {
            return target;
        }
        double sign = target < current ? -1 : 1;
        return current + move * sign;
    }
}