package net.venaglia.gloo.physical.geom.detail;

import net.venaglia.gloo.physical.geom.Point;

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
            put(0.125 * Math.PI / 180.0, null);
            put(8 * Math.PI / 180.0, DetailLevel.LOW);
            put(12 * Math.PI / 180.0, DetailLevel.MEDIUM_LOW);
            put(22 * Math.PI / 180.0, DetailLevel.MEDIUM);
            put(40 * Math.PI / 180.0, DetailLevel.MEDIUM_HIGH);
            put(360 * Math.PI / 180.0, DetailLevel.HIGH);
        }
    };

    DetailLevel computeDetail(Point observer, double longestDimension);
}
