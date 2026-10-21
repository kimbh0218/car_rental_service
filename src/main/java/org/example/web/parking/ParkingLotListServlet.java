package org.example.web.parking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.service.CarService;
import org.example.service.ParkingLotService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@WebServlet("/parking-lots")
public class ParkingLotListServlet extends HttpServlet {

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
        String startTimeValue = request.getParameter("startTime");
        String returnTimeValue = request.getParameter("returnTime");
        String passengerCountValue = request.getParameter("passengerCount");

        request.setAttribute("startTime", startTimeValue);
        request.setAttribute("returnTime", returnTimeValue);
        request.setAttribute("passengerCount", passengerCountValue);

        boolean hasSearchValue = hasText(startTimeValue)
                || hasText(returnTimeValue)
                || hasText(passengerCountValue);

        if (!hasSearchValue) {
            request.setAttribute("parkingLots", parkingLotService.findAllActiveParkingLots());
        } else {
            request.setAttribute("searched", true);

            try {
                if (!hasText(startTimeValue)
                        || !hasText(returnTimeValue)
                        || !hasText(passengerCountValue)) {
                    throw new IllegalArgumentException("대여 시작·반납 시각과 탑승 인원을 모두 선택해주세요.");
                }

                request.setAttribute("parkingLotAvailability",
                        carService.findParkingLotAvailability(
                                LocalDateTime.parse(startTimeValue),
                                LocalDateTime.parse(returnTimeValue),
                                Integer.parseInt(passengerCountValue)
                        ));
            } catch (DateTimeParseException | NumberFormatException e) {
                request.setAttribute("errorMessage", "검색 조건을 올바르게 입력해주세요.");
            } catch (IllegalArgumentException e) {
                request.setAttribute("errorMessage", e.getMessage());
            }
        }

        request.getRequestDispatcher("/WEB-INF/views/parking-lot-list.jsp")
                .forward(request, response);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
