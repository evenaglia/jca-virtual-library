package com.jivesoftware.jcalibrary.objects;

import com.jivesoftware.jcalibrary.structures.JiveInstance;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.complex.Origin;
import net.venaglia.realms.common.physical.geom.detail.AbstractDynamicDetailSource;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.detail.DynamicDetail;
import net.venaglia.realms.common.physical.geom.detail.DynamicDetailSource;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.physical.geom.primitives.Cog;
import net.venaglia.realms.common.physical.geom.primitives.Crescent;
import net.venaglia.realms.common.physical.geom.primitives.Cylinder;
import net.venaglia.realms.common.physical.geom.primitives.Heart;
import net.venaglia.realms.common.physical.geom.primitives.Icosahedron;
import net.venaglia.realms.common.physical.geom.primitives.Scroll;
import net.venaglia.realms.common.physical.geom.primitives.Sphere;
import net.venaglia.realms.common.physical.geom.primitives.Star;
import net.venaglia.realms.common.physical.geom.primitives.Tetrahedron;
import net.venaglia.realms.common.physical.geom.primitives.Torus;
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

    ORIGIN(new Origin(0.25)),
    SERVER_RACK(ServerRackSource.class),
//    BOX_CURSOR_SMALL(new BoxCursor(4,4,4,DetailLevel.MEDIUM).scale(0.0833)),
//    BOX_CURSOR_SMALL(new BoxCursor(0.333,0.333,0.333,0.125,0.025,0.25,DetailLevel.MEDIUM)),
//    BOX_CURSOR_SMALL(new BoxCursor(0.333,0.333,0.333,0.0625,0.025/4,0.25,DetailLevel.MEDIUM)),
    BOX_CURSOR_SMALL(new BoxCursor(1,1,1, 0.125, 0.0125, 0.1875,DetailLevel.MEDIUM)),
//    EMPTY_SERVER_FRAME(new Box().setMaterial(Material.makeWireFrame(Color.GRAY_25))),
    EMPTY_SERVER_FRAME(new Box().setMaterial(Material.makeWireFrame(new Color(0.1f, 0.1f, 0.1f)))),
    SERVER_FRAME(new Box().setMaterial(Material.makeWireFrame(Color.GRAY_25))),
    HEART(new Heart(DetailLevel.MEDIUM_LOW).setMaterial(Material.INHERIT)),
    SCROLL(new Scroll(DetailLevel.MEDIUM_LOW).setMaterial(Material.INHERIT)),
    STAR(new Star(0.5, 0.2, 5, 0.125).setMaterial(Material.INHERIT)),
    COG(new Cog(6, 0.25, 0.1875, DetailLevel.LOW).setMaterial(Material.INHERIT)),
    CRESCENT(new Crescent(0.25, 0.125, DetailLevel.MEDIUM_LOW).setMaterial(Material.INHERIT)),
    CYLINDER(new Cylinder(0.333, 1.0, DetailLevel.MEDIUM).setMaterial(Material.INHERIT)),
    TORUS(new Torus(16, 0.25, 0.1, false).setMaterial(Material.INHERIT)),
    SPHERE(new Sphere(DetailLevel.MEDIUM).scale(0.5).setMaterial(Material.INHERIT)),
    TERTAHEDRON(new Tetrahedron().scale(0.3333).setMaterial(Material.INHERIT)),
    CUBE(new Box().scale(0.5).setMaterial(Material.INHERIT)),
    ICOSAHEDRON(new Icosahedron().scale(0.3333).setMaterial(Material.INHERIT)),
    JIVE_INSTANCE(JiveInstancePresentationSource.class),

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
        this(new AbstractDynamicDetailSource<Shape<?>>(6) {
            public Shape<?> produceAt(DetailLevel detailLevel) {
                return source;
            }
        });
    }

    public DynamicDetail<?> getDynamicDetail() {
        DynamicDetail<?> detail = this.detail.get();
        if (detail == null) {
            //noinspection unchecked
            detail = new DynamicDetail(getSource(), DetailLevel.LOW, JiveInstance.DETAIL_COMPUTER_REF);
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
