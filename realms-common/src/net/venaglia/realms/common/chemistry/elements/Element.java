package net.venaglia.realms.common.chemistry.elements;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 7:52 PM
 */
public interface Element {

    String getAbbreviation();

    BaseElement getBaseElement();

    ElementalFamily getElementalFamily();

    PrimordialAttribute getPrimordialAttribute();

    /**
     * 100 = air, water, etc
     * 10 = stone, wood
     * 1 = pyrite, amethyst, silver
     * 0.1 = diamond, topaz, gold
     * 0.01 = platinum, emerald
     */
    float getNaturalOccurrence();
}
