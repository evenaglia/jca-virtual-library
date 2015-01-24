package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.XForm;

/**
 * User: ed
 * Date: 10/19/14
 * Time: 11:39 PM
 */
public interface Fragment {

    <T> T getCenterXYZ(XForm.View<T> view);

    <T> T getVectorXYZ(XForm.View<T> view);

    double getPressure();

    Color getColor();

    void setColor(Color color);

    Fragment immutableCopy();
}
