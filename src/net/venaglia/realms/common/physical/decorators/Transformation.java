package net.venaglia.realms.common.physical.decorators;

import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.projection.Decorator;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.impl.StaticDecorator;
import net.venaglia.realms.common.util.Lock;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 10:22 AM
 */
public final class Transformation {

    private final Deque<Decorator> decorators = new LinkedList<Decorator>();
    private final Lock lock;

    private boolean isStatic = true;
    private Runnable runWhenNoLongerStatic = null;
    private Collection<Transformation> directReferences;

    public Transformation() {
        this.lock = Lock.NEVER_LOCKED;
    }

    public Transformation(Lock lock) {
        this.lock = lock;
    }

    public Transformation(Runnable runWhenNoLongerStatic) {
        this.lock = Lock.NEVER_LOCKED;
        this.runWhenNoLongerStatic = runWhenNoLongerStatic;
    }

    public Transformation(Lock lock, Runnable runWhenNoLongerStatic) {
        this.lock = lock;
        this.runWhenNoLongerStatic = runWhenNoLongerStatic;
    }

    public void apply(long now, GeometryBuffer buffer) {
        for (Decorator decorator : decorators) {
            decorator.apply(now, buffer);
        }
    }

    public void scale(final double magnitude) {
        decorate(new StaticDecorator() {
            public void apply(GeometryBuffer buffer) {
                buffer.scale(magnitude);
            }
        });
    }

    public void scale(final Vector magnitude) {
        decorate(new StaticDecorator() {
            public void apply(GeometryBuffer buffer) {
                buffer.scale(magnitude);
            }
        });
    }

    public void translate(final Vector translate) {
        decorate(new StaticDecorator() {
            public void apply(GeometryBuffer buffer) {
                buffer.translate(translate);
            }
        });
    }

    public void rotate(final Axis axis, final double angle) {
        decorate(new StaticDecorator() {
            @Override
            protected void apply(GeometryBuffer buffer) {
                buffer.rotate(axis, angle);
            }
        });
    }

    public void rotate(final Vector axis, final double angle) {
        decorate(new StaticDecorator() {
            @Override
            protected void apply(GeometryBuffer buffer) {
                buffer.rotate(axis, angle);
            }
        });
    }

    public void transform(final Transformation transformation) {
        if (transformation == null) {
            return;
        }
        if (transformation == this || transformation.references(this)) {
            throw new IllegalArgumentException("Refusing to apply recursive transformation");
        }
        if (directReferences == null) {
            directReferences = new LinkedList<Transformation>();
        }
        directReferences.add(transformation);
        decorate(new Decorator() {

            public boolean isStatic() {
                return transformation.isStatic();
            }

            public void apply(long nowMS, GeometryBuffer buffer) {
                transformation.apply(nowMS, buffer);
            }
        });
    }

    protected boolean references(Transformation transformation) {
        if (directReferences == null) {
            return false;
        }
        for (Transformation ref : directReferences) {
            if (ref.references(transformation)) {
                return true;
            }
        }
        return false;
    }

    public void decorate(Decorator decorator) {
        lock.assertUnlocked();
        decorators.addFirst(decorator);
        if (isStatic && !decorator.isStatic()) {
            if (runWhenNoLongerStatic != null) {
                runWhenNoLongerStatic.run();
                runWhenNoLongerStatic = null;
            }
            isStatic = false;
        }
    }

    public boolean isStatic() {
        return isStatic;
    }
}
