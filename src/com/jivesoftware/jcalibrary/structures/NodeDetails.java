package com.jivesoftware.jcalibrary.structures;

import com.jivesoftware.jcalibrary.objects.ColorCycle;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.primitives.Cylinder;
import net.venaglia.realms.common.physical.geom.primitives.Icosahedron;
import net.venaglia.realms.common.physical.geom.primitives.Scroll;
import net.venaglia.realms.common.physical.geom.primitives.Star;
import net.venaglia.realms.common.physical.geom.primitives.TriangleSequence;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;
import net.venaglia.realms.common.util.Pair;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
* User: ed
* Date: 5/10/13
* Time: 2:45 PM
*/
public class NodeDetails {

    private static final Material DEEP_RED = new ColorCycle(Brush.FRONT_SHADED, new Color(0.7f,0,0,1), 333);
//    private static final Material WHITE = Material.makeFrontShaded(Color.WHITE);
    private static final Material OFF_WHITE = Material.makeFrontShaded(new Color(1,0.95f,0.9f));
    private static final Material YELLOW = Material.makeFrontShaded(new Color(0.9f,0.7f,0));
    private static final Material MAGENTA = Material.makeFrontShaded(new Color(0.7f,0,0.7f));
    private static final Material GREEN = Material.makeFrontShaded(new Color(0,0.7f,0.3f));
    private static final Map<String,Projectable> SHAPES_BY_TYPE;
    private static final Map<String,Pair<Material,Material>> COLORS_BY_TYPE;

    static {
        Map<String,Projectable> shapes = new HashMap<String,Projectable>();
        shapes.put("thunder", new Cylinder(0.5, 0.125, DetailLevel.MEDIUM_LOW).rotate(Axis.X, Math.PI / 2).setMaterial(Material.INHERIT));
        shapes.put("dbvirtual", new Cylinder(0.25, 0.85, DetailLevel.MEDIUM_LOW).setMaterial(Material.INHERIT));
        shapes.put("cache", new Scroll(DetailLevel.MEDIUM_LOW).scale(0.2).setMaterial(Material.INHERIT));
        shapes.put("dedicatedsearch", new TriangleSequence(new Icosahedron().scale(0.2)).setMaterial(Material.INHERIT));
        shapes.put("dbanalytics", new Cylinder(0.25, 0.85, DetailLevel.MEDIUM_LOW).setMaterial(Material.INHERIT));
        shapes.put("webapp", new TriangleSequence(new Icosahedron().scale(0.2)).setMaterial(Material.INHERIT));
        shapes.put("dbeae", new Cylinder(0.25, 0.85, DetailLevel.MEDIUM_LOW).setMaterial(Material.INHERIT));
        shapes.put("eaeservice", new Star(0.5, 0.1875, 5, 0.125).setMaterial(Material.INHERIT));
        SHAPES_BY_TYPE = Collections.unmodifiableMap(shapes);
        Map<String,Pair<Material,Material>> colors = new HashMap<String,Pair<Material,Material>>();
        colors.put("thunder", new Pair<Material,Material>(OFF_WHITE, DEEP_RED));
        colors.put("dbvirtual", new Pair<Material,Material>(OFF_WHITE, DEEP_RED));
        colors.put("cache", new Pair<Material,Material>(OFF_WHITE, DEEP_RED));
        colors.put("dedicatedsearch", new Pair<Material,Material>(YELLOW, DEEP_RED));
        colors.put("dbanalytics", new Pair<Material,Material>(MAGENTA, DEEP_RED));
        colors.put("webapp", new Pair<Material,Material>(OFF_WHITE, DEEP_RED));
        colors.put("dbeae", new Pair<Material,Material>(GREEN, DEEP_RED));
        colors.put("eaeservice", new Pair<Material,Material>(GREEN, DEEP_RED));
        COLORS_BY_TYPE = Collections.unmodifiableMap(colors);
    }

    private Date timestamp;
    private String details;
    private String type; // thunder, dbvirtual, cache, dedicatedsearch, dbanalytics, webapp, dbeae, eaeservice
    private String status;
    private String url;

    private int activeConnections;
    private int activeSessions;
    private float loadAverage;

    private AtomicBoolean dirty;

    public NodeDetails(AtomicBoolean dirty) {
        this.dirty = dirty;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        if (!eq(this.timestamp, timestamp)) {
            this.timestamp = timestamp;
            dirty.set(true);
        }
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        if (!eq(this.details, details)) {
            this.details = details;
            dirty.set(true);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (!eq(this.type, type)) {
            this.type = type;
            dirty.set(true);
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (!eq(this.status, status)) {
            this.status = status;
            dirty.set(true);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (!eq(this.url, url)) {
            this.url = url;
            dirty.set(true);
        }
    }

    public int getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(int activeConnections) {
        if (this.activeConnections != activeConnections) {
            this.activeConnections = activeConnections;
            dirty.set(true);
        }
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(int activeSessions) {
        if (this.activeSessions != activeSessions) {
            this.activeSessions = activeSessions;
            dirty.set(true);
        }
    }

    public float getLoadAverage() {
        return loadAverage;
    }

    public void setLoadAverage(float loadAverage) {
        if (this.loadAverage != loadAverage) {
            this.loadAverage = loadAverage;
        }
    }

    private <T> boolean eq(T a, T b) {
        return a == b || !(a == null || b == null) && a.equals(b);
    }

    public void project(long nowMS, GeometryBuffer buffer, DetailLevel detailLevel, Transformation xform) {
        if (detailLevel == null) return;
        Projectable shape = SHAPES_BY_TYPE.get(type);
        if (shape != null) {
            buffer.pushTransform();
            try {
                Pair<Material,Material> pair = COLORS_BY_TYPE.get(type);
                if (pair != null) {
                    Material material = "down".equals(status) ? pair.getB() : pair.getA();
                    material.apply(nowMS, buffer);
                }
                xform.apply(nowMS, buffer);
                shape.project(nowMS, buffer);
            } finally {
                buffer.popTransform();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeDetails that = (NodeDetails)o;

        if (activeConnections != that.activeConnections) return false;
        if (activeSessions != that.activeSessions) return false;
        if (Float.compare(that.loadAverage, loadAverage) != 0) return false;
        if (details != null ? !details.equals(that.details) : that.details != null) return false;
        if (dirty != null ? !dirty.equals(that.dirty) : that.dirty != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp != null ? timestamp.hashCode() : 0;
        result = 31 * result + (details != null ? details.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + activeConnections;
        result = 31 * result + activeSessions;
        result = 31 * result + (loadAverage != +0.0f ? Float.floatToIntBits(loadAverage) : 0);
        result = 31 * result + (dirty != null ? dirty.hashCode() : 0);
        return result;
    }
}
