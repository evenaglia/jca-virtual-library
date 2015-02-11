package net.venaglia.realms.common.weather;

import net.venaglia.gloo.physical.geom.XForm;

/**
 * User: ed
 * Date: 2/5/15
 * Time: 5:28 PM
 */
public interface AirSample {

    <T> T getCenterXYZ(XForm.View<T> view);

    void setCenterXYZ(double x, double y, double z);

    // delta from previous position, plus any coriolis effect
    <T> T getVectorXYZ(XForm.View<T> view);

    float getTemperature();

    // megatons of water in this cell
    float getMoisture();

    // the temp at which no more water can be held
    float getDewPoint();

    // cloud cover: 0.0 --> 1.0, close to dew point forms clouds
    float getCloudOpacity();

    // some value that represents how windy it is here, derived by aggregating the deltas of all variables
    float getTurbulance();

    // approx radius, in meters
    float getSize();

    // adds moisture and heat, internal update must factor in loss of heat into space
    void update(float addMoisture, float addHeat);
}
