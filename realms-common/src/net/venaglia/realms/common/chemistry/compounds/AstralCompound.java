package net.venaglia.realms.common.chemistry.compounds;

import static net.venaglia.realms.common.chemistry.elements.AstralElement.*;

import net.venaglia.realms.common.chemistry.elements.AstralElement;
import net.venaglia.realms.common.chemistry.elements.ElementalFamily;

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
public class AstralCompound extends AbstractCompound<AstralElement> {

    static final AstralCompound[] PRIMARY_COMPOUNDS = {
            new AstralCompound(Formula.build(CONSTITUTION, PERSEVERENCE ), "Stamina"      ),
            new AstralCompound(Formula.build(CONSTITUTION, VERSATILITY  ), "Toughness"    ),
            new AstralCompound(Formula.build(CONSTITUTION, PASSION      ), "Fortitude"    ),
            new AstralCompound(Formula.build(CONSTITUTION, COMPASSION   ), "Empathy"      ),
            new AstralCompound(Formula.build(PERSEVERENCE, VERSATILITY  ), "Resilience"   ),
            new AstralCompound(Formula.build(PERSEVERENCE, PASSION      ), "Determination"),
            new AstralCompound(Formula.build(PERSEVERENCE, COMPASSION   ), "Charity"      ),
            new AstralCompound(Formula.build(VERSATILITY , PASSION      ), "Zeal"         ),
            new AstralCompound(Formula.build(VERSATILITY , COMPASSION   ), "Tenderness"   ),
            new AstralCompound(Formula.build(PASSION     , COMPASSION   ), "Selflessness" ),
            new AstralCompound(Formula.build(CONSTITUTION, PERSEVERENCE, VERSATILITY  ), "Diligence"  ),
            new AstralCompound(Formula.build(CONSTITUTION, PERSEVERENCE, PASSION      ), "Leadership" ),
            new AstralCompound(Formula.build(CONSTITUTION, PERSEVERENCE, COMPASSION   ), "Camaraderie"),
            new AstralCompound(Formula.build(PERSEVERENCE, VERSATILITY , PASSION      ), "Vision"     ),
            new AstralCompound(Formula.build(PERSEVERENCE, VERSATILITY , COMPASSION   ), "Benevolence"),
            new AstralCompound(Formula.build(VERSATILITY , PASSION     , COMPASSION   ), "Love"       )
    };

    AstralCompound(Formula<AstralElement> formula, String name) {
        super(formula, name);
    }

    public ElementalFamily getFamily() {
        return ElementalFamily.ASTRAL;
    }

}
