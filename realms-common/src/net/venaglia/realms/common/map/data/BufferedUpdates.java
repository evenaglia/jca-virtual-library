package net.venaglia.realms.common.map.data;

import net.venaglia.gloo.physical.geom.Point;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;

/**
 * User: ed
 * Date: 4/9/14
 * Time: 8:00 PM
 */
public class BufferedUpdates {

    private static final int UPDATING_POSITION_FLAG   = 1;
    private static final int UPDATING_CUBE_ID_FLAG    = 2;
    private static final int UPDATING_PROPERTIES_FLAG = 4;
    private static final int INSERT_INDEX = 8;
    private static final int DELETE_INDEX = 9;

    public enum UpdatedFields {
        DeleteThing(INSERT_INDEX),
        InsertThing(DELETE_INDEX),
        UpdatePosition(UPDATING_POSITION_FLAG),
        UpdatePositionAndCube(UPDATING_POSITION_FLAG | UPDATING_CUBE_ID_FLAG),
        UpdateProperties(UPDATING_PROPERTIES_FLAG),
        UpdatePropertiesAndPosition(UPDATING_PROPERTIES_FLAG | UPDATING_POSITION_FLAG),
        UpdatePropertiesPositionAndCube(UPDATING_PROPERTIES_FLAG | UPDATING_POSITION_FLAG | UPDATING_CUBE_ID_FLAG);

        private final int flags;
        private final boolean update;

        UpdatedFields(int flags) {
            this.flags = flags;
            this.update = flags < INSERT_INDEX;
        }

        public int getFlags() {
            return flags;
        }

        public boolean isDelete() {
            return this == DeleteThing;
        }

        public boolean isInsert() {
            return this == InsertThing;
        }

        public boolean updatePosition() {
            return update ? (flags & UPDATING_POSITION_FLAG) != 0 : flags == INSERT_INDEX;
        }

        public boolean updateCubeId() {
            return update ? (flags & UPDATING_CUBE_ID_FLAG) != 0 : flags == INSERT_INDEX;
        }

        public boolean updateProperties() {
            return update ? (flags & UPDATING_PROPERTIES_FLAG) != 0 : flags == INSERT_INDEX;
        }
    }

    private final Queue<Delta> unused = new ArrayDeque<Delta>(1536);
    @SuppressWarnings("unchecked")
    private final Collection<Delta>[] buffer = (Collection<Delta>[])new Collection[]{
            null,                       // delete
            new ArrayList<Delta>(1536), // position
            null,                       // cube
            new ArrayList<Delta>(1536), // cube + position
            new ArrayList<Delta>(1536), // props
            new ArrayList<Delta>(1536), // props + position
            null,                       // props + cube
            new ArrayList<Delta>(1536), // props + cube + position
            new ArrayList<Delta>(256),  // insert
            new ArrayList<Delta>(256)   // delete
    };
    @SuppressWarnings("unchecked")
    private final Collection<Delta>[] immutableBuffer = (Collection<Delta>[])new Collection[buffer.length];

    public BufferedUpdates() {
        for (int i = 0; i < buffer.length; i++) {
            Collection<Delta> deltas = buffer[i];
            if (deltas != null) {
                immutableBuffer[i] = Collections.unmodifiableCollection(deltas);
            }
        }
    }

    public void add(Long id, Point position, Long cubeId, byte[] properties) {
        if (id == null) throw new NullPointerException("id");
        if (position == null) throw new NullPointerException("position");
        if (cubeId == null) throw new NullPointerException("cubeId");
        if (properties == null) throw new NullPointerException("properties");
        queueImpl(id, position, cubeId, properties, INSERT_INDEX);
    }

    public void remove(Long id) {
        if (id == null) throw new NullPointerException("id");
        queueImpl(id, null, null, null, DELETE_INDEX);
    }

    public void update(Long id, Point position, Long cubeId, byte[] properties) {
        if (id == null) throw new NullPointerException("id");
        queueImpl(id, position, cubeId, properties, -1);
    }

    private void queueImpl(Long id, Point position, Long cubeId, byte[] properties, int index) {
        Delta delta = getEmptyDelta().load(id, position, cubeId, properties);
        if (index == -1) index = delta.flags;
        Collection<Delta> deltas = buffer[index];
        if (deltas == null) {
            putEmptyDelta(delta);
            throw new IllegalArgumentException();
        }
        deltas.add(delta);
    }

    public boolean has(UpdatedFields fields) {
        return !buffer[fields.getFlags()].isEmpty();
    }

    public Collection<Delta> subset(UpdatedFields fields) {
        return immutableBuffer[fields.getFlags()];
    }

    public void clear() {
        for (Collection<Delta> deltas : buffer) {
            if (deltas != null) {
                for (Delta delta : deltas) {
                    delta.recycle();
                }
                unused.addAll(deltas);
                deltas.clear();
            }
        }
    }

    private Delta getEmptyDelta() {
        Delta delta = unused.poll();
        if (delta == null) {
            delta = new Delta();
        }
        return delta;
    }

    private void putEmptyDelta(Delta delta) {
        delta.recycle();
        unused.add(delta);
    }

    public static class Delta {

        private static final InputStream NULL_INPUT_STREAM = new InputStream() {
            @Override
            public int read() throws IOException {
                return -1;
            }
        };

        private final ReusableByteStream inputStream = new ReusableByteStream();

        private Long id;
        private Point position;
        private Long cubeId;
        private byte[] properties;
        private int flags;

        Delta load(Long id, Point position, Long cubeId, byte[] properties) {
            if (properties != null) {
                inputStream.load(properties);
            }
            this.id = id;
            this.position = position;
            this.cubeId = cubeId;
            this.properties = properties;
            this.flags = (position != null ? UPDATING_POSITION_FLAG : 0) |
                         (cubeId != null ? UPDATING_CUBE_ID_FLAG : 0) |
                         (properties != null ? UPDATING_PROPERTIES_FLAG : 0);
            return this;
        }

        void recycle() {
            inputStream.recycle();
            this.id = null;
            this.position = null;
            this.cubeId = null;
            this.properties = null;
            this.flags = 0;
        }

        public Long getId() {
            return id;
        }

        public Point getPosition() {
            return position;
        }

        public Long getCubeId() {
            return cubeId;
        }

        public int getPropertiesLength() {
            return properties == null ? 0 : properties.length;
        }

        public InputStream getPropertiesStream() {
            return properties == null ? NULL_INPUT_STREAM : inputStream;
        }

        public byte[] getProperties() {
            return properties;
        }
    }

}
