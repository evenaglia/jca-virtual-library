package net.venaglia.realms.common.map.data;

import net.venaglia.common.util.RecycleBuffer;
import net.venaglia.common.util.impl.RecycleBufferImpl;

import java.io.IOException;
import java.io.InputStream;

/**
* User: ed
* Date: 4/11/14
* Time: 5:28 PM
*/
public class ReusableByteStream extends InputStream {

    public static RecycleBuffer<ReusableByteStream> UNUSED = RecycleBufferImpl.forType(ReusableByteStream.class, 1500);

    private boolean closed = true;
    private int position = -1;
    private int available = -1;
    private byte[] data;

    public ReusableByteStream load(byte[] data) {
        this.closed = false;
        this.data = data;
        this.position = 0;
        this.available = data.length;
        return this;
    }

    void recycle() {
        this.closed = true;
        this.data = null;
        this.position = -1;
        this.available = -1;
    }

    public int size() {
        return data.length;
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }
        if (position >= data.length) {
            return -1;
        }
        available--;
        return data[position++];
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }
        if (available <= 0) {
            return -1;
        } else if (b == null) {
               throw new NullPointerException();
           } else if (off < 0 || len < 0 || len > b.length - off) {
               throw new IndexOutOfBoundsException();
           } else if (len == 0) {
               return 0;
           }

        if (len >= available) {
            len = available;
            System.arraycopy(data, position, b, off, len);
            position = data.length;
            available = 0;
            return len;
        } else {
            System.arraycopy(data, position, b, off, len);
            available -= len;
            position += len;
            return len;
        }
    }

    @Override
    public long skip(long len) throws IOException {
        if (len >= available) {
            len = available;
            position = data.length;
            available = 0;
            return len;
        } else {
            available -= len;
            position += len;
            return len;
        }
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }
        return available;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }
        closed = true;
    }

    public static ReusableByteStream get() {
        return UNUSED.get();
    }

    public static void recycle(ReusableByteStream reusableByteStream) {
        UNUSED.recycle(reusableByteStream);
    }
}
