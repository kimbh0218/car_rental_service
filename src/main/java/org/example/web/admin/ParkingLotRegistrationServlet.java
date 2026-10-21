package org.example.web.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.ParkingLotDao;
import org.example.service.ParkingLotService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/admin/manage/parking-lots/new")
public class ParkingLotRegistrationServlet extends HttpServlet {

    private final ParkingLotService parkingLotService = new ParkingLotService(
            new ParkingLotDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/admin-parking-lot-form.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        String name = request.getParameter("name");
        String address = request.getParameter("address");
        String maxParkingCountValue = request.getParameter("maxParkingCount");

        try {
            int maxParkingCount = Integer.parseInt(maxParkingCountValue);
            parkingLotService.registerParkingLot(name, maxParkingCount, address);

            response.sendRedirect(request.getContextPath()
                    + "/admin/manage/parking-lots?message="
                    + URLEncoder.encode("주차장을 등록했습니다.", StandardCharsets.UTF_8));

        } catch (NumberFormatException e) {
            forwardWithError(
                    request,
                    response,
                    name,
                    address,
                    maxParkingCountValue,
                    "최대 주차 가능 대수는 숫자로 입력해주세요."
            );
        } catch (IllegalArgumentException e) {
            forwardWithError(
                    request,
                    response,
                    name,
                    address,
                    maxParkingCountValue,
                    e.getMessage()
            );
        } catch (RuntimeException e) {
            forwardWithError(
                    request,
                    response,
                    name,
                    address,
                    maxParkingCountValue,
                    "주차장 등록 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            );
        }
    }

    private void forwardWithError(
            HttpServletRequest request,
            HttpServletResponse response,
            String name,
            String address,
            String maxParkingCount,
            String errorMessage
    ) throws ServletException, IOException {
        request.setAttribute("name", name);
        request.setAttribute("address", address);
        request.setAttribute("maxParkingCount", maxParkingCount);
        request.setAttribute("errorMessage", errorMessage);
        request.getRequestDispatcher("/WEB-INF/views/admin-parking-lot-form.jsp")
                .forward(request, response);
    }
}
