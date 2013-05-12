package net.venaglia.realms.common.physical.geom.detail;

import net.venaglia.realms.common.physical.geom.Point;

import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * User: ed
 * Date: 5/11/13
 * Time: 8:08 PM
 */
public interface DetailComputer {

    NavigableMap<Double,DetailLevel> DETAIL_LEVELS_BY_VISIBLE_ANGLE = new TreeMap<Double,DetailLevel>() {
        {
            put(Math.tan(0.125 * Math.PI / 180.0), null);
            put(Math.tan(3 * Math.PI / 180.0), DetailLevel.LOW);
            put(Math.tan(6 * Math.PI / 180.0), DetailLevel.MEDIUM_LOW);
            put(Math.tan(12 * Math.PI / 180.0), DetailLevel.MEDIUM);
            put(Math.tan(24 * Math.PI / 180.0), DetailLevel.MEDIUM_HIGH);
            put(Double.POSITIVE_INFINITY, DetailLevel.HIGH);
        }
    };

    DetailLevel computeDetail(Point observer, double longestDimension);
}
