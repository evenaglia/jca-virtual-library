package net.venaglia.realms.common.util;

import java.util.Comparator;

/**
 * User: ed
 * Date: 2/4/13
 * Time: 8:12 AM
 */
public class Pair<A,B> implements Tuple2<A,B> {

    private static final Comparator CMP_A = new Comparator<Pair<Comparable,?>>() {
        @SuppressWarnings("unchecked")
        public int compare(Pair<Comparable,?> l, Pair<Comparable,?> r) {
            return l.getA().compareTo(r.getA());
        }
    };
    private static final Comparator CMP_B = new Comparator<Pair<?,Comparable>>() {
            @SuppressWarnings("unchecked")
            public int compare(Pair<?,Comparable> l, Pair<?,Comparable> r) {
                return l.getB().compareTo(r.getB());
            }
        };

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Comparator<Pair<T,?>> compareA() {
        return (Comparator<Pair<T,?>>)CMP_A;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Comparator<Pair<?,T>> compareB() {
        return (Comparator<Pair<?,T>>)CMP_B;
    }

    private final A a;
    private final B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public Pair<B,A> reverse() {
        return new Pair<B,A>(b, a);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair)o;

        if (a != null ? !a.equals(pair.a) : pair.a != null) return false;
        if (b != null ? !b.equals(pair.b) : pair.b != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "a=" + a +
                ", b=" + b +
                '}';
    }
}
