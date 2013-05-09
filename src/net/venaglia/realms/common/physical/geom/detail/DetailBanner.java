package net.venaglia.realms.common.physical.geom.detail;

import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureCoordinate;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.physical.texture.impl.TextureFactory;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 5/8/13
 * Time: 11:11 PM
 */
public class DetailBanner extends AbstractShape<DetailBanner> implements DetailListener {

    private static final Brush BRUSH;
    private static final ThreadLocal<Texture> TEXTURE = new ThreadLocal<Texture>() {
        @Override
        protected Texture initialValue() {
            return new TextureFactory().loadClasspathResource("images/detail-level.png").build();
        }
    };

    static {
        Brush brush = new Brush();
        brush.setTexturing(true);
        BRUSH = brush.immutable();
    }

    private static final Point[] BASE_POINTS = {
            new Point(-0.5,0,0.1),
            new Point(-0.5,0,-0.1),
            new Point(0.5,0,-0.1),
            new Point(0.5,0,0.1),
    };

    private final Vector normal;
    private final Point center;
    private final MyTextureMapping myTextureMapping;

    private DetailLevel detailLevel;

    public DetailBanner(DetailLevel detailLevel) {
        this(BASE_POINTS.clone(), Vector.Y.reverse(), Point.ORIGIN, detailLevel);
    }

    private DetailBanner(Point[] points, Vector normal, Point center, DetailLevel detailLevel) {
        super(points);
        this.normal = normal;
        this.center = center;
        this.detailLevel = detailLevel;
        this.myTextureMapping = new MyTextureMapping();
    }

    @Override
    protected DetailBanner build(Point[] points, XForm xForm) {
        return new DetailBanner(points, xForm.apply(normal), xForm.apply(center), detailLevel);
    }

    public void usingDetail(DetailLevel detailLevel) {
        if (this.detailLevel != detailLevel) {
            this.detailLevel = detailLevel;
            this.myTextureMapping.updateDetailLevel();
        }
    }

    @Override
    public DetailBanner setMaterial(Material material) {
        throw new UnsupportedOperationException();
    }

    public Vector getNormal(int index) {
        if (index < 0 || index > 4) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return normal;
    }

    public Point getCenter() {
        return center;
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.pushBrush();
        buffer.applyBrush(BRUSH);

        buffer.useTexture(TEXTURE.get(), myTextureMapping);
        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
        buffer.normal(normal);
        buffer.vertex(points[0]);
        buffer.vertex(points[1]);
        buffer.vertex(points[2]);
        buffer.vertex(points[3]);
        buffer.end();

        buffer.popBrush();
    }

    private class MyTextureMapping implements TextureMapping {

        private int seq = 0;
        private TextureCoordinate[] coords;

        private MyTextureMapping() {
            updateDetailLevel();
        }

        public void updateDetailLevel() {
            float y1 = detailLevel == null ? 0 : 0.8f - detailLevel.ordinal() * 0.2f;
            float y2 = detailLevel == null ? 0 : y1 + 0.2f;
            this.coords = new TextureCoordinate[] {
                    new TextureCoordinate(0,y1),
                    new TextureCoordinate(0,y2),
                    new TextureCoordinate(1,y2),
                    new TextureCoordinate(1,y1)
            };
        }

        public void newSequence() {
            seq = 0;
        }

        public TextureCoordinate unwrap(Point p) {
            return getNextTextureCoordinate();
        }

        public void unwrap(double x, double y, double z, float[] out) {
            TextureCoordinate coord = getNextTextureCoordinate();
            out[0] = coord.s;
            out[1] = coord.t;
        }

        private TextureCoordinate getNextTextureCoordinate() {
            TextureCoordinate coord = coords[seq % coords.length];
            seq++;
            return coord;
        }

        @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
        public TextureMapping clone() {
            return new MyTextureMapping();
        }
    }
}
