package net.venaglia.realms.common.map_x.db.impl.spatial;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 8/2/13
* Time: 9:07 PM
* To change this template use File | Settings | File Templates.
*/
enum Octant {
    LO_X__LO_Y__LO_Z,
    LO_X__LO_Y__HI_Z,
    LO_X__HI_Y__LO_Z,
    LO_X__HI_Y__HI_Z,
    HI_X__LO_Y__LO_Z,
    HI_X__LO_Y__HI_Z,
    HI_X__HI_Y__LO_Z,
    HI_X__HI_Y__HI_Z
}
