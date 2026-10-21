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
import java.util.List;

@WebServlet("/admin/manage/parking-lots")
public class ParkingLotListServlet extends HttpServlet {

    private final ParkingLotService parkingLotService = new ParkingLotService(
            new ParkingLotDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        List<ParkingLot> parkingLots = parkingLotService.findAllParkingLots();
        request.setAttribute("parkingLots", parkingLots);

        request.getRequestDispatcher("/WEB-INF/views/admin-parking-lot-list.jsp")
                .forward(request, response);
    }
}
