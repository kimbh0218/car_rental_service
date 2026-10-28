package org.example.storage;

import com.google.cloud.WriteChannel;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.example.domain.StoredCarImage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GcsCarImageStorageTest {
    private final Storage storageClient = mock(Storage.class);
    private final GcsCarImageStorage storage = new GcsCarImageStorage(
            storageClient,
            "car-rental-images-test"
    );

    @Test
    void savesJpegWithGeneratedCarsKeyAndMetadata() throws Exception {
        byte[] image = "image-data".getBytes(StandardCharsets.UTF_8);
        WriteChannel writeChannel = mock(WriteChannel.class);
        when(storageClient.writer(any(BlobInfo.class))).thenReturn(writeChannel);
        when(writeChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buffer = invocation.getArgument(0);
            int bytes = buffer.remaining();
            buffer.position(buffer.limit());
            return bytes;
        });

        StoredCarImage stored = storage.save(
                new ByteArrayInputStream(image), "car.jpg", "image/jpeg", image.length
        );

        ArgumentCaptor<BlobInfo> blobCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storageClient).writer(blobCaptor.capture());
        BlobInfo blob = blobCaptor.getValue();
        assertEquals("car-rental-images-test", blob.getBucket());
        assertTrue(blob.getName().matches("cars/[0-9a-f-]{36}\\.jpg"));
        assertEquals("image/jpeg", blob.getContentType());
        assertEquals(blob.getName(), stored.getStorageKey());
        assertEquals("car.jpg", stored.getOriginalFileName());
    }

    @Test
    void opensImageFromItsStoredKey() throws Exception {
        ReadChannel readChannel = mock(ReadChannel.class);
        when(storageClient.reader(any(BlobId.class))).thenReturn(readChannel);

        assertNotNull(storage.open("cars/car-id.png"));

        ArgumentCaptor<BlobId> blobCaptor = ArgumentCaptor.forClass(BlobId.class);
        verify(storageClient).reader(blobCaptor.capture());
        assertEquals("car-rental-images-test", blobCaptor.getValue().getBucket());
        assertEquals("cars/car-id.png", blobCaptor.getValue().getName());
    }

    @Test
    void deletesImageFromItsStoredKey() throws Exception {
        storage.delete("cars/car-id.jpg");

        ArgumentCaptor<BlobId> blobCaptor = ArgumentCaptor.forClass(BlobId.class);
        verify(storageClient).delete(blobCaptor.capture());
        assertEquals("car-rental-images-test", blobCaptor.getValue().getBucket());
        assertEquals("cars/car-id.jpg", blobCaptor.getValue().getName());
    }

    @Test
    void rejectsAKeyOutsideTheCarsPrefix() {
        IOException exception = assertThrows(IOException.class, () -> storage.open("other/file.jpg"));

        assertEquals("이미지 저장 키가 올바르지 않습니다.", exception.getMessage());
    }

    @Test
    void wrapsGcsFailuresAsIOException() {
        when(storageClient.delete(any(BlobId.class)))
                .thenThrow(new StorageException(500, "GCS failure"));

        IOException exception = assertThrows(IOException.class, () -> storage.delete("cars/car-id.jpg"));

        assertEquals("GCS에서 이미지를 삭제하지 못했습니다.", exception.getMessage());
        assertInstanceOf(StorageException.class, exception.getCause());
    }
}
