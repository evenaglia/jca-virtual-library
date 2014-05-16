package net.venaglia.common.util;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User: ed
 * Date: 4/30/14
 * Time: 8:59 AM
 */
public class Digest {

    private static final ThreadSingletonSource<MessageDigest> SHA1 =
            new ThreadSingletonSource<MessageDigest>() {
                @Override
                protected MessageDigest newInstance() {
                    try {
                        return MessageDigest.getInstance("SHA-1");
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }
            };


    private Digest() {
        // pure static utility class
    }

    public static String sha1(String data) {
        return sha1(data.getBytes(Charset.forName("UTF-8")));
    }

    public static String sha1(byte[] data) {
        byte[] digest = SHA1.get().digest(data);
        StringBuilder hash = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hash.append("0123456789abcdef".charAt((b >> 4) & 0x0F));
            hash.append("0123456789abcdef".charAt(b & 0x0F));
        }
        return hash.toString();
    }

}
