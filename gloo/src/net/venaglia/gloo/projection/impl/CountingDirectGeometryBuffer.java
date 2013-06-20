package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.bounds.Bounded;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.Coordinate;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.common.util.Tuple2;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 9/28/12
 * Time: 10:20 AM
 */
public class CountingDirectGeometryBuffer extends DirectGeometryBuffer implements Bounded {

    private int count = 0;
    private double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

    @Override
    public void applyBrush(Brush brush) {
        super.applyBrush(brush);
        count++;
    }

    @Override
    public void pushBrush() {
        super.pushBrush();
        count++;
    }

    @Override
    public void popBrush() {
        super.popBrush();
        count++;
    }

    @Override
    public void useTexture(Texture texture, TextureMapping mapping) {
        super.useTexture(texture, mapping);
        count++;
    }

    @Override
    public void clearTexture() {
        super.clearTexture();
        count++;
    }

    @Override
    public void start(GeometrySequence seq) {
        super.start(seq);
        count++;
    }

    @Override
    public void end() {
        super.end();
        count++;
    }

    @Override
    public void coordinates(CoordinateList coordinateList, GeometrySequence seq) {
        super.coordinates(coordinateList, seq);
        count++;
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, int[] order) {
        super.coordinates(coordinateList, seq, order);
        count++;
    }

    public void coordinates(CoordinateList coordinateList, Iterable<Tuple2<GeometrySequence, int[]>> sequences) {
        super.coordinates(coordinateList, sequences);
        count++;
    }

    @Override
    protected void load(CoordinateList coordinateList) {
        super.load(coordinateList);
        extendBounds(coordinateList);
    }

    @Override
    public void vertex(Point point) {
        super.vertex(point);
        extendBounds(point.x, point.y, point.z);
        count++;
    }

    @Override
    public void vertex(double x, double y, double z) {
        super.vertex(x, y, z);
        extendBounds(x, y, z);
        count++;
    }

    private void extendBounds(CoordinateList coordinateList) {
        ByteBuffer data = coordinateList.data();
        int length = coordinateList.size();
        int stride = coordinateList.stride(CoordinateList.Field.VERTEX);
        for (int i = 0, j = coordinateList.offset(CoordinateList.Field.VERTEX); i < length; i++, j += stride) {
            data.position(j);
            extendBounds(data.getDouble(), data.getDouble(), data.getDouble());
        }
    }

    private void extendBounds(double x, double y, double z) {
        this.minX = Math.min(x, minX);
        this.maxX = Math.max(x, maxX);
        this.minY = Math.min(y, minY);
        this.maxY = Math.max(y, maxY);
        this.minZ = Math.min(z, minZ);
        this.maxZ = Math.max(z, maxZ);
    }

    @Override
    public void normal(Vector normal) {
        super.normal(normal);
        count++;
    }

    @Override
    public void normal(double i, double j, double k) {
        super.normal(i, j, k);
        count++;
    }

    @Override
    public void color(Color color) {
        super.color(color);
        count++;
    }

    @Override
    public void color(float r, float g, float b) {
        super.color(r, g, b);
        count++;
    }

    @Override
    public void colorAndAlpha(Color color) {
        super.colorAndAlpha(color);
        count++;
    }

    @Override
    public void colorAndAlpha(float r, float g, float b, float a) {
        super.colorAndAlpha(r, g, b, a);
        count++;
    }

    @Override
    public void coordinate(Coordinate coordinate) {
        super.coordinate(coordinate);
        count++;
    }

    @Override
    public void coordinates(Iterable<Coordinate> coordinates) {
        super.coordinates(coordinates);
        count++;
    }

    @Override
    public void pushTransform() {
        super.pushTransform();
        count++;
    }

    @Override
    public void popTransform() {
        super.popTransform();
        count++;
    }

    @Override
    public void identity() {
        super.identity();
        count++;
    }

    @Override
    public void rotate(Axis axis, double angle) {
        super.rotate(axis, angle);
        count++;
    }

    @Override
    public void rotate(Vector axis, double angle) {
        super.rotate(axis, angle);
        count++;
    }

    @Override
    public void translate(Vector magnitude) {
        super.translate(magnitude);
        count++;
    }

    @Override
    public void scale(double magnitude) {
        super.scale(magnitude);
        count++;
    }

    @Override
    public void scale(Vector magnitude) {
        super.scale(magnitude);
        count++;
    }

    @Override
    public void callDisplayList(int glDisplayListId) {
        super.callDisplayList(glDisplayListId);
        count++;
    }

    public int getCount() {
        return count;
    }

    public void clearCount() {
        count = 0;
    }

    public void clearBounds() {
        minX = minY = minZ = Double.MAX_VALUE;
        maxX = maxY = maxZ = Double.MIN_VALUE;
    }

    public BoundingVolume<?> getBounds() {
        return new BoundingBox(new Point(minX, minY, minZ), new Point(maxX, maxY, maxZ));
    }
}
