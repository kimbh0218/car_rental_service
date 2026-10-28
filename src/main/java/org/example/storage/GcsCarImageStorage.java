package org.example.storage;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import org.example.domain.StoredCarImage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.UUID;

public class GcsCarImageStorage implements CarImageStorage {
    public static final String BUCKET_ENV = "GCS_BUCKET";

    private static final String KEY_PREFIX = "cars/";

    private final Storage storage;
    private final String bucket;

    public GcsCarImageStorage(Storage storage, String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("GCS 버킷 이름이 필요합니다.");
        }
        this.storage = storage;
        this.bucket = bucket;
    }

    public static GcsCarImageStorage fromEnvironment() {
        return new GcsCarImageStorage(
                StorageOptions.getDefaultInstance().getService(),
                requiredEnvironment(BUCKET_ENV)
        );
    }

    @Override
    public StoredCarImage save(
            InputStream imageData,
            String originalFileName,
            String contentType,
            long sizeBytes
    ) throws IOException {
        String storageKey = KEY_PREFIX + UUID.randomUUID() + extensionFor(contentType);
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, storageKey))
                .setContentType(contentType)
                .build();

        try (WriteChannel writeChannel = storage.writer(blobInfo);
             OutputStream output = Channels.newOutputStream(writeChannel)) {
            imageData.transferTo(output);
        } catch (StorageException e) {
            throw new IOException("GCS에 이미지를 저장하지 못했습니다.", e);
        }

        return new StoredCarImage(storageKey, originalFileName, contentType, sizeBytes);
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        try {
            return Channels.newInputStream(storage.reader(BlobId.of(
                    bucket,
                    validateStorageKey(storageKey)
            )));
        } catch (StorageException e) {
            throw new IOException("GCS에서 이미지를 읽지 못했습니다.", e);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            storage.delete(BlobId.of(bucket, validateStorageKey(storageKey)));
        } catch (StorageException e) {
            throw new IOException("GCS에서 이미지를 삭제하지 못했습니다.", e);
        }
    }

    private String validateStorageKey(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank() || !storageKey.startsWith(KEY_PREFIX)
                || storageKey.contains("..")) {
            throw new IOException("이미지 저장 키가 올바르지 않습니다.");
        }
        return storageKey;
    }

    private String extensionFor(String contentType) throws IOException {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IOException("지원하지 않는 이미지 형식입니다.");
        };
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 환경 변수가 필요합니다.");
        }
        return value;
    }
}
