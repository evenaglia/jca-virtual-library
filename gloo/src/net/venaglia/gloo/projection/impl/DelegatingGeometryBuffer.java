package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.Coordinate;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureMapping;

import java.awt.geom.Rectangle2D;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * User: ed
 * Date: 9/4/12
 * Time: 2:44 PM
 */
public class DelegatingGeometryBuffer implements GeometryBuffer {

    protected GeometryBuffer delegate;

    public DelegatingGeometryBuffer(GeometryBuffer delegate) {
        this.delegate = delegate;
    }

    public void applyBrush(Brush brush) {
        delegate.applyBrush(brush);
    }

    public void pushBrush() {
        delegate.pushBrush();
    }

    public void popBrush() {
        delegate.popBrush();
    }

    public void useTexture(Texture texture, TextureMapping mapping) {
        delegate.useTexture(texture, mapping);
    }

    public void clearTexture() {
        delegate.clearTexture();
    }

    public void start(GeometrySequence seq) {
        delegate.start(seq);
    }

    public void end() {
        delegate.end();
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq) {
        delegate.coordinates(coordinateList, seq);
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, ShortBuffer order) {
        delegate.coordinates(coordinateList, seq, order);
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, IntBuffer order) {
        delegate.coordinates(coordinateList, seq, order);
    }

    public void coordinates(CoordinateList coordinateList, Drawable drawable) {
        delegate.coordinates(coordinateList, drawable);
    }

    public void coordinate(Coordinate coordinate) {
        delegate.coordinate(coordinate);
    }

    public void coordinates(Iterable<Coordinate> coordinates) {
        delegate.coordinates(coordinates);
    }

    public void vertex(Point point) {
        delegate.vertex(point);
    }

    public void vertex(double x, double y, double z) {
        delegate.vertex(x, y, z);
    }

    public void normal(Vector normal) {
        delegate.normal(normal);
    }

    public void normal(double i, double j, double k) {
        delegate.normal(i, j, k);
    }

    public void color(Color color) {
        delegate.color(color);
    }

    public void color(float r, float g, float b) {
        delegate.color(r, g, b);
    }

    public void colorAndAlpha(Color color) {
        delegate.colorAndAlpha(color);
    }

    public void colorAndAlpha(float r, float g, float b, float a) {
        delegate.colorAndAlpha(r, g, b, a);
    }

    public void pushTransform() {
        delegate.pushTransform();
    }

    public void popTransform() {
        delegate.popTransform();
    }

    public void identity() {
        delegate.identity();
    }

    public void rotate(Axis axis, double angle) {
        delegate.rotate(axis, angle);
    }

    public void rotate(Vector axis, double angle) {
        delegate.rotate(axis, angle);
    }

    public void translate(Vector magnitude) {
        delegate.translate(magnitude);
    }

    public void scale(double magnitude) {
        delegate.scale(magnitude);
    }

    public void scale(Vector magnitude) {
        delegate.scale(magnitude);
    }

    public void callDisplayList(int glDisplayListId) {
        delegate.callDisplayList(glDisplayListId);
    }

    public Point whereIs(Point point) {
        return delegate.whereIs(point);
    }

    public Rectangle2D whereIs(BoundingBox bounds) {
        return delegate.whereIs(bounds);
    }

    public double viewingAngle(BoundingBox bounds, Point observer) {
        return delegate.viewingAngle(bounds, observer);
    }

    public boolean isScreen() {
        return delegate.isScreen();
    }

    public boolean isTarget() {
        return delegate.isTarget();
    }

    public boolean isOverlay() {
        return delegate.isOverlay();
    }

    public boolean isVirtual() {
        return delegate.isVirtual();
    }
}
