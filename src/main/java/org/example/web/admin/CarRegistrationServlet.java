package org.example.web.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.StoredCarImage;
import org.example.domain.ValidatedCarImage;
import org.example.service.CarService;
import org.example.service.ParkingLotService;
import org.example.domain.ParkingLot;
import org.example.storage.CarImageStorage;
import org.example.storage.CarImageStorageFactory;
import org.example.web.image.CarImageValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@WebServlet("/admin/manage/cars/new")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class CarRegistrationServlet extends HttpServlet {

    private final CarService carService = new CarService(new CarDao(), new ParkingLotDao(), new RentalDao());

    private final ParkingLotService parkingLotService = new ParkingLotService(new ParkingLotDao());
    private final CarImageStorage imageStorage = CarImageStorageFactory.create();

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response
    ) throws ServletException, IOException {
        List<ParkingLot> parkingLots = parkingLotService.findAllActiveParkingLots();
        request.setAttribute("parkingLots", parkingLots);

        request.getRequestDispatcher("/WEB-INF/views/admin-car-form.jsp").forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request, HttpServletResponse response
    ) throws ServletException, IOException {
        String name = request.getParameter("name");
        String maxPassengerCountValue = request.getParameter("maxPassengerCount");
        String pricePerKmValue = request.getParameter("pricePerKm");
        String parkingLotIdValue = request.getParameter("parkingLotId");
        StoredCarImage storedImage = null;

        try {
            int maxPassengerCount = Integer.parseInt(maxPassengerCountValue);
            int pricePerKm = Integer.parseInt(pricePerKmValue);
            long parkingLotId = Long.parseLong(parkingLotIdValue);

            Part imagePart = request.getPart("image");
            ValidatedCarImage validatedImage = CarImageValidator.validate(imagePart);
            if (validatedImage != null) {
                try (InputStream imageData = imagePart.getInputStream()) {
                    storedImage = imageStorage.save(
                            imageData,
                            validatedImage.getOriginalFileName(),
                            validatedImage.getContentType(),
                            validatedImage.getSizeBytes()
                    );
                }
            }

            carService.registerCar(
                    name,
                    maxPassengerCount,
                    pricePerKm,
                    parkingLotId,
                    storedImage
            );

            response.sendRedirect(request.getContextPath() + "/cars");
        } catch  (IllegalArgumentException | IllegalStateException | IOException e) {
            deleteQuietly(storedImage);
            forwardWithError(
                    request,
                    response,
                    e instanceof IOException
                            ? "이미지 저장 중 문제가 발생했습니다. 다시 시도해주세요."
                            : e.getMessage(),
                    name,
                    maxPassengerCountValue,
                    pricePerKmValue,
                    parkingLotIdValue
            );
        }
    }

    private void forwardWithError(
            HttpServletRequest request,
            HttpServletResponse response,
            String errorMessage,
            String name,
            String maxPassengerCountValue,
            String pricePerKmValue,
            String parkingLotIdValue
    ) throws ServletException, IOException {
        request.setAttribute("errorMessage", errorMessage);
        request.setAttribute("name", name);
        request.setAttribute("maxPassengerCount", maxPassengerCountValue);
        request.setAttribute("pricePerKm", pricePerKmValue);
        request.setAttribute("parkingLotId", parkingLotIdValue);
        request.setAttribute("parkingLots", parkingLotService.findAllActiveParkingLots());
        request.getRequestDispatcher("/WEB-INF/views/admin-car-form.jsp")
                .forward(request, response);
    }

    private void deleteQuietly(StoredCarImage image) {
        if (image == null) {
            return;
        }
        try {
            imageStorage.delete(image.getStorageKey());
        } catch (IOException ignored) {
            // DB 등록 실패 뒤 남은 파일은 이후 운영 점검에서 정리한다.
        }
    }
}
