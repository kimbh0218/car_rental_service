package org.example.web.car;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.MemberDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.dto.CarScheduleItem;
import org.example.service.RentalService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet("/cars/schedule")
public class CarScheduleServlet extends HttpServlet {
    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 90;

    private final RentalService rentalService = new RentalService(
            new CarDao(),
            new RentalDao(),
            new MemberDao(),
            new ParkingLotDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try {
            long carId = Long.parseLong(request.getParameter("carId"));
            LocalDate from = parseFrom(request.getParameter("from"));
            int days = parseDays(request.getParameter("days"));

            List<CarScheduleItem> schedule = rentalService.findCarSchedule(
                    carId,
                    from.atStartOfDay(),
                    from.plusDays(days).atStartOfDay()
            );

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(toJson(schedule));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private LocalDate parseFrom(String value) {
        return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value);
    }

    private int parseDays(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_DAYS;
        }

        int days = Integer.parseInt(value);
        if (days <= 0 || days > MAX_DAYS) {
            throw new IllegalArgumentException("조회 기간은 1일 이상 90일 이하여야 합니다.");
        }
        return days;
    }

    private String toJson(List<CarScheduleItem> schedule) {
        StringBuilder json = new StringBuilder("[");

        for (int index = 0; index < schedule.size(); index++) {
            CarScheduleItem item = schedule.get(index);
            if (index > 0) {
                json.append(',');
            }
            json.append("{\"startTime\":\"")
                    .append(item.startTime())
                    .append("\",\"returnTime\":\"")
                    .append(item.returnTime())
                    .append("\",\"status\":\"")
                    .append(item.rentalStatus().name())
                    .append("\"}");
        }

        return json.append(']').toString();
    }
}
