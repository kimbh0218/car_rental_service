package org.example.storage;

import org.example.domain.StoredCarImage;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class S3CarImageStorage implements CarImageStorage {
    public static final String BUCKET_ENV = "S3_BUCKET";
    public static final String REGION_ENV = "AWS_REGION";

    private static final String KEY_PREFIX = "cars/";

    private final S3Client s3Client;
    private final String bucket;

    public S3CarImageStorage(S3Client s3Client, String bucket) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 버킷 이름이 필요합니다.");
        }
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public static S3CarImageStorage fromEnvironment() {
        String bucket = requiredEnvironment(BUCKET_ENV);
        String region = requiredEnvironment(REGION_ENV);

        S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        return new S3CarImageStorage(s3Client, bucket);
    }

    @Override
    public StoredCarImage save(
            InputStream imageData,
            String originalFileName,
            String contentType,
            long sizeBytes
    ) throws IOException {
        String storageKey = KEY_PREFIX + UUID.randomUUID() + extensionFor(contentType);
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .contentLength(sizeBytes)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(imageData, sizeBytes));
        } catch (SdkException e) {
            throw new IOException("S3에 이미지를 저장하지 못했습니다.", e);
        }

        return new StoredCarImage(storageKey, originalFileName, contentType, sizeBytes);
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(validateStorageKey(storageKey))
                    .build());
        } catch (SdkException e) {
            throw new IOException("S3에서 이미지를 읽지 못했습니다.", e);
        }
    }

    @Override
    public void delete(String storageKey) throws IOException {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(validateStorageKey(storageKey))
                    .build());
        } catch (SdkException e) {
            throw new IOException("S3에서 이미지를 삭제하지 못했습니다.", e);
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
