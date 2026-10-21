package org.example.storage;

import org.example.domain.StoredCarImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class LocalCarImageStorage implements CarImageStorage {
    public static final String UPLOAD_DIRECTORY_ENV = "CAR_IMAGE_UPLOAD_DIR";
    private final Path baseDirectory;

    public LocalCarImageStorage(Path baseDirectory) {
        this.baseDirectory = baseDirectory.toAbsolutePath().normalize();
    }

    public static LocalCarImageStorage fromEnvironment() {
        String configuredDirectory = System.getenv(UPLOAD_DIRECTORY_ENV);
        Path baseDirectory = configuredDirectory == null || configuredDirectory.isBlank()
                ? Path.of(System.getProperty("user.home"), "car-rental-uploads")
                : Path.of(configuredDirectory);
        return new LocalCarImageStorage(baseDirectory);
    }

    @Override
    public StoredCarImage save(
            InputStream imageData,
            String originalFileName,
            String contentType,
            long sizeBytes
    ) throws IOException {
        Path carDirectory = baseDirectory.resolve("cars");
        Files.createDirectories(carDirectory);

        String extension = extensionFor(contentType);
        String fileName = UUID.randomUUID() + extension;
        Path target = carDirectory.resolve(fileName).normalize();
        if (!target.startsWith(carDirectory)) {
            throw new IOException("이미지 저장 경로가 올바르지 않습니다.");
        }

        try {
            Files.copy(imageData, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }

        return new StoredCarImage(
                "cars/" + fileName,
                originalFileName,
                contentType,
                sizeBytes
        );
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) throws IOException {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IOException("이미지 저장 키가 없습니다.");
        }

        Path resolvedPath = baseDirectory.resolve(storageKey).normalize();
        if (!resolvedPath.startsWith(baseDirectory)) {
            throw new IOException("이미지 저장 경로가 올바르지 않습니다.");
        }
        return resolvedPath;
    }

    private String extensionFor(String contentType) throws IOException {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new IOException("지원하지 않는 이미지 형식입니다.");
        };
    }
}
