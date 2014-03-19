package net.venaglia.realms.common.map.db_x.impl;

import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.db_x.Index;
import net.venaglia.realms.common.map.db_x.IndexEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 10:12 PM
 */
public abstract class AbstractDiskIndex implements Index {

    public static final int RECORD_SIZE = 16; // bytes

    protected final String banner;
    protected final int offset;
    protected final File file;
    protected final boolean writable;

    protected RandomAccessFile raf;
    protected FileChannel channel;
    protected int size;

    protected AbstractDiskIndex(String banner, String name, boolean writable) {
        byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
        this.banner = banner;
        this.offset = align(bannerBytes.length + 1);
        file = buildFile(name);
        this.writable = writable;
    }

    public void open() throws IOException {
        if (!this.file.exists()) {
            FileOutputStream out = new FileOutputStream(this.file);
            try {
                byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
                byte[] b = new byte[this.offset];
                Arrays.fill(b, (byte)0);
                System.arraycopy(bannerBytes, 0, b, 0, bannerBytes.length);
                b[bannerBytes.length] = 26;
                out.write(b);
            } finally {
                out.close();
            }
        }
        raf = new RandomAccessFile(file, writable ? "rw" : "r");
        channel = raf.getChannel();
        this.size = (int)((file.length() - offset) / RECORD_SIZE);
    }

    public void close() throws IOException {
        raf.close();
        channel.close();
    }

    private int align(int length) {
        int slack = length % RECORD_SIZE;
        return slack == 0 ? length : length + (RECORD_SIZE - slack);
    }

    public boolean contains(Integer key) {
        return get(key) != null;
    }

    public void put(Integer key, IndexEntry value) {
        throw new UnsupportedOperationException();
    }

    protected abstract IndexEntry readEntry(int i);

    protected static File buildFile(String name) {
        File dir = new File("db", Configuration.DATABASE_DIRECTORY.getString());
        return new File(dir, name + ".index");
    }

    public boolean isReadOnly() {
        return !writable;
    }
}
