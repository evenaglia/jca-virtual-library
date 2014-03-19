package net.venaglia.realms.common.map.db_x;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 5:55 PM
 */
public interface Index extends KeyValueStore<Integer,IndexEntry,IndexEntry> {

    IndexEntry add (Integer id, long offset, int length);

    IndexEntry update (IndexEntry entry, long offset, int length);

    boolean isReadOnly();

    boolean hasUncommittedChanges();

    void commitChanges();
}
