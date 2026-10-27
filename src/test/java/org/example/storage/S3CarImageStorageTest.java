package org.example.storage;

import org.example.domain.StoredCarImage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3CarImageStorageTest {
    private final S3Client s3Client = mock(S3Client.class);
    private final S3CarImageStorage storage = new S3CarImageStorage(s3Client, "car-rental-images-test");

    @Test
    void savesJpegWithGeneratedCarsKeyAndMetadata() throws Exception {
        byte[] image = "image-data".getBytes(StandardCharsets.UTF_8);

        StoredCarImage stored = storage.save(
                new ByteArrayInputStream(image), "car.jpg", "image/jpeg", image.length
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("car-rental-images-test", request.bucket());
        assertTrue(request.key().matches("cars/[0-9a-f-]{36}\\.jpg"));
        assertEquals("image/jpeg", request.contentType());
        assertEquals(image.length, request.contentLength());
        assertEquals(image.length, bodyCaptor.getValue().optionalContentLength().orElseThrow());
        assertEquals(request.key(), stored.getStorageKey());
        assertEquals("car.jpg", stored.getOriginalFileName());
    }

    @Test
    void opensImageFromItsStoredKey() throws Exception {
        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> response = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);

        assertSame(response, storage.open("cars/car-id.png"));

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(requestCaptor.capture());
        assertEquals("car-rental-images-test", requestCaptor.getValue().bucket());
        assertEquals("cars/car-id.png", requestCaptor.getValue().key());
    }

    @Test
    void deletesImageFromItsStoredKey() throws Exception {
        storage.delete("cars/car-id.jpg");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertEquals("car-rental-images-test", requestCaptor.getValue().bucket());
        assertEquals("cars/car-id.jpg", requestCaptor.getValue().key());
    }

    @Test
    void rejectsAKeyOutsideTheCarsPrefix() {
        IOException exception = assertThrows(IOException.class, () -> storage.open("other/file.jpg"));

        assertEquals("이미지 저장 키가 올바르지 않습니다.", exception.getMessage());
    }

    @Test
    void wrapsS3FailuresAsIOException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(500).build());

        IOException exception = assertThrows(IOException.class, () -> storage.delete("cars/car-id.jpg"));

        assertEquals("S3에서 이미지를 삭제하지 못했습니다.", exception.getMessage());
        assertInstanceOf(S3Exception.class, exception.getCause());
    }
}
