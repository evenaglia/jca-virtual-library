package net.venaglia.realms.common.map.db_x.impl.writer;

/**
 * User: ed
 * Date: 8/21/13
 * Time: 6:22 PM
 */
public interface IndexWriter {

    void queue(Runnable work, String identifier);

    boolean cancel(String identifier);
}
