package com.jivesoftware.jcalibrary.api.rest.manager;

import com.jivesoftware.jcalibrary.LibraryProps;
import com.jivesoftware.jcalibrary.scheduler.WorkScheduler;
import com.jivesoftware.jcalibrary.util.IOUtils;
import net.venaglia.common.util.Pair;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.impl.SimpleRef;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

/**
 * RestClientManager
 */
public class RestClientManager {

    public static final RestClientManager INSTANCE = new RestClientManager("rest.cache", true);

    private final File cacheFile;
    private final Map<String,Response> cache;
    private final Map<String,Response> newEntries = new HashMap<String,Response>();
    private final byte[] buffer = new byte[16777216];

    private RestClientManager(String filename, boolean scheduleWriteTask) {
        cacheFile = new File(filename);
        cache = loadCache();
        if (scheduleWriteTask) {
            WorkScheduler.interval(new Runnable() {
                public void run() {
                    updateCacheFile();
                }
            }, 15, TimeUnit.SECONDS);
        }
    }

    private Map<String,Response> loadCache() {
        ConcurrentSkipListMap<String,Response> cache = new ConcurrentSkipListMap<String, Response>();
        if (!cacheFile.exists()) {
            return cache;
        }
        int bytes = 0;
        try {
            RandomAccessFile raf = new RandomAccessFile(cacheFile, "r");
            MappedByteBuffer buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, cacheFile.length());
            buffer.order(ByteOrder.nativeOrder());
            while (buffer.hasRemaining()) {
                String key = readString(buffer);
                int status = buffer.getInt();
                int bodyOffset = buffer.position();
                int bodyLength = skipString(buffer) + 4;
                cache.put(key, new Response(status, new BodyRef(bodyOffset, bodyLength)));
            }
            bytes = buffer.position();
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Loaded rest cache: %d entries, %.1f KB\n", cache.size(), bytes / 1000.0);
        return cache;
    }

    private void updateCacheFile() {
        Map<String,Response> toWrite;
        synchronized (newEntries) {
            toWrite = new HashMap<String,Response>(newEntries);
            newEntries.clear();
        }
        if (!toWrite.isEmpty()) {
            append(toWrite);
        }
    }

