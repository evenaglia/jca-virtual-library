package net.venaglia.common.util;

/**
 * User: ed
 * Date: 1/25/13
 * Time: 8:41 AM
 */
public abstract class StronglyTypedKey<T> {

    private final String name;
    private final Class<T> type;

    protected StronglyTypedKey(String name, Class<T> type) {
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
        StronglyTypedKey that = (StronglyTypedKey)o;
        return name.equals(that.name) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}
