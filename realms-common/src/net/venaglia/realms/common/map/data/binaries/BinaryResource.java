package net.venaglia.realms.common.map.data.binaries;

import net.venaglia.common.util.Digest;
import net.venaglia.common.util.Identifiable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 4/11/14
 * Time: 5:05 PM
 */
public class BinaryResource implements Identifiable {

    private Long id = null;
    private BinaryType type;
    private Map<String,Object> mutableMetadata = new HashMap<String,Object>();
    private Map<String,Object> immutableMetadata = Collections.unmodifiableMap(mutableMetadata);
    private String sha1Hash;
    private byte[] data;

    public void init(Long id,
                     BinaryType type,
                     Map<String,Object> metadata,
                     String sha1Hash,
                     byte[] data) {
        if (this.data != null) {
            throw new IllegalStateException("BinaryResource is already loaded");
        }
        if (type == null) throw new NullPointerException("type");
        if (metadata == null) throw new NullPointerException("metadata");
        if (data == null) throw new NullPointerException("data");
        this.id = id;
        this.type = type;
        this.mutableMetadata.putAll(metadata);
        this.sha1Hash = sha1Hash;
        this.data = data;
    }

    public void recycle() {
        if (this.data != null) {
            this.id = null;
            this.type = null;
            this.mutableMetadata.clear();
            this.sha1Hash = null;
            this.data = null;
        }
    }

    public Long getId() {
        return id;
    }

    public BinaryType getType() {
        return type;
    }

    public Map<String,Object> getMetadata() {
        return immutableMetadata;
    }

    public int getLength() {
        return data.length;
    }

    public String getSha1Hash() {
        if (sha1Hash == null) {
            sha1Hash = Digest.sha1(data);
        }
        return sha1Hash;
    }

    public byte[] getData() {
        return data;
    }
}
