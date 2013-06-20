package net.venaglia.realms.common.chemistry;

/**
 * User: ed
 * Date: 3/26/13
 * Time: 5:38 PM
 */
public enum BaseElement implements Element {

    SOD,  // Earth
    AQUA, // Water
    ETHO, // Air
    PELE, // Fire
    ZOT,  // Plasma
    VOID; // Nothing

    public BaseElement getBaseElement() {
        return this;
    }

    public ElementalFamily getElementalFamily() {
        return ElementalFamily.BASE;
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
                return AstralElement.INSTABILITY;
            case ZOT:
                return AstralElement.COMPASSION;
        }
        return null;
    }
}
