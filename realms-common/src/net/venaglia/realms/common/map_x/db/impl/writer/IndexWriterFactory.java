package net.venaglia.realms.common.map_x.db.impl.writer;

import java.util.UUID;

/**
 * User: ed
 * Date: 8/26/13
 * Time: 8:35 AM
 */
public class IndexWriterFactory {

    private IndexWriterFactory() {
        // pure static class
    }

    public static IndexWriter buildWriter() {
        return new IndexWriter() {

            private final String id = UUID.randomUUID() + ":";

            public void queue(Runnable work, String identifier) {
                AsyncIndexWriter.INSTANCE.queue(id + identifier, work);
            }

            public boolean cancel(String identifier) {
                return AsyncIndexWriter.INSTANCE.cancel(identifier);
            }
        };
    }
}
