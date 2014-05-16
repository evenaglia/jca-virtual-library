package net.venaglia.realms.common.map;

/**
 * User: ed
 * Date: 5/15/14
 * Time: 5:23 PM
 */
public interface PropertyStore {

    String get(String name);

    void set(String name, String value);

    void remove(String name);

}
