package net.venaglia.realms.common.projection.impl;

import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.projection.Coordinate;
import net.venaglia.realms.common.projection.CoordinateList;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.util.Tuple2;

import java.awt.geom.Rectangle2D;

/**
 * User: ed
 * Date: 4/5/13
 * Time: 8:59 PM
 */
public abstract class DisabledGeometryBuffer implements GeometryBuffer {

    public void applyBrush(Brush brush) {
        fail();
    }

    public void pushBrush() {
        fail();
    }

    public void popBrush() {
        fail();
    }

    public void useTexture(Texture texture, TextureMapping mapping) {
        fail();
    }

    public void clearTexture() {
        fail();
    }

    public void start(GeometrySequence seq) {
        fail();
    }

    public void end() {
        fail();
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq) {
        fail();
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, int[] order) {
        fail();
    }

    public void coordinates(CoordinateList coordinateList, Iterable<Tuple2<GeometrySequence, int[]>> sequences) {
        fail();
    }

    public void pushTransform() {
        fail();
    }

    public void popTransform() {
        fail();
    }

    public void rotate(Axis axis, double angle) {
        fail();
    }

    public void rotate(Vector axis, double angle) {
        fail();
    }

    public void translate(Vector magnitude) {
        fail();
    }

    public void scale(double magnitude) {
        fail();
    }

    public void scale(Vector magnitude) {
        fail();
    }

    public void callDisplayList(int glDisplayListId) {
        fail();
    }

    public Point whereIs(Point point) {
        fail();
        return null;
    }

    public Rectangle2D whereIs(BoundingBox bounds) {
        fail();
        return null;
    }

    public double viewingAngle(BoundingBox bounds, Point observer) {
        fail();
        return 0;
    }

    public void vertex(Point point) {
        fail();
    }

    public void vertex(double x, double y, double z) {
        fail();
    }

    public void normal(Vector normal) {
        fail();
    }

    public void normal(double i, double j, double k) {
        fail();
    }

    public void color(Color color) {
        fail();
    }

    public void color(float r, float g, float b) {
        fail();
    }

    public void colorAndAlpha(Color color) {
        fail();
    }

    public void colorAndAlpha(float r, float g, float b, float a) {
        fail();
    }

    public void coordinate(Coordinate coordinate) {
        fail();
    }

    public void coordinates(Iterable<Coordinate> coordinates) {
        fail();
    }

    public boolean isScreen() {
        fail();
        return false;
    }

    public boolean isTarget() {
        fail();
        return false;
    }

    public boolean isOverlay() {
        fail();
        return false;
    }

    public boolean isVirtual() {
        fail();
        return false;
    }

    protected abstract void fail();
}
