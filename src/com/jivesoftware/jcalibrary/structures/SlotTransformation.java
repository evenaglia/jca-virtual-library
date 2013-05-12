package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.ServerRackSource;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.projection.Decorator;
import net.venaglia.realms.common.physical.geom.detail.DetailComputer;
import net.venaglia.realms.common.projection.GeometryBuffer;

import java.util.Map;

/**
 * User: ed
 * Date: 5/10/13
 * Time: 11:51 AM
 */
public class SlotTransformation implements Decorator, DetailComputer {

    private final double homeX;
    private final double homeY;
    private final double homeZ;
    private final double homeRadius;
    private final double homeAngle;
    private final Vector homeAngleVector;
    private final Vector homeZVector;
    private final int rackSeq;
    private final int slotSeq;

    private long lastMS = -1;
    private double currentScale = 1;
    private double targetScale = 1;
    private double currentTelescope = 0;
    private double targetTelescope = 0;

    public SlotTransformation(ServerSlot slot) {
        int rackNum = slot.getServerRack().getSeq();
        double rackAngle = Math.PI * -2.0 * rackNum / 40.0;

        int slotsPerShelf = (int)(ServerRackSource.WIDTH / 1.1111f);
        double slotWidth = ServerRackSource.WIDTH * 0.3 / slotsPerShelf;
        int shelfSeq = slot.getSeq() % slotsPerShelf;
        int shelfNum = slot.getSeq() / slotsPerShelf;
        float shelfHeight = (ServerRackSource.HEIGHT - ServerRackSource.LOWEST_SHELF) / (ServerRackSource.SHELVES - 1);
        double slotX = 19.625;
        double slotY = ServerRackSource.WIDTH * -0.15 + slotWidth * (shelfSeq + 0.5);
        double slotZ = shelfNum * shelfHeight * 0.3 + shelfHeight * 0.15 + ServerRackSource.LOWEST_SHELF * 0.3;

        homeX = slotX;
        homeY = slotY;
//        homeX = cos * slotX + sin * slotY;
//        homeY = cos * slotY - sin * slotX;
        homeZ = slotZ;
        homeRadius = Math.sqrt(homeX * homeX + homeY * homeY);
        homeAngle = Math.atan2(homeY, homeX) + rackAngle;
        homeAngleVector = new Vector(Math.sin(homeAngle), Math.cos(homeAngle), 0);
        homeZVector = Vector.Z.scale(homeZ);

        this.rackSeq = rackNum;
        this.slotSeq = slot.getSeq();
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public void apply(long nowMS, GeometryBuffer buffer) {
        currentScale = animate(nowMS, currentScale, targetScale, 0.25);
        currentTelescope = animate(nowMS, currentTelescope, targetTelescope, 4.0);
        buffer.translate(homeAngleVector.scale(homeRadius - currentTelescope));
        buffer.rotate(Axis.Z, -homeAngle);
        buffer.translate(homeZVector);
        buffer.scale(currentScale * 0.3);
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

    public void setTargetScale(double targetScale) {
        this.targetScale = targetScale;
    }

    public void setTargetTelescope(double targetTelescope) {
        this.targetTelescope = targetTelescope;
    }

    public DetailLevel computeDetail(Point observer, double longestDimension) {
        if (longestDimension <= 0) {
            return null; // do not render
        }
        double telescope = (homeRadius - currentTelescope);
        double scale = currentScale * 0.3;
        double dimension = longestDimension * 0.5;
        double x = homeAngleVector.i * telescope;
        double y = homeAngleVector.j * telescope;
        double z = homeAngleVector.k * telescope + homeZVector.k;
        double distance = Vector.computeDistance(observer.x - x, observer.y - y, observer.z - z);
        double angle = dimension / distance;
        Map.Entry<Double,DetailLevel> entry = DETAIL_LEVELS_BY_VISIBLE_ANGLE.ceilingEntry(angle);
        return entry == null ? null : entry.getValue();
    }
}
