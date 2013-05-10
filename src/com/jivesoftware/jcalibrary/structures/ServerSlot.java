package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.AbstractLibraryElement;
import com.jivesoftware.jcalibrary.objects.Objects;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.view.MouseTarget;
import net.venaglia.realms.common.view.MouseTargetEventListener;

/**
 * User: ed
 * Date: 4/23/13
 * Time: 10:45 AM
 */
public class ServerSlot extends AbstractLibraryElement<ServerSlot> {

    private final ServerRack serverRack;
    private final int seq;
    private final Box bounds;
    private final MouseTarget<ServerSlot> mouseTarget;
    private final MouseTargetEventListener<ServerSlot> delegateListener;

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

            public void mouseClick(MouseTarget<? extends ServerSlot> target, MouseButton button) {
                if (delegateListener != null) {
                    delegateListener.mouseClick(target, button);
                }
            }
        }, this);
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

    @Override
    public void project(long nowMS, GeometryBuffer buffer) {
        if (jiveInstance == null) {
            buffer.pushTransform();
            getTransformation().apply(nowMS, buffer);
            if (jiveInstance != null) {
                Objects.SERVER_FRAME.project(nowMS, buffer);
                jiveInstance.project(nowMS, buffer);
            }
            buffer.popTransform();
        } else {
            super.project(nowMS, buffer);
        }
    }

    @Override
    protected void projectImpl(long nowMS, GeometryBuffer buffer) {
        if (jiveInstance != null) {
            jiveInstance.project(nowMS, buffer);
        }
    }
}
