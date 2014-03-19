package net.venaglia.realms.common.map_x.db;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 11:31 PM
 */
public interface IdProvider<T> {

    int getId(T value);
}
