package net.venaglia.realms.builder.terraform;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.util.ColorGradient;
import net.venaglia.realms.common.map.world.ref.AcreLookup;
import net.venaglia.realms.builder.view.AcreView;

/**
* User: ed
* Date: 1/31/15
* Time: 4:04 PM
*/
class ElevationAcreView extends AcreView {

    protected final AcreLookup acreLookup;

    protected boolean st = true;
    protected ColorGradient gradient = new ColorGradient(new Color(0.0f, 0.0f, 0.25f), new Color(1.0f, 1.0f, 1.0f))
            .addStop(0.10f, new Color(0.0f, 0.0f, 0.4f))
            .addStop(0.35f, new Color(0.0f, 0.2f, 1.0f))
            .addStop(0.49f, new Color(0.6f, 0.9f, 1.0f))
            .addStop(0.50f, new Color(1.0f, 0.9f, 0.8f))
            .addStop(0.51f, new Color(0.0f, 0.8f, 0.1f))
            .addStop(0.63f, new Color(0.4f, 0.1f, 0.0f))
            .addStop(0.75f, new Color(1.0f, 1.0f, 1.0f)).highPerformance();

    public ElevationAcreView(String name, AcreViewGeometry geometry, AcreLookup acreLookup) {
        super(geometry, name);
        this.acreLookup = acreLookup;
    }

    @Override
    public boolean isStatic() {
        return st;
    }

    void setStatic(boolean st) {
        this.st = st;
    }

    @Override
    protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
        float elevation = acreLookup.get(acreId).getElevation();
        float v = Math.max(0.0f, Math.min(1.0f, elevation * 0.5f + 0.5f));
        gradient.applyColor(v, acreRenderer);
    }
}
