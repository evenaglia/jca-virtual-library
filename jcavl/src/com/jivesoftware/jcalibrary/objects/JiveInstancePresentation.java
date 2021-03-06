package com.jivesoftware.jcalibrary.objects;

import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.JiveInstanceState;
import com.jivesoftware.jcalibrary.structures.NodeDetails;
import com.jivesoftware.jcalibrary.structures.ServerSlot;
import net.venaglia.gloo.physical.bounds.Bounded;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.decorators.Transformation;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.primitives.Box;
import net.venaglia.gloo.physical.geom.primitives.QuadSequence;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.Projectable;

import java.util.Iterator;
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
            JiveInstanceState.EMPTY_SLOT.getWireframe().apply(nowMS, buffer);
            if (canShow(DetailLevel.MEDIUM)) {
                FRAME.project(nowMS, buffer);
            } else {
                FRAME_LOW.project(nowMS, buffer);
            }
        } else {
            projectFrame(instance, nowMS, buffer);
            if (canShow(DetailLevel.MEDIUM_LOW)) {
                project(VisualObjects.ICONS.get(instance), nowMS, buffer);
            } else {
                project(VisualObjects.BLANK_ICONS.get(instance), nowMS, buffer);
            }
            if (canShow(DetailLevel.MEDIUM)) {
                projectNodeDetails(instance, nowMS,  buffer);
            }
        }
    }

    private void project(Projectable projectable, long nowMS, GeometryBuffer buffer) {
        if (projectable != null) {
            projectable.project(nowMS, buffer);
        }
    }

    private void projectFrame(JiveInstance jiveInstance, long nowMS, GeometryBuffer buffer) {
        JiveInstanceState state = VisualObjects.OVERALL_STATE.get(jiveInstance);
//        if (jiveInstance.getCustomerInstallationId() == 9437)  {
//            "".toString();
//        }
        Material material = state.getWireframe();
        material.apply(nowMS, buffer);
        if (canShow(DetailLevel.MEDIUM_HIGH)) {
            FRAME.project(nowMS, buffer);
        } else {
            FRAME_LOW.project(nowMS, buffer);
        }
    }

    private void projectNodeDetails(JiveInstance instance, long nowMS, GeometryBuffer buffer) {
        Map<String,NodeDetails> allNodeDetails = instance.getAllNodeDetails();
        Iterator<NodeDetails> detailsIterator = allNodeDetails.values().iterator();
        for (Transformation xform : TRANSFORMATIONS[allNodeDetails.size()]) {
            detailsIterator.next().project(nowMS, buffer, detailLevel.less(2), xform);
        }
    }

    private static final Transformation[][] TRANSFORMATIONS = {
            { },                          //  0
            buildXForms(0,0,1,0,0),       //  1
            buildXForms(0.25,0,2,0,0),    //  2
            buildXForms(0.25,0,3,0,0),    //  3
            buildXForms(0.25,0.25,0,2,2), //  4
            buildXForms(0.3,0.35,1,2,2),  //  5
            buildXForms(0.3,0.35,0,3,3),  //  6
            buildXForms(0.3,0.35,1,3,3),  //  7
            buildXForms(0.3,0.35,2,3,3),  //  8
            buildXForms(0.3,0.35,3,3,3),  //  9
            buildXForms(0.3,0.35,3,4,3),  // 10
            buildXForms(0.3,0.35,4,3,4),  // 11
            buildXForms(0.3,0.35,4,4,4),  // 12
            buildXForms(0.32,0.35,5,4,4), // 13
            buildXForms(0.32,0.35,4,5,5), // 14
            buildXForms(0.32,0.35,5,5,5), // 15
    };

    private static Transformation[] buildXForms(double radius, double z, int n, int m, int o) {
        Transformation[] result = new Transformation[n + m + o];
        double a1 = Math.PI * (n == 0 ? 1.0 : 0.66667);
        double a2 = Math.PI * (n == 0 ? 0.0 : -0.66667);
        for (int i = 0; i < n; i++) {
            Transformation xForm = new Transformation();
            xForm.scale(0.5 - radius);
            if (radius > 0) {
                xForm.translate(new Vector(0,-radius,0));
            }
            if (i != 0) {
                xForm.rotate(new Vector(-3, -1, 10).normalize(), Math.PI * 2 * i / n);
            }
            result[i] = xForm;
        }
        for (int i = 0; i < m; i++) {
            Transformation xForm = new Transformation();
            xForm.scale(0.5 - radius);
            if (radius > 0 || z != 0) {
                xForm.translate(new Vector(0,-radius,z));
            }
            if (i != 0) {
                xForm.rotate(new Vector(-3, -1, 10).normalize(), a1 + Math.PI * 2 * i / m);
            }
            result[i + n] = xForm;
        }
        for (int i = 0; i < o; i++) {
            Transformation xForm = new Transformation();
            xForm.scale(0.5 - radius);
            if (radius > 0 || z != 0) {
                xForm.translate(new Vector(0,-radius,-z));
            }
            if (i != 0) {
                xForm.rotate(new Vector(-3, -1, 10).normalize(), a2 + Math.PI * 2 * i / o);
            }
            result[i + n + m] = xForm;
        }
        return result;
    }

}
