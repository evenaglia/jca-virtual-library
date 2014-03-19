package net.venaglia.realms.common.map.elements;

import net.venaglia.realms.common.map.db_x.DatabaseOptions;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Shape;

/**
 * User: ed
 * Date: 2/26/13
 * Time: 5:54 PM
 */
@DatabaseOptions(
        filename = "geo",
        banner = "Detailed geological data for the world"
)
public class DetailAcre extends WorldElement {

    private float[] altitude = new float[96 * 96]; // height map
    private float ruggedness = 1.0f; // 1 = plains; 10 = rolling hills; 100 = mountains

    public Shape<?> getSurface(DetailLevel detailLevel) {
        // todo
        return null;
    }
}