    private synchronized void append(Map<String,Response> toWrite) {
        int bytes = 0;
        try {
            int base = (int)cacheFile.length();
            ByteBuffer buffer = ByteBuffer.wrap(this.buffer);
            buffer.order(ByteOrder.nativeOrder());
            buffer.clear();
            for (Map.Entry<String,Response> entry : toWrite.entrySet()) {
                writeString(buffer, entry.getKey());
                Response response = entry.getValue();
                buffer.putInt(response.getStatus());
                int offset = buffer.position();
                writeString(buffer, response.getBody());
                int length = buffer.position() - offset;
                BodyRef body = new BodyRef(base + offset, length);
                entry.setValue(new Response(response.getStatus(), body));
            }
            bytes = buffer.position();

            FileOutputStream out = new FileOutputStream(cacheFile, true);
            out.write(this.buffer, 0, bytes);
            out.close();
            cache.putAll(toWrite);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Appended rest cache: %d entries, %.1f KB\n", toWrite.size(), bytes / 1000.0);
    }

    private String readString(ByteBuffer buffer) throws UnsupportedEncodingException {
        int len = buffer.getInt();
        if (len == 0) {
            return "";
        } else if (len == -1) {
            return null;
        }
        byte[] data = new byte[len];
        buffer.get(data);
        return new String(data, "UTF-8");
    }

    private int skipString(ByteBuffer buffer) throws UnsupportedEncodingException {
        int len = buffer.getInt();
        if (len <= 0) {
            return len;
        }
        buffer.position(buffer.position() + len);
        return len;
    }

    private void writeString(ByteBuffer buffer, String string) throws UnsupportedEncodingException {
        if (string == null) {
            buffer.putInt(-1);
        } else if (string.length() == 0) {
            buffer.putInt(0);
        } else {
            byte[] data = string.getBytes("UTF-8");
            buffer.putInt(data.length);
            buffer.put(data);
        }
    }

    private WebClient getWebClient() {
        LibraryProps libraryProps = LibraryProps.INSTANCE;
        Pair<String,String> jcaCredentials = libraryProps.getJCACredentials();

        String serverUrl = LibraryProps.INSTANCE.getProperty(LibraryProps.JCA_URL);
        String user = jcaCredentials.getA();
        String password = jcaCredentials.getB();
        return WebClient.create(serverUrl, user, password, null);
    }

    public Response get(String endpoint) {
        return get(endpoint, null, null);
    }

    public Response get(String endpoint, String queryName, String queryValue) {
        String key = getKey(endpoint, queryName, queryValue);
        Response response = cache.get(key);
        if (response == null) {
            response = getImpl(endpoint, queryName, queryValue);
            addCacheEntry(key, response);
        }
        return response;
    }

    private void addCacheEntry(String key, Response response) {
        cache.put(key, response);
        synchronized (newEntries) {
            newEntries.put(key, response);
        }
    }

    private String getKey(String endpoint, String queryName, String queryValue) {
        if (null != queryName && null != queryValue) {
            try {
                if (endpoint.contains("?")) {
                    return endpoint + "&" + URLEncoder.encode(queryName, "ISO-8859-1") + "=" + URLEncoder.encode(queryValue, "ISO-8859-1");
                } else {
                    return endpoint + "?" + URLEncoder.encode(queryName, "ISO-8859-1") + "=" + URLEncoder.encode(queryValue, "ISO-8859-1");
                }
            } catch (UnsupportedEncodingException e) {
                // don't care
            }
        }
        return endpoint;
    }

    public Response getImpl(String endpoint, String queryName, String queryValue) {
        // timeout default is 30 seconds
        WebClient webClient = getWebClient();
        HTTPConduit conduit = WebClient.getConfig(webClient).getHttpConduit();
        conduit.getClient().setReceiveTimeout(300000);
        conduit.getClient().setConnectionTimeout(300000);
        webClient.path(endpoint);
        if (null != queryName && null != queryValue) {
            webClient.query(queryName, queryValue);
        }
        webClient.accept(MediaType.APPLICATION_JSON);
        webClient.type(MediaType.APPLICATION_JSON);
        javax.ws.rs.core.Response response = webClient.get();
        return new Response(response);
    }

    public Response post(String endpoint, String jsonRepresentation) {
         // timeout default is 30 seconds
        WebClient webClient = getWebClient();
        webClient.path(endpoint);
        webClient.accept(MediaType.APPLICATION_JSON);
        webClient.type(MediaType.APPLICATION_JSON);
        javax.ws.rs.core.Response response = webClient.post(jsonRepresentation);
        return new Response(response);
    }

    private class BodyRef implements Ref<String> {

        private final int bodyOffset;
        private final int bodyLength;

        public BodyRef(int bodyOffset, int bodyLength) {
            this.bodyOffset = bodyOffset;
            this.bodyLength = bodyLength;
        }

        public String get() {
            FileChannel channel = null;
            try {
                RandomAccessFile raf = new RandomAccessFile(cacheFile, "r");
                channel = raf.getChannel();
                ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, bodyOffset, bodyLength);
                buffer.order(ByteOrder.nativeOrder());
                int checkLength = buffer.getInt();
                if (checkLength != bodyLength - 4) {
                    String segment = String.format("%08x | %s", bodyLength, toHexString(buffer, 0, Math.min(16, bodyLength)));
                    throw new RuntimeException(String.format("The length in cache was unexpected: expected=%1$d[%1$08x], file=%2$d[%2$08x] -- %3$s...",
                                                             bodyLength,
                                                             checkLength,
                                                             segment));
                }
                byte[] data = new byte[checkLength];
                buffer.get(data);
                return new String(data, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(channel);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        File testCache = new File("test.cache");
        if (testCache.exists()) {
            assert testCache.delete();
        }
        assert testCache.createNewFile();
        testCache.deleteOnExit();
        Random rnd = new Random();
        Map<String,Response> responses = new HashMap<String,Response>();
        Map<String,Response> tmp;

        RestClientManager rcm = new RestClientManager(testCache.getName(), false);
        assert rcm.cache.isEmpty();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                for (Map.Entry<String,Response> entry : randomResponses(rnd, 5).entrySet()) {
                    responses.put(entry.getKey(), entry.getValue());
                    rcm.addCacheEntry(entry.getKey(), entry.getValue());
                }
                rcm.updateCacheFile();
                assertEquals(responses, rcm.cache);
            }

            rcm = new RestClientManager(testCache.getName(), false);
            assertEquals(responses, rcm.cache);
        }
    }

    private static Map<String,Response> randomResponses(Random rnd, int len) {
        Map<String,Response> result = new HashMap<String,Response>(len);
        for (int i = 0; i < len; i++) {
//            String body = randomString(rnd, rnd.nextInt(8) + 4);
            String body = randomString(rnd, rnd.nextInt(65536 - 256) + 256);
            int status = rnd.nextInt(4) * 100 + 200 + rnd.nextInt(16);
            Response response = new Response(status, SimpleRef.create(body));
//            String key = randomString(rnd, rnd.nextInt(8) + 4);
            String key = randomString(rnd, rnd.nextInt(24) + 24);
            result.put(key, response);
        }
        return result;
    }

    private static String randomString(Random rnd, int len) {
        char[] text = new char[len];
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < len; i++) {
            text[i] = alphabet.charAt(rnd.nextInt(alphabet.length()));
        }
        return new String(text);
    }

    public static void assertEquals(Map<String,Response> expected, Map<String,Response> actual) {
        assert expected.size() == actual.size();
        for (Map.Entry<String,Response> entry : expected.entrySet()) {
            String key = entry.getKey();
            assert actual.containsKey(key);
            Response ex = entry.getValue();
            Response ac = actual.get(key);
            assert ex.getStatus() == ac.getStatus();
            String exBody = ex.getBody();
            String acBody = ac.getBody();
            assert exBody.equals(acBody);
        }
    }

    public static String toHexString (ByteBuffer buffer, int position, int length) {
        byte[] data = new byte[length];
        int reset = buffer.position();
        buffer.position(position);
        buffer.get(data);
        buffer.position(reset);
        char[] result = new char[length * 2];
        for (int i = 0, j = 0; i < length; i++) {
            int b = data[i] & 0xFF;
            result[j++] = "0123456789abcdef".charAt(b >> 4);
            result[j++] = "0123456789abcdef".charAt(b & 15);
        }
        return new String(result);
    }
}
