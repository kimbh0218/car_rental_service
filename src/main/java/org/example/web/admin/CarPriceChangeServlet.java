package org.example.web.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.service.CarService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/admin/manage/cars/price")
public class CarPriceChangeServlet extends HttpServlet {

    private final CarService carService = new CarService(
            new CarDao(),
            new ParkingLotDao(),
            new RentalDao()
    );

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        long carId;
        int pricePerKm;

        try {
            carId = Long.parseLong(request.getParameter("carId"));
            pricePerKm = Integer.parseInt(request.getParameter("pricePerKm"));
        } catch (NumberFormatException | NullPointerException e) {
            redirect(response, request, "error", "차량 요금 정보가 올바르지 않습니다.");
            return;
        }

        try {
            carService.changePricePerKm(carId, pricePerKm);
            redirect(response, request, "message", "1km당 요금을 변경했습니다.");
        } catch (IllegalArgumentException e) {
            redirect(response, request, "error", e.getMessage());
        }
    }

    private void redirect(
            HttpServletResponse response,
            HttpServletRequest request,
            String parameter,
            String message
    ) throws IOException {
        response.sendRedirect(request.getContextPath()
                + "/admin/manage/cars?"
                + parameter
                + "="
                + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
