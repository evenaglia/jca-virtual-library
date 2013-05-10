package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.AbstractLibraryElement;
import com.jivesoftware.jcalibrary.objects.Objects;
import com.jivesoftware.jcalibrary.objects.ServerRackSource;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;
import net.venaglia.realms.common.view.MouseTarget;
import net.venaglia.realms.common.view.MouseTargetEventListener;
import net.venaglia.realms.common.view.MouseTargets;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 4:59 PM
 */
public class ServerRack extends AbstractLibraryElement<ServerRack> {

    protected final ServerSlot[] slots;
    protected final MouseTarget<ServerRack> mouseTarget;
    protected final int seq;

    protected JiveInstance.Grouping grouping;
    protected Material material;

    public ServerRack(Projectable target, MouseTargetEventListener<ServerSlot> eventListener, int seq) {
        grouping = JiveInstance.Grouping.byRack(seq);
        material = Material.makeSelfIlluminating(grouping.color);
        int slotsPerShelf = (int)(ServerRackSource.WIDTH / 1.1111f);
        double slotWidth = ServerRackSource.WIDTH / slotsPerShelf;
        this.slots = new ServerSlot[ServerRackSource.SHELVES * slotsPerShelf];
        this.mouseTarget = new MouseTarget<ServerRack>(target, null, this);
        MouseTargets children = new MouseTargets();
        this.mouseTarget.setChildren(children);
        this.seq = seq;
        Box baseBox  = new Box().scale(0.8888).translate(Vector.Z.scale(0.57291625)).translate(Vector.Y.scale(-0.83333));
        int k = 0;
        for (int i = 0; i < ServerRackSource.SHELVES; i++) {
            double z = i * (ServerRackSource.HEIGHT - ServerRackSource.LOWEST_SHELF) / (ServerRackSource.SHELVES - 1) + ServerRackSource.LOWEST_SHELF;
            for (int j = 0; j < slotsPerShelf; j++) {
                double x = ServerRackSource.WIDTH * -0.5 + slotWidth * (j + 0.5);
                Vector xlate = new Vector(x, 0, z);
                Box mouseTarget = baseBox.translate(xlate).translate(Vector.Y.scale(66.6666)).rotate(Axis.Z, Math.PI * 2.0 * seq / 40.0).scale(0.3);
                mouseTarget.setMaterial(Material.INHERIT);
//                mouseTarget.getTransformation().transform(getTransformation());
                ServerSlot serverSlot = new ServerSlot(this, mouseTarget, eventListener, k);
//                serverSlot.getTransformation().transform(getTransformation());
                serverSlot.getTransformation().translate(xlate.sum(Vector.Z.scale(0.625)));
//                serverSlot.getTransformation().translate(xlate);
                slots[k++] = serverSlot;
                children.add(new MouseTarget<ServerSlot>(mouseTarget, eventListener, serverSlot));
            }
        }
    }

    public ServerSlot getFirstAvailableSlot() {
        for (ServerSlot serverSlot : slots) {
            if (serverSlot.getJiveInstance() == null) {
                return serverSlot;
            }
        }
        return null;
    }

    public int getSeq() {
        return seq;
    }

    @Override
    protected void projectImpl(long nowMS, GeometryBuffer buffer) {
        buffer.pushBrush();
        material.apply(nowMS, buffer);
        Objects.SERVER_RACK.project(nowMS, buffer);
        for (ServerSlot slot : slots) {
            slot.project(nowMS, buffer);
        }
        buffer.popBrush();
    }

    @Override
    public MouseTarget<ServerRack> getMouseTarget() {
        return mouseTarget;
    }
}
