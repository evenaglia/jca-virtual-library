package net.venaglia.realms.common.map.things;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.realms.common.map.Cube;

/**
* User: ed
* Date: 3/18/14
* Time: 9:14 PM
*/
public interface ThingWriter {

    void unchanged(Point position, Cube cube, ThingProperties properties);

    void updatePosition(Point position, Cube cube);
    void updateThing(ThingProperties properties);
    void addThing(Point position, Cube cube, ThingProperties properties);
    void deleteThing();
}
