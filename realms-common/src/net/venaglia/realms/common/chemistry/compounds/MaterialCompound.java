package net.venaglia.realms.common.chemistry.compounds;

import static net.venaglia.realms.common.chemistry.elements.MaterialElement.*;
import static net.venaglia.realms.common.chemistry.elements.MaterialElement.FIRE;
import static net.venaglia.realms.common.chemistry.elements.MaterialElement.PLASMA;

import net.venaglia.realms.common.chemistry.elements.ElementalFamily;
import net.venaglia.realms.common.chemistry.elements.MaterialElement;

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
public class MaterialCompound extends AbstractCompound<MaterialElement> {

    static final MaterialCompound[] PRIMARY_COMPOUNDS = {
            new MaterialCompound(Formula.build(EARTH, WATER ), "Clay" ),
            new MaterialCompound(Formula.build(EARTH, AIR   ), "Sand" ),
            new MaterialCompound(Formula.build(EARTH, FIRE  ), "Stone"),
            new MaterialCompound(Formula.build(EARTH, PLASMA), "Iron" ),
            new MaterialCompound(Formula.build(WATER, AIR   ), "Water"),
            new MaterialCompound(Formula.build(WATER, FIRE  ), "Acid" ),
            new MaterialCompound(Formula.build(WATER, PLASMA), "Oil"  ),
            new MaterialCompound(Formula.build(AIR  , FIRE  ), "Ash"  ),
            new MaterialCompound(Formula.build(AIR  , PLASMA), "Fog"  ),
            new MaterialCompound(Formula.build(FIRE , PLASMA), "Arc"  ),
            new MaterialCompound(Formula.build(EARTH, WATER, AIR   ), "Crystal" ),
            new MaterialCompound(Formula.build(EARTH, WATER, FIRE  ), "Flint"   ),
            new MaterialCompound(Formula.build(EARTH, WATER, PLASMA), "Aluminum"),
            new MaterialCompound(Formula.build(WATER, AIR  , FIRE  ), "Sodium"  ),
            new MaterialCompound(Formula.build(WATER, AIR  , PLASMA), "Grease"  ),
            new MaterialCompound(Formula.build(AIR  , FIRE , PLASMA), "Neon"    )
    };

    MaterialCompound(Formula<MaterialElement> formula, String name) {
        super(formula, name);
    }

    public ElementalFamily getFamily() {
        return ElementalFamily.MATERIAL;
    }

}
