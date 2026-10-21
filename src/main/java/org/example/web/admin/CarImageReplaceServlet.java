package org.example.web.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.example.dao.CarDao;
import org.example.dao.CarImageDao;
import org.example.domain.StoredCarImage;
import org.example.domain.ValidatedCarImage;
import org.example.service.CarImageService;
import org.example.storage.CarImageStorage;
import org.example.storage.CarImageStorageFactory;
import org.example.web.image.CarImageValidator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/admin/manage/cars/image")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class CarImageReplaceServlet extends HttpServlet {
    private final CarImageService carImageService = new CarImageService(
            new CarDao(),
            new CarImageDao()
    );
    private final CarImageStorage imageStorage = CarImageStorageFactory.create();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException, ServletException {
        StoredCarImage storedImage = null;

        try {
            long carId = Long.parseLong(request.getParameter("carId"));
            Part imagePart = request.getPart("image");
            ValidatedCarImage validatedImage = CarImageValidator.validate(imagePart);
            if (validatedImage == null) {
                throw new IllegalArgumentException("업로드할 차량 이미지를 선택해주세요.");
            }

            try (InputStream imageData = imagePart.getInputStream()) {
                storedImage = imageStorage.save(
                        imageData,
                        validatedImage.getOriginalFileName(),
                        validatedImage.getContentType(),
                        validatedImage.getSizeBytes()
                );
            }

            String previousStorageKey = carImageService.replacePrimaryImage(
                    carId,
                    storedImage
            );
            deleteQuietly(previousStorageKey);

            redirect(response, request, "message", "차량 이미지를 저장했습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            deleteQuietly(storedImage == null ? null : storedImage.getStorageKey());
            redirect(response, request, "error", e.getMessage());
        } catch (IOException e) {
            deleteQuietly(storedImage == null ? null : storedImage.getStorageKey());
            redirect(response, request, "error", "이미지 저장 중 문제가 발생했습니다. 다시 시도해주세요.");
        }
    }

    private void redirect(
            HttpServletResponse response,
            HttpServletRequest request,
            String parameter,
            String message
    ) throws IOException {
        response.sendRedirect(request.getContextPath() + "/admin/manage/cars?"
                + parameter + "="
                + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private void deleteQuietly(String storageKey) {
        if (storageKey == null) {
            return;
        }
        try {
            imageStorage.delete(storageKey);
        } catch (IOException ignored) {
            // 새 DB 참조가 이미 저장된 경우에도 이전 파일 정리 실패는 기능 실패로 처리하지 않는다.
        }
    }
}
