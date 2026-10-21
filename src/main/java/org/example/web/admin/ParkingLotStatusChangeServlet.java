package org.example.web.admin;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.ParkingLotDao;
import org.example.domain.ParkingLotStatus;
import org.example.service.ParkingLotService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/admin/manage/parking-lots/status")
public class ParkingLotStatusChangeServlet extends HttpServlet {

    private final ParkingLotService parkingLotService = new ParkingLotService(
            new ParkingLotDao()
    );

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        long parkingLotId;
        ParkingLotStatus parkingLotStatus;

        try {
            parkingLotId = Long.parseLong(request.getParameter("parkingLotId"));
            parkingLotStatus = ParkingLotStatus.valueOf(request.getParameter("parkingLotStatus"));
        } catch (IllegalArgumentException | NullPointerException e) {
            redirect(response, request, "error", "주차장 상태 정보가 올바르지 않습니다.");
            return;
        }

        try {
            parkingLotService.changeParkingLotStatus(parkingLotId, parkingLotStatus);
            String message = parkingLotStatus == ParkingLotStatus.INACTIVE
                    ? "주차장을 비활성화했습니다."
                    : "주차장을 다시 활성화했습니다.";
            redirect(response, request, "message", message);
        } catch (IllegalArgumentException | IllegalStateException e) {
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
                + "/admin/manage/parking-lots?"
                + parameter
                + "="
                + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
