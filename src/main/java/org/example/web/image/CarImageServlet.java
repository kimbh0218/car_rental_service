package org.example.web.image;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarImageDao;
import org.example.domain.CarImage;
import org.example.service.CarImageService;
import org.example.storage.CarImageStorage;
import org.example.storage.CarImageStorageFactory;

import java.io.IOException;
import java.io.InputStream;

@WebServlet("/images/cars/*")
public class CarImageServlet extends HttpServlet {
    private final CarImageService carImageService = new CarImageService(
            new org.example.dao.CarDao(),
            new CarImageDao()
    );
    private final CarImageStorage imageStorage = CarImageStorageFactory.create();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException, ServletException {
        long imageId = imageId(request);
        if (imageId <= 0) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        CarImage image;
        try {
            image = carImageService.findById(imageId);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(image.getContentType());
        response.setContentLengthLong(image.getSizeBytes());
        response.setHeader("Cache-Control", "public, max-age=86400");

        try (InputStream imageData = imageStorage.open(image.getStorageKey())) {
            imageData.transferTo(response.getOutputStream());
        } catch (IOException e) {
            response.reset();
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private long imageId(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.matches("/[1-9][0-9]*")) {
            return -1;
        }
        return Long.parseLong(pathInfo.substring(1));
    }
}
