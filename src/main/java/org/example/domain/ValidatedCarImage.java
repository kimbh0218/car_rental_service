package org.example.domain;

public class ValidatedCarImage {
    private final String originalFileName;
    private final String contentType;
    private final long sizeBytes;

    public ValidatedCarImage(
            String originalFileName,
            String contentType,
            long sizeBytes
    ) {
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
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
