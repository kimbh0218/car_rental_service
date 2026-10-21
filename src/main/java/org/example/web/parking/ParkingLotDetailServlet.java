package org.example.web.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.ParkingLot;
import org.example.service.CarService;
import org.example.service.ParkingLotService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@WebServlet("/parking-lots/detail")
public class ParkingLotDetailServlet extends HttpServlet {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm");

    private final ParkingLotService parkingLotService = new ParkingLotService(
            new ParkingLotDao()
    );
    private final CarService carService = new CarService(
            new CarDao(),
            new ParkingLotDao(),
            new RentalDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        long parkingLotId;
        try {
            parkingLotId = Long.parseLong(request.getParameter("parkingLotId"));
        } catch (NumberFormatException | NullPointerException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "주차장 정보가 올바르지 않습니다.");
            return;
        }

        ParkingLot parkingLot;
        try {
            parkingLot = parkingLotService.findActiveParkingLotById(parkingLotId);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
            return;
        }

        String startTimeValue = request.getParameter("startTime");
        String returnTimeValue = request.getParameter("returnTime");
        String passengerCountValue = request.getParameter("passengerCount");

        request.setAttribute("parkingLot", parkingLot);
        request.setAttribute("startTime", startTimeValue);
        request.setAttribute("returnTime", returnTimeValue);
        request.setAttribute("passengerCount", passengerCountValue);

        try {
            if (!hasText(startTimeValue)
                    || !hasText(returnTimeValue)
                    || !hasText(passengerCountValue)) {
                throw new IllegalArgumentException("대여 조건을 먼저 선택해주세요.");
            }

            LocalDateTime startTime = LocalDateTime.parse(startTimeValue);
            LocalDateTime returnTime = LocalDateTime.parse(returnTimeValue);

            request.setAttribute("cars", carService.findAvailableCarsAtParkingLot(
                    parkingLotId,
                    startTime,
                    returnTime,
                    Integer.parseInt(passengerCountValue)
            ));
            request.setAttribute(
                    "startTimeDisplay",
                    DATE_TIME_FORMATTER.format(startTime)
            );
            request.setAttribute(
                    "returnTimeDisplay",
                    DATE_TIME_FORMATTER.format(returnTime)
            );
        } catch (DateTimeParseException | NumberFormatException e) {
            redirectToParkingLotList(response, request, "검색 조건을 올바르게 입력해주세요.");
            return;
        } catch (IllegalArgumentException e) {
            redirectToParkingLotList(response, request, e.getMessage());
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/parking-lot-detail.jsp")
                .forward(request, response);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void redirectToParkingLotList(
            HttpServletResponse response,
            HttpServletRequest request,
            String message
    ) throws IOException {
        response.sendRedirect(request.getContextPath()
                + "/parking-lots?error="
                + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
