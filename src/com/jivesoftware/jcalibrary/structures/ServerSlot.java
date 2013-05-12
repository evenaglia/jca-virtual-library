package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.AbstractLibraryElement;
import com.jivesoftware.jcalibrary.objects.Objects;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.detail.DetailComputer;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.util.Ref;
import net.venaglia.realms.common.view.MouseTarget;
import net.venaglia.realms.common.view.MouseTargetEventListener;

/**
 * User: ed
 * Date: 4/23/13
 * Time: 10:45 AM
 */
public class ServerSlot extends AbstractLibraryElement<ServerSlot> {

    public static final Ref<DetailComputer> DETAIL_COMPUTER_REF = new Ref<DetailComputer>() {
        @Override
        public DetailComputer get() {
            ServerSlot slot = ACTIVE_SERVER_SLOT.get();
            return slot == null ? null : slot.slotTransformation;
        }
    };
    public static final ThreadLocal<ServerSlot> ACTIVE_SERVER_SLOT = new ThreadLocal<ServerSlot>();

    private final ServerRack serverRack;
    private final int seq;
    private final Box bounds;
    private final MouseTarget<ServerSlot> mouseTarget;
    private final MouseTargetEventListener<ServerSlot> delegateListener;
    private final SlotTransformation slotTransformation;

    private boolean hover;
    private JiveInstance jiveInstance;

    public ServerSlot(ServerRack serverRack, Box bounds, MouseTargetEventListener<ServerSlot> listener, int seq) {
        this.serverRack = serverRack;
        this.seq = seq;
        this.transformation = new Transformation();
        this.bounds = bounds;
        this.delegateListener = listener;
        this.mouseTarget = new MouseTarget<ServerSlot>(bounds, new MouseTargetEventListener<ServerSlot>() {
            public void mouseOver(MouseTarget<? extends ServerSlot> target) {
                hover = true;
                if (delegateListener != null) {
                    delegateListener.mouseOver(target);
                }
            }

            public void mouseOut(MouseTarget<? extends ServerSlot> target) {
                hover = false;
                if (delegateListener != null) {
                    delegateListener.mouseOut(target);
                }
            }

            @Override
            public void mouseDown(MouseTarget<? extends ServerSlot> target, MouseButton button) {
                if (delegateListener != null) {
                    delegateListener.mouseDown(target, button);
                }
            }

            @Override
            public void mouseUp(MouseTarget<? extends ServerSlot> target, MouseButton button) {
                if (delegateListener != null) {
                    delegateListener.mouseUp(target, button);
                }
            }
        }, this);
        slotTransformation = new SlotTransformation(this) {
            @Override
            public DetailLevel computeDetail(Point observer, double longestDimension) {
                DetailLevel detailLevel = super.computeDetail(observer, longestDimension);
                if (detailLevel != null && jiveInstance != null && jiveInstance.isSelected()) {
                    return detailLevel.more(2);
                }
                return detailLevel;
            }
        };
    }

    public ServerRack getServerRack() {
        return serverRack;
    }

    public int getSeq() {
        return seq;
    }

    public Box getBounds() {
        return bounds;
    }

    public MouseTarget<ServerSlot> getMouseTarget() {
        return mouseTarget;
    }

    public JiveInstance getJiveInstance() {
        return jiveInstance;
    }

    public void setJiveInstance(JiveInstance jiveInstance) {
        this.jiveInstance = jiveInstance;
    }

    public boolean isHover() {
        return hover;
    }

    public SlotTransformation getSlotTransformation() {
        return slotTransformation;
    }

    @Override
    protected void projectImpl(long nowMS, GeometryBuffer buffer) {
        ACTIVE_SERVER_SLOT.set(this);
        try {
            buffer.pushBrush();
            buffer.pushTransform();
            buffer.identity();
            slotTransformation.apply(nowMS, buffer);
            if (jiveInstance != null) {
                jiveInstance.project(nowMS, buffer);
            } else {
                Objects.JIVE_INSTANCE.project(nowMS, buffer);
            }
            buffer.popBrush();
            buffer.popTransform();
        } finally {
            ACTIVE_SERVER_SLOT.remove();
        }
    }
}
