package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.primitives.TriangleSequence;
import net.venaglia.realms.common.physical.text.FontBuilder;
import net.venaglia.realms.common.physical.text.TextRibbon;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;
import net.venaglia.realms.common.util.Pair;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 5/12/13
 * Time: 11:45 PM
 */
public class LibraryBanner implements Projectable {

    public static final LibraryBanner INSTANCE = new LibraryBanner();

    private final double sqrt3 = Math.sqrt(3);
    private final double rotate120 = Math.PI * 0.666667;
    private final TriangleSequence bottomCover = new TriangleSequence(
            new Point(0, Math.sqrt(3) - 1.0 / -Math.sqrt(3), 0),
            new Point(1, 1.0 / Math.sqrt(3), 0),
            new Point(-1, 1.0 / Math.sqrt(3), 0)
    );

    private Library library;
    private String text = "";
    private FontBuilder fontBuilder;
    private AtomicReference<Pair<TextRibbon,TriangleSequence>> textRibbon =
            new AtomicReference<Pair<TextRibbon,TriangleSequence>>();

    private LibraryBanner() {
        // singleton
        fontBuilder = new FontBuilder("fonts/DroidSansMono.ttf", DetailLevel.MEDIUM);
    }

    public void setText(String text) {
        if (text == null || library == null) {
            text = "";
            this.textRibbon.set(null);
        }
        if (!this.text.equals(text)) {
            this.text = text;
            if (text.length() == 0) {
                this.textRibbon.set(null);
            } else {
                TextRibbon textRibbon = new TextRibbon(fontBuilder, " " + text + " ");
                BoundingVolume<?> bounds = textRibbon.getBounds();
                double r = bounds.min(Axis.X) / sqrt3;
                textRibbon = textRibbon.translate(new Vector(0, r, library.getCeilingHeight() + 1.4));
                TriangleSequence bottom = bottomCover.scale(r)
                                                     .translate(new Vector(0, 0, library.getCeilingHeight() + 0.9))
                                                     .setMaterial(Material.makeFrontShaded(Color.BLACK));
                this.textRibbon.set(new Pair<TextRibbon,TriangleSequence>(textRibbon, bottom));
            }
        }
    }

    public void clear() {
        setText(null);
    }

    public void setLibrary(Library library) {
        this.library = library;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public void project(long nowMS, GeometryBuffer buffer) {
        Pair<TextRibbon,TriangleSequence> pair = this.textRibbon.get();
        if (pair != null) {
//            pair.getB().project(nowMS, buffer);
            TextRibbon textRibbon = pair.getA();
            buffer.pushTransform();
            textRibbon.project(nowMS, buffer);
            buffer.rotate(Axis.Z, rotate120);
            textRibbon.project(nowMS, buffer);
            buffer.rotate(Axis.Z, rotate120);
            textRibbon.project(nowMS, buffer);
            buffer.popTransform();
        }
    }
}
