package net.venaglia.realms.common.chemistry.compounds;

import net.venaglia.common.util.ImmutablePairSet;
import net.venaglia.common.util.ImmutableTripleSet;
import net.venaglia.common.util.Pair;
import net.venaglia.common.util.Series;
import net.venaglia.realms.common.chemistry.elements.Element;
import net.venaglia.realms.common.chemistry.elements.ElementalFamily;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 5/21/14
 * Time: 7:52 PM
 */
public class Formula<E extends Element> implements Series<E> {

    private static final Map<Integer,String> FORMATS = new HashMap<Integer,String>();

    static {
        FORMATS.put(2, "(%s%s)");
        FORMATS.put(3, "(%s%s%s)");
        FORMATS.put(5, "(%s%s)~(%s%s%s)");
        FORMATS.put(8, "(%s%s%s)~((%s%s)~(%s%s%s))");
        FORMATS.put(13, "((%s%s)~(%s%s%s))~((%s%s%s)~((%s%s)~(%s%s%s)))");
    }

    /**
     * 2 or 3 elements, simple
     * 5 elements : (2 ~ 3)
     * 8 elements : (3 ~ (2 ~ 3))
     * 13 elements : (2 ~ 3) ~ (3 ~ (2 ~ 3))
     */
    private final Element[] sequence;
    private final ElementalFamily elementalFamily;
    private final String formulaString;
    private final int size;

    private Formula(boolean sort, Element... sequence) {
        size = sequence.length;
        assert FORMATS.containsKey(size);
        if (sort) {
            Arrays.sort(sequence);
        }
        this.elementalFamily = sequence[0].getElementalFamily();
        Object[] abbreviations = new Object[size];
        for (int i = 0; i < sequence.length; i++) {
            abbreviations[i] = sequence[i].getAbbreviation();
            assert sequence[i].getElementalFamily() == elementalFamily;
        }
        this.sequence = sequence;
        this.formulaString = String.format(FORMATS.get(size), abbreviations);
    }

