package net.venaglia.realms.common.chemistry.compounds;

import net.venaglia.realms.common.chemistry.elements.Element;
import net.venaglia.realms.common.chemistry.elements.ElementalFamily;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 8:05 PM
 *
 * Atoms bond on one of the five resonant points. Higher frequency points bond
 * with higher energy, but bonds always prefer the lowest possible energy bond.
 * Atoms can only bond at matching resonant sites.
 */
public interface Compound<E extends Element> {

    String getName(); // may be null

    ElementalFamily getFamily(); // Material or Astral

    Formula<E> getFormula();

    int getMass();

    float getPotential();

    /**
     * Half-life, in years
     */
    float getStability();

    /**
     * 100 = air, water, etc
     * 10 = stone, wood
     * 1 = pyrite, amethyst, silver
     * 0.1 = diamond, topaz, gold
     * 0.01 = platinum, emerald
     */
    float getNaturalOccurence();
}
