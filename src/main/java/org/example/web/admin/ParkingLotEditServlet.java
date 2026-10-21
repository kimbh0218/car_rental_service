package org.example.web.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.ParkingLotDao;
import org.example.domain.ParkingLot;
import org.example.service.ParkingLotService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/admin/manage/parking-lots/edit")
public class ParkingLotEditServlet extends HttpServlet {

    private final ParkingLotService parkingLotService = new ParkingLotService(
            new ParkingLotDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        long parkingLotId = parseParkingLotId(request, response);
        if (parkingLotId <= 0) {
            return;
        }

        try {
            ParkingLot parkingLot = parkingLotService.findParkingLotById(parkingLotId);
            setFormValues(request, parkingLot);
            request.getRequestDispatcher("/WEB-INF/views/admin-parking-lot-edit.jsp")
                    .forward(request, response);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        long parkingLotId = parseParkingLotId(request, response);
        if (parkingLotId <= 0) {
            return;
        }

        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String maxParkingCountValue = request.getParameter("maxParkingCount");

        try {
            int maxParkingCount = Integer.parseInt(maxParkingCountValue);
            parkingLotService.updateParkingLot(
                    parkingLotId,
                    name,
                    address,
                    maxParkingCount
            );
            response.sendRedirect(request.getContextPath()
                    + "/admin/manage/parking-lots?message="
                    + URLEncoder.encode("주차장 정보를 수정했습니다.", StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            forwardWithError(
                    request,
                    response,
                    parkingLotId,
                    name,
                    address,
                    maxParkingCountValue,
                    "최대 수용 대수는 숫자로 입력해주세요."
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            forwardWithError(
                    request,
                    response,
                    parkingLotId,
                    name,
                    address,
                    maxParkingCountValue,
                    e.getMessage()
            );
        } catch (RuntimeException e) {
            forwardWithError(
                    request,
                    response,
                    parkingLotId,
                    name,
                    address,
                    maxParkingCountValue,
                    "주차장 정보 수정 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    private long parseParkingLotId(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try {
            long parkingLotId = Long.parseLong(request.getParameter("parkingLotId"));
            if (parkingLotId <= 0) {
                throw new NumberFormatException();
            }
            return parkingLotId;
        } catch (NumberFormatException | NullPointerException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "주차장 정보가 올바르지 않습니다.");
            return -1;
        }
    }

    private void forwardWithError(
            HttpServletRequest request,
            HttpServletResponse response,
            long parkingLotId,
            String name,
            String address,
            String maxParkingCount,
            String errorMessage
    ) throws ServletException, IOException {
        request.setAttribute("parkingLotId", parkingLotId);
        request.setAttribute("name", name);
        request.setAttribute("address", address);
        request.setAttribute("maxParkingCount", maxParkingCount);
        request.setAttribute("errorMessage", errorMessage);
        request.getRequestDispatcher("/WEB-INF/views/admin-parking-lot-edit.jsp")
                .forward(request, response);
    }

    private void setFormValues(HttpServletRequest request, ParkingLot parkingLot) {
        request.setAttribute("parkingLotId", parkingLot.getId());
        request.setAttribute("name", parkingLot.getName());
        request.setAttribute("address", parkingLot.getAddress());
        request.setAttribute("maxParkingCount", parkingLot.getMaxParkingCount());
        request.setAttribute(
                "occupiedCarCount",
                parkingLot.getCurrentParkingCount() + parkingLot.getRentedCarCount()
        );
    }
}
