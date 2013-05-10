package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.detail.DynamicDetail;
import net.venaglia.realms.common.physical.geom.detail.DynamicDetailSource;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;
import net.venaglia.realms.demo.DemoObjects;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 4/24/13
 * Time: 9:08 PM
 */
public enum Objects implements Projectable {

    SERVER_RACK(ServerRackSource.class),
//    BOX_CURSOR_SMALL(new BoxCursor(4,4,4,DetailLevel.MEDIUM).scale(0.0833)),
//    BOX_CURSOR_SMALL(new BoxCursor(0.333,0.333,0.333,0.125,0.025,0.25,DetailLevel.MEDIUM)),
//    BOX_CURSOR_SMALL(new BoxCursor(0.333,0.333,0.333,0.0625,0.025/4,0.25,DetailLevel.MEDIUM)),
    BOX_CURSOR_SMALL(new BoxCursor(1,1,1, 0.125, 0.0125, 0.1875,DetailLevel.MEDIUM)),
    EMPTY_SERVER_FRAME(new Box().setMaterial(Material.makeWireFrame(new Color(0.1f, 0.1f, 0.1f)))),
    SERVER_FRAME(new Box().setMaterial(Material.makeWireFrame(Color.GRAY_25))),
    HEART(DemoObjects.ObjectCategory.INTERESTING_SHAPES.getDynamicDetailSource(1)),
    SCROLL(DemoObjects.ObjectCategory.INTERESTING_SHAPES.getDynamicDetailSource(2)),
    STAR(DemoObjects.ObjectCategory.EXTRUDED_SHAPES.getDynamicDetailSource(0)),
    COG(DemoObjects.ObjectCategory.EXTRUDED_SHAPES.getDynamicDetailSource(1)),
    CRESCENT(DemoObjects.ObjectCategory.EXTRUDED_SHAPES.getDynamicDetailSource(2)),
    CYLINDER(DemoObjects.ObjectCategory.ROUND_SOLIDS.getDynamicDetailSource(0)),
    TORUS(DemoObjects.ObjectCategory.ROUND_SOLIDS.getDynamicDetailSource(1)),
    SPHERE(DemoObjects.ObjectCategory.ROUND_SOLIDS.getDynamicDetailSource(2)),
    TERTAHEDRON(DemoObjects.ObjectCategory.PLATONIC_SOLIDS.getDynamicDetailSource(0)),
    CUBE(DemoObjects.ObjectCategory.PLATONIC_SOLIDS.getDynamicDetailSource(1)),
    ICOSAHEDRON(DemoObjects.ObjectCategory.PLATONIC_SOLIDS.getDynamicDetailSource(2)),

    DEMO_OBJECT_0(DemoObjects.ObjectCategory.INTERESTING_SHAPES.getDynamicDetailSource(0)),
    DEMO_OBJECT_1(DemoObjects.ObjectCategory.INTERESTING_SHAPES.getDynamicDetailSource(1)),
    DEMO_OBJECT_2(DemoObjects.ObjectCategory.INTERESTING_SHAPES.getDynamicDetailSource(2)),
    DEMO_OBJECT_3(DemoObjects.ObjectCategory.ROUND_SOLIDS.getDynamicDetailSource(0)),
    DEMO_OBJECT_4(DemoObjects.ObjectCategory.ROUND_SOLIDS.getDynamicDetailSource(1)),
    DEMO_OBJECT_5(DemoObjects.ObjectCategory.ROUND_SOLIDS.getDynamicDetailSource(2)),
    DEMO_OBJECT_6(DemoObjects.ObjectCategory.CURVED_SURFACES.getDynamicDetailSource(0)),
    DEMO_OBJECT_7(DemoObjects.ObjectCategory.CURVED_SURFACES.getDynamicDetailSource(1)),
    DEMO_OBJECT_8(DemoObjects.ObjectCategory.CURVED_SURFACES.getDynamicDetailSource(2)),
    DEMO_OBJECT_9(DemoObjects.ObjectCategory.EXTRUDED_SHAPES.getDynamicDetailSource(0)),
    DEMO_OBJECT_A(DemoObjects.ObjectCategory.EXTRUDED_SHAPES.getDynamicDetailSource(1)),
    DEMO_OBJECT_B(DemoObjects.ObjectCategory.EXTRUDED_SHAPES.getDynamicDetailSource(2)),
    DEMO_OBJECT_C(DemoObjects.ObjectCategory.PLATONIC_SOLIDS.getDynamicDetailSource(0)),
    DEMO_OBJECT_D(DemoObjects.ObjectCategory.PLATONIC_SOLIDS.getDynamicDetailSource(1)),
    DEMO_OBJECT_E(DemoObjects.ObjectCategory.PLATONIC_SOLIDS.getDynamicDetailSource(2));

    private final AtomicReference<DynamicDetail<?>> detail = new AtomicReference<DynamicDetail<?>>();
    private final Class<? extends DynamicDetailSource<?>> sourceClass;

    private DynamicDetailSource<?> source;

    private Objects(Class<? extends DynamicDetailSource<?>> sourceClass) {
        this.sourceClass = sourceClass;
    }

    private Objects(DynamicDetailSource<?> source) {
        this.sourceClass = null;
        this.source = source;
    }

    private Objects(final Shape<?> source) {
        this(new DynamicDetailSource<Shape<?>>() {
            public float getSizeFactor() {
                return 6;
            }

            public Shape<?> produceAt(DetailLevel detailLevel) {
                return source;
            }

            public Shape<?> getTarget() {
                return null; // compute default target from shape
            }
        });
    }

    public DynamicDetail<?> getDynamicDetail() {
        DynamicDetail<?> detail = this.detail.get();
        if (detail == null) {
            //noinspection unchecked
            detail = new DynamicDetail(getSource(), DetailLevel.LOW);
            this.detail.set(detail);
        }
        return detail;
    }

    private DynamicDetailSource<?> getSource() {
        try {
            if (source == null) {
                source = sourceClass.newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return source;
    }

    public boolean isStatic() {
        return false;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        DynamicDetail<?> detail = getDynamicDetail();
        if (detail != null) {
            detail.project(nowMS, buffer);
        }
    }

    public Shape<?> getTarget() {
        DynamicDetailSource<?> source = getSource();
        Shape<?> target = source.getTarget();
        if (target == null) {
            DynamicDetail<?> detail = getDynamicDetail();
            target = detail == null ? null : detail.getBounds().asShape(0.95f);
        }
        return target;
    }
}
