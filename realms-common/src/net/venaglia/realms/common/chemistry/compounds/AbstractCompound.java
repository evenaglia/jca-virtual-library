package net.venaglia.realms.common.chemistry.compounds;

import net.venaglia.realms.common.chemistry.elements.Element;

/**
 * User: ed
 * Date: 5/22/14
 * Time: 8:08 AM
 *
 * element counts:
 * 2: 10
 * 3: 6
 * 5: 60
 * 8: 360
 * 13: 21600
 */
abstract class AbstractCompound<E extends Element> implements Compound<E> {

    protected final Formula<E> formula;
    protected final String name;
    protected final float e;
    protected final int m;
    protected final float naturalOccurrence;

    AbstractCompound(Formula<E> formula, String name) {
        this.formula = formula;
        this.name = name;
        float e = 0;
        int m = 0;
        float naturalOccurrence = 1.0f;
        for (E atom : formula) {
            e += atom.getPrimordialAttribute().energy();
            m += atom.getPrimordialAttribute().mass();
            naturalOccurrence *= atom.getNaturalOccurrence();
        }
        this.e = e;
        this.m = m;
        int size = formula.size();
        double numerator = size == 2 ? 1.5 : 0.5;
        this.naturalOccurrence = (float)Math.pow(naturalOccurrence, numerator / size);
    }

    public String getName() {
        return name;
    }

    public Formula<E> getFormula() {
        return formula;
    }

    public int getMass() {
        return m;
    }

    public float getPotential() {
        return e;
    }

    public float getStability() {
        return Float.POSITIVE_INFINITY;
    }

    public float getNaturalOccurence() {
        return naturalOccurrence;
    }

}
