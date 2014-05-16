package net.venaglia.realms.common.map;

/**
 * User: ed
 * Date: 3/23/14
 * Time: 10:22 PM
 */
public interface UniqueIdSource {

    String getName();

    long next();
}
