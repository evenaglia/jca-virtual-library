package com.jivesoftware.jcalibrary.util;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

/**
 * User: ed
 * Date: 6/3/13
 * Time: 8:29 AM
 *
 * Similar functionality as found in Apache Commons IO, but without the rest of the dependencies.
 */
public class IOUtils {

    public static String toString(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder(256);
        InputStreamReader reader = new InputStreamReader(in);
        char[] b = new char[256];
        for (int i = reader.read(b); i >= 0; i = reader.read(b)) {
            buffer.append(b, 0, i);
        }
        return buffer.toString();
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // don't care
        }
    }

    public static void copy(InputStream in, FileOutputStream out) throws IOException {
        copyLarge(in, out, new byte[256]);
    }

    public static void copyLarge(InputStream in, FileOutputStream out, byte[] buffer) throws IOException {
        if (buffer == null) {
            buffer = new byte[1024];
        }
        for (int i = in.read(buffer); i >= 0; i = in.read(buffer)) {
            out.write(buffer, 0, i);
        }
    }

    public static void copy(Reader in, Writer out) throws IOException {
        copyLarge(in, out, new char[256]);
    }

    public static void copyLarge(Reader in, Writer out, char[] buffer) throws IOException {
        if (buffer == null) {
            buffer = new char[1024];
        }
        for (int i = in.read(buffer); i >= 0; i = in.read(buffer)) {
            out.write(buffer, 0, i);
        }
    }
}
