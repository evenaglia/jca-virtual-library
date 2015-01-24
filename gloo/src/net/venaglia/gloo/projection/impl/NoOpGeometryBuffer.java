package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.projection.Coordinate;
import net.venaglia.gloo.projection.CoordinateList;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * User: ed
 * Date: 5/27/13
 * Time: 9:08 PM
 */
public abstract class NoOpGeometryBuffer extends DisabledGeometryBuffer {

    public void applyBrush(Brush brush) {
        // no-op
    }

    public void pushBrush() {
        // no-op
    }

    public void popBrush() {
        // no-op
    }

    public void useTexture(Texture texture, TextureMapping mapping) {
        // no-op
    }

    public void clearTexture() {
        // no-op
    }

    public void start(GeometrySequence seq) {
        // no-op
    }

    public void end() {
        // no-op
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq) {
        // no-op
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, ShortBuffer order) {
        // no-op
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, IntBuffer order) {
        // no-op
    }

    public void coordinates(CoordinateList coordinateList, Drawable drawable) {
        // no-op
    }

    public void pushTransform() {
        // no-op
    }

    public void popTransform() {
        // no-op
    }

    public void identity() {
        // no-op
    }

    public void rotate(Axis axis, double angle) {
        // no-op
    }

    public void rotate(Vector axis, double angle) {
        // no-op
    }

    public void translate(Vector magnitude) {
        // no-op
    }

    public void scale(double magnitude) {
        // no-op
    }

    public void scale(Vector magnitude) {
        // no-op
    }

    public void callDisplayList(int glDisplayListId) {
        // no-op
    }

    public void vertex(Point point) {
        // no-op
    }

    public void vertex(double x, double y, double z) {
        // no-op
    }

    public void normal(Vector normal) {
        // no-op
    }

    public void normal(double i, double j, double k) {
        // no-op
    }

    public void color(Color color) {
        // no-op
    }

    public void color(float r, float g, float b) {
        // no-op
    }

    public void colorAndAlpha(Color color) {
        // no-op
    }

    public void colorAndAlpha(float r, float g, float b, float a) {
        // no-op
    }

    public void coordinate(Coordinate coordinate) {
        // no-op
    }

    public void coordinates(Iterable<Coordinate> coordinates) {
        // no-op
    }

    @Override
    protected void fail() {
        throw new UnsupportedOperationException("NoOpGeometryBuffer does not support read operations");
    }
}
