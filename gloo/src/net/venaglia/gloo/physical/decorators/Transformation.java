package net.venaglia.gloo.physical.decorators;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.projection.Decorator;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.impl.StaticDecorator;
import net.venaglia.common.util.Lock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 10:22 AM
 */
public final class Transformation {

    private final Deque<Decorator> decorators = new LinkedList<Decorator>();
    private final Lock lock;

    private boolean isStatic = true;
    private List<Op> ops = new ArrayList<Op>();
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

    public boolean isEmpty() {
        return ops.isEmpty();
    }

    public void clear() {
        decorators.clear();
        ops.clear();
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
        }, new Op("sca", "%s", magnitude));
    }

    public void scale(final Vector magnitude) {
        decorate(new StaticDecorator() {
            public void apply(GeometryBuffer buffer) {
                buffer.scale(magnitude);
            }
        }, new Op("sca", "%s", magnitude));
    }

    public void translate(final Vector translate) {
        decorate(new StaticDecorator() {
            public void apply(GeometryBuffer buffer) {
                buffer.translate(translate);
            }
        }, new Op("xlt", "%s", translate));
    }

    public void rotate(final Axis axis, final double angle) {
        decorate(new StaticDecorator() {
            @Override
            protected void apply(GeometryBuffer buffer) {
                buffer.rotate(axis, angle);
            }
        }, new Op("rot", "%s,%.4f", axis.name(), angle));
    }

    public void rotate(final Vector axis, final double angle) {
        decorate(new StaticDecorator() {
            @Override
            protected void apply(GeometryBuffer buffer) {
                buffer.rotate(axis, angle);
            }
        }, new Op("rot", "%s,%.4f", axis, angle));
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
        }, new Op("xfm", "%s", transformation));
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

    public void decorate(Decorator decorator, Op op) {
        lock.assertUnlocked();
        decorators.addFirst(decorator);
        if (isStatic && !decorator.isStatic()) {
            if (runWhenNoLongerStatic != null) {
                runWhenNoLongerStatic.run();
                runWhenNoLongerStatic = null;
            }
            isStatic = false;
        }
        ops.add(op);
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getOp() {
        StringBuilder buffer = new StringBuilder();
        for (Op op : ops) {
            if (buffer.length() > 0) {
                buffer.append(" -> ");
            }
            buffer.append(op);
        }
        return buffer.toString();
    }

    @Override
    public int hashCode() {
        return ops.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Transformation && ops.equals(((Transformation)obj).ops);
    }

    @Override
    public String toString() {
        return getOp();
    }

    public static final class Op {
        private final String name;
        private final String format;
        private final Object[] params;

        public Op(String name, String format, Object... params) {
            if (name == null) throw new NullPointerException("name");
            if (format == null) throw new NullPointerException("format");
            if (params == null) throw new NullPointerException("params");
            this.name = name;
            this.format = format;
            this.params = params;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Op op = (Op)o;

            if (!format.equals(op.format)) return false;
            if (!name.equals(op.name)) return false;
            if (!Arrays.equals(params, op.params)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + format.hashCode();
            result = 31 * result + Arrays.hashCode(params);
            return result;
        }

        @Override
        public String toString() {
            return name + "(" + String.format(format, params) + ")";
        }
    }
}
