package net.venaglia.realms.common.chemistry.elements;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 5:44 PM
 */
public enum AstralElement implements Element {

    CONSTITUTION("Con", BaseElement.SOD , 1000.0f),
    PERSEVERENCE("Per", BaseElement.AQUA,  650.0f),
    VERSATILITY ("Ver", BaseElement.ETHO,  350.0f),
    PASSION     ("Pas", BaseElement.PELE,   85.0f),
    COMPASSION  ("Com", BaseElement.ZOT ,   25.0f);

    private final String abbr;
    private final BaseElement baseElement;
    private final float naturalOccurrence;
    private final PrimordialAttribute pa;

    private AstralElement(String abbr, BaseElement baseElement, float naturalOccurrence) {
        this.abbr = abbr;
        this.baseElement = baseElement;
        this.naturalOccurrence = naturalOccurrence;
        ResonanceSite p = baseElement.getProp();
        this.pa = new PrimordialAttribute(p.wind(-1), 1, -2);
    }

    public String getAbbreviation() {
        return abbr;
    }

    public BaseElement getBaseElement() {
        return baseElement;
    }

    public ElementalFamily getElementalFamily() {
        return ElementalFamily.ASTRAL;
    }

    public PrimordialAttribute getPrimordialAttribute() {
        return pa;
    }

    public float getNaturalOccurrence() {
        return naturalOccurrence;
    }
}
