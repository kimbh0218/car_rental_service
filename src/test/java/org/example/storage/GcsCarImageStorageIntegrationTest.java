package org.example.storage;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.example.domain.StoredCarImage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GcsCarImageStorageIntegrationTest {
    private static final String BUCKET = "car-rental-images-test";
    private static final GenericContainer<?> GCS_EMULATOR = new GenericContainer<>(
            DockerImageName.parse("fsouza/fake-gcs-server:1.56.1")
    ).withCommand("-scheme", "http")
            .withExposedPorts(4443)
            .waitingFor(Wait.forHttp("/storage/v1/b").forPort(4443).forStatusCode(200));

    private static Storage storageClient;
    private static GcsCarImageStorage storage;

    @BeforeAll
    static void startGcs() {
        GCS_EMULATOR.start();
        storageClient = StorageOptions.newBuilder()
                .setHost("http://" + GCS_EMULATOR.getHost() + ":" + GCS_EMULATOR.getMappedPort(4443))
                .setProjectId("car-rental-test")
                .setCredentials(GoogleCredentials.create(new AccessToken(
                        "test-token",
                        Date.from(Instant.now().plusSeconds(3600))
                )))
                .build()
                .getService();
        storageClient.create(BucketInfo.newBuilder(BUCKET).build());
        storage = new GcsCarImageStorage(storageClient, BUCKET);
    }

    @AfterAll
    static void stopGcs() {
        GCS_EMULATOR.stop();
    }

    @Test
    void savesReadsAndDeletesAnImageInGcs() throws Exception {
        byte[] image = "gcs-image-data".getBytes(StandardCharsets.UTF_8);

        StoredCarImage stored = storage.save(
                new ByteArrayInputStream(image), "car.png", "image/png", image.length
        );

        assertTrue(stored.getStorageKey().matches("cars/[0-9a-f-]{36}\\.png"));
        assertEquals("image/png", storageClient.get(BUCKET, stored.getStorageKey()).getContentType());

        try (InputStream loaded = storage.open(stored.getStorageKey())) {
            assertArrayEquals(image, loaded.readAllBytes());
        }

        storage.delete(stored.getStorageKey());

        assertNull(storageClient.get(BUCKET, stored.getStorageKey()));
    }
}