    public int size() {
        return size;
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {

            private int i = 0;

            public boolean hasNext() {
                return i < size;
            }

            @SuppressWarnings("unchecked")
            public E next() {
                if (i >= size()) {
                    throw new NoSuchElementException();
                }
                return get(i++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public List<Set<E>> getTuples() {
        List<Set<E>> tuples = new ArrayList<Set<E>>();
        Iterator<E> iter = iterator();
        switch (size) {
            case 2:
                tuples.add(new ImmutablePairSet<E>(iter.next(), iter.next()));
                break;
            case 3:
                tuples.add(new ImmutableTripleSet<E>(iter.next(), iter.next(), iter.next()));
                break;
            case 5:
                tuples.add(new ImmutablePairSet<E>(iter.next(), iter.next()));
                tuples.add(new ImmutableTripleSet<E>(iter.next(), iter.next(), iter.next()));
            case 8:
                tuples.add(new ImmutableTripleSet<E>(iter.next(), iter.next(), iter.next()));
                tuples.add(new ImmutablePairSet<E>(iter.next(), iter.next()));
                tuples.add(new ImmutableTripleSet<E>(iter.next(), iter.next(), iter.next()));
                break;
            case 13:
                tuples.add(new ImmutablePairSet<E>(iter.next(), iter.next()));
                tuples.add(new ImmutableTripleSet<E>(iter.next(), iter.next(), iter.next()));
                tuples.add(new ImmutableTripleSet<E>(iter.next(), iter.next(), iter.next()));
                tuples.add(new ImmutablePairSet<E>(iter.next(), iter.next()));
                tuples.add(new ImmutableTripleSet<E>(iter.next(), iter.next(), iter.next()));
                break;
        }
        return tuples;
    }

    public Set<Formula<E>> decompose() {
        switch (size) {
            case 5:
                return new ImmutablePairSet<Formula<E>>(
                        new Formula<E>(false, sequence[0], sequence[1]),
                        new Formula<E>(false, sequence[2], sequence[3], sequence[4])
                );
            case 8:
                return new ImmutablePairSet<Formula<E>>(
                        new Formula<E>(false, sequence[0], sequence[1], sequence[2]),
                        new Formula<E>(false, sequence[3], sequence[4], sequence[5], sequence[6], sequence[7])
                );
            case 13:
                return new ImmutablePairSet<Formula<E>>(
                        new Formula<E>(false, sequence[0], sequence[1], sequence[2], sequence[3], sequence[4]),
                        new Formula<E>(false, sequence[5], sequence[6], sequence[7], sequence[8], sequence[9], sequence[10], sequence[11], sequence[12])
                );
        }
        return Collections.emptySet();
    }

    @SuppressWarnings("unchecked")
    private E get(int i) {
        return (E)sequence[i];
    }

    public String getFormulaString() {
        return formulaString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Formula elements = (Formula)o;
        return formulaString.equals(elements.formulaString);
    }

    @Override
    public int hashCode() {
        return formulaString.hashCode();
    }

    @Override
    public String toString() {
        return formulaString;
    }

    public static <E extends Element> Formula<E> build(E e1, E e2) {
        assert !e1.equals(e2);
        return new Formula<E>(true, e1, e2);
    }

    public static <E extends Element> Formula<E> build(E e1, E e2, E e3) {
        assert !e1.equals(e2);
        assert !e1.equals(e3);
        assert !e2.equals(e3);
        return new Formula<E>(true, e1, e2, e3);
    }

    public static <E extends Element> Formula<E> combine(Formula<E> f1, Formula<E> f2) {
        int size = f1.size + f2.size;
        assert f1.size < f2.size;
        assert f1.elementalFamily == f2.elementalFamily;
        assert FORMATS.containsKey(size);
        Element[] sequence = new Element[size];
        System.arraycopy(f1.sequence, 0, sequence, 0, f1.size);
        System.arraycopy(f2.sequence, 0, sequence, f1.size, f2.size);
        return new Formula<E>(false, sequence);
    }

    public static <E extends Enum & Element> Formula<E> parse(String formulaString, Class<E> domain) {
        Pair<Pattern,Map<String,E>> pair = parsingDomain(domain);
        Matcher matcher = pair.getA().matcher(formulaString);
        List<E> sequence = new ArrayList<E>(13);
        int end = 0;
        while (matcher.find()) {
            assert matcher.start() == end || formulaString.substring(end, matcher.start()).matches("[~()]+");
            end = matcher.start();
            E element = pair.getB().get(matcher.group());
            assert element != null;
            sequence.add(element);
            assert sequence.size() < 15;
        }
        assert formulaString.length() == end || formulaString.substring(end).matches("[~()]+");
        assert FORMATS.containsKey(sequence.size());
        switch (sequence.size()) {
            case 2:
                sortFragment(sequence, 0, 2);
                break;
            case 3:
                sortFragment(sequence, 0, 3);
                break;
            case 5:
                sortFragment(sequence, 0, 2);
                sortFragment(sequence, 2, 5);
                break;
            case 8:
                sortFragment(sequence, 0, 3);
                sortFragment(sequence, 3, 5);
                sortFragment(sequence, 5, 8);
                break;
            case 13:
                sortFragment(sequence,  0,  2);
                sortFragment(sequence,  2,  5);
                sortFragment(sequence,  5,  8);
                sortFragment(sequence,  8, 10);
                sortFragment(sequence, 10, 13);
                break;
        }
        return new Formula<E>(false, sequence.toArray(new Element[sequence.size()]));
    }

    private static ConcurrentHashMap<Class<?>,Pair<Pattern,Map<String,?>>> PATTERN_CACHE =
            new ConcurrentHashMap<Class<?>,Pair<Pattern,Map<String,?>>>();

    private static <E extends Enum & Element> Pair<Pattern,Map<String,E>> parsingDomain(Class<E> domain) {
        Pair<Pattern,Map<String,?>> pair = PATTERN_CACHE.get(domain);
        if (pair == null) {
            E[] elements = domain.getEnumConstants();
            StringBuilder regex = new StringBuilder();
            Map<String,E> lookup = new HashMap<String,E>();
            for (E element : elements) {
                regex.append("|");
                regex.append(element.getAbbreviation());
            }
            Pattern matchElements = Pattern.compile(regex.toString());
            pair = new Pair<Pattern,Map<String,?>>(matchElements, lookup);
            PATTERN_CACHE.put(domain, pair);
        }
        //noinspection unchecked
        return (Pair<Pattern,Map<String,E>>)(Pair)pair;
    }

    private static <E extends Comparable> void sortFragment(List<E> list, int from, int to) {
        List<E> fragment = list.subList(from, to);
        assert fragment.size() == 2 || fragment.size() == 3;
        assert new HashSet<E>(fragment).size() == fragment.size();
        assert new ArrayList<E>(new TreeSet<E>(fragment)).equals(new ArrayList<E>(fragment));
    }
}
