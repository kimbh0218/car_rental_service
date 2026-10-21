package org.example.storage;

public final class CarImageStorageFactory {
    private static final String STORAGE_TYPE_ENV = "IMAGE_STORAGE_TYPE";

    private CarImageStorageFactory() {
    }

    public static CarImageStorage create() {
        String storageType = System.getenv(STORAGE_TYPE_ENV);
        if (storageType == null || storageType.isBlank()
                || "local".equalsIgnoreCase(storageType)) {
            return LocalCarImageStorage.fromEnvironment();
        }

        throw new IllegalStateException(
                "지원하지 않는 이미지 저장소 유형입니다: " + storageType
        );
    }
}
