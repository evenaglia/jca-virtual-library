package net.venaglia.common.util;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 9:20 PM
 */
public class Triple<A,B,C> implements Tuple3<A,B,C> {

    private final A a;
    private final B b;

    public Triple(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    private final C c;

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public C getC() {
        return c;
    }

    public Triple<C,B,A> reverse() {
        return new Triple<C,B,A>(c, b, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Triple tuple3 = (Triple)o;

        if (a != null ? !a.equals(tuple3.a) : tuple3.a != null) return false;
        if (b != null ? !b.equals(tuple3.b) : tuple3.b != null) return false;
        if (c != null ? !c.equals(tuple3.c) : tuple3.c != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + (c != null ? c.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Tuple3{" +
                "a=" + a +
                ", b=" + b +
                ", c=" + c +
                '}';
    }
}
