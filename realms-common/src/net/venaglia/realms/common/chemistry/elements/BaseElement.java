package net.venaglia.realms.common.chemistry.elements;

import static net.venaglia.realms.common.chemistry.elements.ResonanceSite.*;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 5:38 PM
 *
 * Base elements represent elements before the cataclysm of the heavens. They no longer exist.
 */
public enum BaseElement implements Element {

    SOD ("Sd", A),   // Earth
    AQUA("Aq", B),   // Water
    ETHO("Et", C),   // Air
    PELE("Py", D),   // Fire
    ZOT ("Pz", E),   // Plasma
    VOID("V", null); // Nothing

    private final String abbr;
    private final PrimordialAttribute pa;
    private final ResonanceSite prop;

    private BaseElement(String abbr, ResonanceSite prop) {
        this.abbr = abbr;
        this.prop = prop;
        this.pa = prop == null ? new PrimordialAttribute(0.0f) : new PrimordialAttribute(prop);
    }

    public String getAbbreviation() {
        return abbr;
    }

    public BaseElement getBaseElement() {
        return this;
    }

    public ElementalFamily getElementalFamily() {
        return this == VOID ? ElementalFamily.VOID : ElementalFamily.BASE;
    }

    public float getPotential(Element other) {
        return pa.energy();
    }

    ResonanceSite getProp() {
        return prop;
    }

    public PrimordialAttribute getPrimordialAttribute() {
        return pa;
    }

    public float getNaturalOccurrence() {
        return 0.0f;
    }

    public MaterialElement getPhysicalElement() {
        switch (this) {
            case SOD:
                return MaterialElement.EARTH;
            case AQUA:
                return MaterialElement.WATER;
            case ETHO:
                return MaterialElement.AIR;
            case PELE:
                return MaterialElement.FIRE;
            case ZOT:
                return MaterialElement.PLASMA;
        }
        return null;
    }

    public AstralElement getAstralElement() {
        switch (this) {
            case SOD:
                return AstralElement.CONSTITUTION;
            case AQUA:
                return AstralElement.PERSEVERENCE;
            case ETHO:
                return AstralElement.VERSATILITY;
            case PELE:
                return AstralElement.PASSION;
            case ZOT:
                return AstralElement.COMPASSION;
        }
        return null;
    }
}
