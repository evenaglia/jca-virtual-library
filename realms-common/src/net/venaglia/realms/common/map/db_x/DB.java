package net.venaglia.realms.common.map.db_x;

import net.venaglia.common.util.Tuple2;

/**
 * User: ed
 * Date: 2/23/13
 * Time: 12:16 PM
 */
public interface DB<V> extends KeyValueStore<Integer,V,Tuple2<Integer,V>> {

    enum Mode {
        READONLY, VALUE_MUTABLE, FULLY_MUTABLE;

        public String fileMode() {
            switch (this) {
                case READONLY:
                    return "r";
                case VALUE_MUTABLE:
                case FULLY_MUTABLE:
                    return "rw";
            }
            return null;
        }

        public boolean isReadOnly() {
            return this == READONLY;
        }

        public boolean isAppendable() {
            return this == FULLY_MUTABLE;
        }
    }

    boolean isReadOnly();

    boolean hasUncommittedChanges();

    void commitChanges();
}
