package net.venaglia.realms.common.map.data.binaries;

import java.util.Map;

/**
 * User: ed
 * Date: 4/13/14
 * Time: 7:39 PM
 */
public interface BinaryType {

    Class<?> getJavaType();

    String mimeType();

    Map<String,Object> generateMetadata(byte[] data);

    Map<String,Object> decodeMetadata(String encoded);

    String encodeMetadata(Map<String,Object> metadata);
}
