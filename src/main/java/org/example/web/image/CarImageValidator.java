package org.example.web.image;

import jakarta.servlet.http.Part;
import org.example.domain.ValidatedCarImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;

public final class CarImageValidator {
    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_DIMENSION = 4_096;

    private CarImageValidator() {
    }

    public static ValidatedCarImage validate(Part imagePart) throws IOException {
        if (imagePart == null || imagePart.getSize() == 0) {
            return null;
        }
        if (imagePart.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("이미지 파일은 5MB 이하만 업로드할 수 있습니다.");
        }

        try (ImageInputStream imageInput = ImageIO.createImageInputStream(
                imagePart.getInputStream()
        )) {
            if (imageInput == null) {
                throw new IllegalArgumentException("이미지 파일을 읽을 수 없습니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("JPEG 또는 PNG 이미지 파일만 업로드할 수 있습니다.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                if (reader.getWidth(0) > MAX_DIMENSION
                        || reader.getHeight(0) > MAX_DIMENSION) {
                    throw new IllegalArgumentException(
                            "이미지 가로와 세로는 각각 4096px 이하여야 합니다."
                    );
                }

                String contentType = contentTypeFor(reader.getFormatName());
                return new ValidatedCarImage(
                        originalFileName(imagePart.getSubmittedFileName()),
                        contentType,
                        imagePart.getSize()
                );
            } finally {
                reader.dispose();
            }
        }
    }

    private static String contentTypeFor(String formatName) {
        if ("JPEG".equalsIgnoreCase(formatName)
                || "JPG".equalsIgnoreCase(formatName)) {
            return "image/jpeg";
        }
        if ("PNG".equalsIgnoreCase(formatName)) {
            return "image/png";
        }
        throw new IllegalArgumentException("JPEG 또는 PNG 이미지 파일만 업로드할 수 있습니다.");
    }

    private static String originalFileName(String submittedFileName) {
        if (submittedFileName == null || submittedFileName.isBlank()) {
            return "car-image";
        }

        String normalized = submittedFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        return fileName.length() > 255 ? fileName.substring(0, 255) : fileName;
    }
}
