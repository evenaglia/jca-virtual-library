package net.venaglia.realms.common.map.things;

/**
* User: ed
* Date: 3/18/14
* Time: 9:14 PM
*/
public enum ThingStatus {

    /** Newly created */
    NEW,

    /** No changes since last i/o */
    CLEAN,

    /** Has changed since last i/o */
    DIRTY,

    /** Has been removed since last i/o */
    GHOST,

    /** Deleted, persisted */
    DELETED,

    /** Not loaded yet */
    NOT_LOADED
}
