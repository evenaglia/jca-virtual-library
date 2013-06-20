package net.venaglia.gloo.view;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * User: ed
 * Date: 5/27/13
 * Time: 4:47 PM
 */
public class NativeBinariesInstaller {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    private void prepareNativeLibs() throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File tempDir = File.createTempFile("native-libs","").getCanonicalFile();
        System.out.println("Using temp dir for native libs: " + tempDir);
        tempDir.deleteOnExit();
        if (!(tempDir.delete() && tempDir.mkdir())) { // convert from file to directory
            throw new RuntimeException("Unable to create a temp dir for storing native library files");
        }

        System.setProperty("org.lwjgl.librarypath", tempDir.getCanonicalPath());
        byte[] buffer = new byte[4096];
        String libArchive = getClass().getPackage().getName().replace('.', '/') + "/native/binary.data";
        ZipInputStream zip = new ZipInputStream(classLoader.getResourceAsStream(libArchive));
        ZipEntry entry = zip.getNextEntry();
        Pattern fileMatch;
        String osName = System.getProperty("os.name", "");
        if (osName.contains("Mac")) {
            fileMatch = Pattern.compile("\\bmacosx/");
        } else if (osName.contains("Win")) {
            fileMatch = Pattern.compile("\\bwindows/");
        } else if (osName.contains("Lin") || osName.contains("nix")) {
            fileMatch = Pattern.compile("\\blinux/");
        } else {
            fileMatch = Pattern.compile("\\bsolaris/");
        }
        while (entry != null) {
            String filename = entry.getName();
            if (filename.endsWith("/") || !fileMatch.matcher(filename).find()) {
                entry = zip.getNextEntry();
                continue;
            }
            filename = filename.substring(filename.lastIndexOf('/') + 1);
            File outFile = new File(tempDir, filename);
            System.out.println("writing native lib file at: " + outFile);
            entry = zip.getNextEntry();
            outFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(outFile);
            try {
                copyLarge(zip, out, buffer);
            } finally {
                closeQuietly(out);
            }
        }
        closeQuietly(zip);
    }

    private static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // don't care
        }
    }

    public void install() {
        if (!INSTALLED.getAndSet(true)) {
            try {
                prepareNativeLibs();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void installIfRunningFromJar() {
        URL myClassFile = NativeBinariesInstaller.class.getResource(NativeBinariesInstaller.class.getSimpleName() + ".class");
        if (String.valueOf(myClassFile).startsWith("jar:")) {
            install();
        }
    }
}
