package org.example.storage;

import org.example.domain.StoredCarImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3CarImageStorageIntegrationTest {
    private static final String BUCKET = "car-rental-images-test";
    private static final GenericContainer<?> S3_MOCK = new GenericContainer<>(
            DockerImageName.parse("adobe/s3mock:5.2.3")
    ).withExposedPorts(9090)
            .waitingFor(Wait.forHttp("/favicon.ico").forPort(9090).forStatusCode(200));

    private static S3Client s3Client;
    private static S3CarImageStorage storage;

    @BeforeAll
    static void startS3() {
        S3_MOCK.start();
        s3Client = S3Client.builder()
                .endpointOverride(URI.create("http://" + S3_MOCK.getHost() + ":" + S3_MOCK.getMappedPort(9090)))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .forcePathStyle(true)
                .build();
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        storage = new S3CarImageStorage(s3Client, BUCKET);
    }

    @AfterAll
    static void stopS3() {
        if (s3Client != null) {
            s3Client.close();
        }
        S3_MOCK.stop();
    }

    @Test
    void savesReadsAndDeletesAnImageInS3() throws Exception {
        byte[] image = "localstack-image-data".getBytes(StandardCharsets.UTF_8);

        StoredCarImage stored = storage.save(
                new ByteArrayInputStream(image), "car.png", "image/png", image.length
        );

        assertTrue(stored.getStorageKey().matches("cars/[0-9a-f-]{36}\\.png"));
        assertEquals("image/png", s3Client.headObject(HeadObjectRequest.builder()
                .bucket(BUCKET)
                .key(stored.getStorageKey())
                .build()).contentType());

        try (InputStream loaded = storage.open(stored.getStorageKey())) {
            assertArrayEquals(image, loaded.readAllBytes());
        }

        storage.delete(stored.getStorageKey());

        S3Exception exception = assertThrows(S3Exception.class, () -> s3Client.headObject(HeadObjectRequest.builder()
                .bucket(BUCKET)
                .key(stored.getStorageKey())
                .build()));
        assertEquals(404, exception.statusCode());
    }
}
