package net.venaglia.realms.common.util.work;

/**
 * User: ed
 * Date: 1/25/13
 * Time: 8:41 AM
 */
public final class WorkSourceKey<T> {

    public static final WorkSourceKey<?>[] NO_DEPENDENCIES = {};

    private final String name;
    private final Class<T> type;

    private WorkSourceKey(String name, Class<T> type) {
        if (name == null) throw new NullPointerException("name");
        if (type == null) throw new NullPointerException("type");
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public boolean is(Object o) {
        return type.isInstance(o);
    }

    public T cast(Object o) {
        return type.cast(o);
    }

    @Override
    public String toString() {
        return String.format("%s<%s>", name, type.getSimpleName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WorkSourceKey that = (WorkSourceKey)o;

        return name.equals(that.name) && type.equals(that.type);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> WorkSourceKey<T> create(String name, Class<?> type) {
        return new WorkSourceKey<T>(name, (Class<T>)type);
    }
}
