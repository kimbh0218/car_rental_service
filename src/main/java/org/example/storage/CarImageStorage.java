package org.example.storage;

import org.example.domain.StoredCarImage;

import java.io.IOException;
import java.io.InputStream;

public interface CarImageStorage {

    StoredCarImage save(
            InputStream imageData,
            String originalFileName,
            String contentType,
            long sizeBytes
    ) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
