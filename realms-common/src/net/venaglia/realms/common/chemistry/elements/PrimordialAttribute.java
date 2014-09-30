package net.venaglia.realms.common.chemistry.elements;

import net.venaglia.common.util.PrimeFactors;

import java.util.Set;
import java.util.NavigableSet;
import java.util.TreeSet;

/**
 * User: ed
 * Date: 5/16/14
 * Time: 8:44 PM
 *
 * Hidden properties that drive the manifestations of all of the elements
 */
public class PrimordialAttribute {

    private final ResonanceSite[] span;
    private final float s; // 0.5 or -0.5, indicating the direction the span rotates around
    private final int l; // plane: 0, 1, 2, or 3
    private final int f; // internal frequency of this attribute
    private final float e; // overall energy of this attribute
    private final int m; // mass attribute
    private final float g; // how big the gate to the elemental dimension is; bigger gates take longer to close and release more stuff
    private final String string;

    PrimordialAttribute(float spin) {
        string = "-";
        span = new ResonanceSite[]{ null, null, null };
        s = spin;
        l = spin == 0.0f ? 0 : 3;
        f = 0;
        e = 0;
        m = 0;
        g = 0;
    }

    PrimordialAttribute(ResonanceSite p) {
        string = p.name();
        span = new ResonanceSite[]{ p, p, p };
        l = 0;
        s = 0;
        f = 0;
        e = 0;
        m = 0;
        g = 0;
    }

    PrimordialAttribute(ResonanceSite p, int wind1, int wind2) {
        this(p.wind(wind1).name() + p.name() +  p.wind(wind2).name());
    }

    public PrimordialAttribute(String def) {

        assert def.matches("[A-E]{3}");
        final ResonanceSite p1 = ResonanceSite.valueOf(def.substring(0, 1));
        final ResonanceSite p2 = ResonanceSite.valueOf(def.substring(1, 2));
        final ResonanceSite p3 = ResonanceSite.valueOf(def.substring(2, 3));

        int winding1 = p1.winding(p2);
        int winding2 = p2.winding(p3);
        assert winding1 != 0 && winding2 != 0;
        assert winding1 != winding2;
        assert winding1 + winding2 != 0;
        assert Math.signum(winding1) == Math.signum(winding2);

        this.string = String.format("%s%s%s", p1.name(), p2.name(), p3.name());
        this.span = new ResonanceSite[]{ p1, p2, p3 };
        this.s = (winding1 + winding2) * 0.5f;
        this.l = s < 0 ? 1 : 2;
        this.f = leastCommonMultiple(p1.value(), p2.value(), p3.value());
        this.e = (float)Math.log(f);
        this.m = 1560 / f; // 1560 = least common multiple of [2,3,5,8,13]
        this.g = (float)(0.75 * Math.PI * f * f * f);
    }

    private int leastCommonMultiple(int i, int j, int k) {
        PrimeFactors factorsI = new PrimeFactors(i);
        PrimeFactors factorsJ = new PrimeFactors(j);
        PrimeFactors factorsK = new PrimeFactors(k);
        NavigableSet<Integer> factors = new TreeSet<Integer>();
        factors.addAll(factorsI.getFactors());
        factors.addAll(factorsJ.getFactors());
        factors.addAll(factorsK.getFactors());
        int lcm = 1;
        for (Integer factor : factors) {
            int countI = factorsI.getFactorCount(factor);
            int countJ = factorsJ.getFactorCount(factor);
            int countK = factorsK.getFactorCount(factor);
            int count = Math.max(countI, Math.max(countJ, countK));
            for (int n = 0; n < count; n++) {
                lcm *= factor;
            }
        }
        return lcm;
    }

    public float intersect(PrimordialAttribute pa) {
        return e * s * pa.e * pa.s;
    }

    public float spin() {
        return s;
    }

    public int frequency() {
        return f;
    }

    public float mass() {
        return m;
    }

    public float energy() {
        return e;
    }

    public float gate() {
        return g;
    }

    @Override
    public String toString() {
        return string;
    }

    public static float intersect(Set<Element> elements) {
        if (elements.size() == 2 && elements.contains(PhotaicElement.LIGHT) && elements.contains(PhotaicElement.DARK)) {
            return Float.POSITIVE_INFINITY;
        }
        float e = 1.0f;
        int l = 3;
        for (Element element : elements) {
            e *= element.getPrimordialAttribute().e;
            l &= element.getPrimordialAttribute().l;
        }
        int c = elements.size();
        return l == 0 ? 0 : (float)Math.pow(e, c);
    }

    public static void main(String[] args) {
//        for (Property property : Property.values()) {
//            System.out.printf("%s.e = %.4f", property, property.value);
//        }
    }
 }
