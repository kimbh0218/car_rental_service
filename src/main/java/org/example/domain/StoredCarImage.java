package org.example.domain;

public class StoredCarImage {
    private final String storageKey;
    private final String originalFileName;
    private final String contentType;
    private final long sizeBytes;

    public StoredCarImage(
            String storageKey,
            String originalFileName,
            String contentType,
            long sizeBytes
    ) {
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}
