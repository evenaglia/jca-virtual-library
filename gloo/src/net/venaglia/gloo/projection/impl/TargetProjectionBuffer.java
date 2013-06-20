package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 3/31/13
 * Time: 10:56 AM
 */
public class TargetProjectionBuffer extends DelegatingGeometryBuffer {

    public TargetProjectionBuffer(GeometryBuffer delegate) {
        super(delegate);
    }

    @Override
    public void applyBrush(Brush brush) {
        // no-op
    }

    @Override
    public void pushBrush() {
        // no-op
    }

    @Override
    public void popBrush() {
        // no-op
    }

    @Override
    public void useTexture(Texture texture, TextureMapping mapping) {
        // no-op
    }

    @Override
    public void normal(Vector normal) {
        // no-op
    }

    @Override
    public void normal(double i, double j, double k) {
        // no-op
    }

    @Override
    public void clearTexture() {
        // no-op
    }

    @Override
    public void color(Color color) {
        // no-op
    }

    @Override
    public void color(float r, float g, float b) {
        // no-op
    }

    @Override
    public void colorAndAlpha(Color color) {
        // no-op
    }

    @Override
    public void colorAndAlpha(float r, float g, float b, float a) {
        // no-op
    }

    @Override
    public boolean isScreen() {
        return false;
    }

    @Override
    public boolean isOverlay() {
        return false;
    }

    @Override
    public boolean isTarget() {
        return true;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }
}
