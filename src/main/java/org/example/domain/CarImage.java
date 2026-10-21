package org.example.domain;

public class CarImage {
    private final long id;
    private final long carId;
    private final String storageKey;
    private final String originalFileName;
    private final String contentType;
    private final long sizeBytes;
    private final int sortOrder;

    public CarImage(
            long id,
            long carId,
            String storageKey,
            String originalFileName,
            String contentType,
            long sizeBytes,
            int sortOrder
    ) {
        this.id = id;
        this.carId = carId;
        this.storageKey = storageKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.sortOrder = sortOrder;
    }

    public long getId() {
        return id;
    }

    public long getCarId() {
        return carId;
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

    public int getSortOrder() {
        return sortOrder;
    }
}
