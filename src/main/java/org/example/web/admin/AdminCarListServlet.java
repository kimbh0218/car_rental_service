package org.example.web.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.CarWithParkingLot;
import org.example.service.CarService;
import org.example.service.ParkingLotService;
import org.example.domain.ParkingLot;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin/manage/cars")
public class AdminCarListServlet extends HttpServlet {

    private final CarService carService = new CarService(
            new CarDao(),
            new ParkingLotDao(),
            new RentalDao()
    );

    private final ParkingLotService parkingLotService = new ParkingLotService(
            new ParkingLotDao()
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<CarWithParkingLot> cars = carService.findAllCars();
        List<ParkingLot> parkingLots = parkingLotService.findAllParkingLots();

        request.setAttribute("cars", cars);
        request.setAttribute("parkingLots", parkingLots);

        request.getRequestDispatcher("/WEB-INF/views/admin-car-list.jsp").forward(request, response);
    }
}
