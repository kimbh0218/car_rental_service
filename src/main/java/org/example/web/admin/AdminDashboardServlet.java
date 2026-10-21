package org.example.web.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.domain.CarStatus;
import org.example.domain.CarWithParkingLot;
import org.example.domain.ParkingLot;
import org.example.service.CarService;
import org.example.service.ParkingLotService;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin")
public class AdminDashboardServlet extends HttpServlet {

    private final CarService carService = new CarService(
            new CarDao(),
            new ParkingLotDao(),
            new RentalDao()
    );
    private final ParkingLotService parkingLotService = new ParkingLotService(
            new ParkingLotDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        List<CarWithParkingLot> cars = carService.findAllCars();
        List<ParkingLot> parkingLots = parkingLotService.findAllActiveParkingLots();

        long availableCarCount = cars.stream()
                .filter(car -> car.getCar().getCarStatus() == CarStatus.AVAILABLE)
                .count();
        long rentedCarCount = cars.stream()
                .filter(car -> car.getCar().getCarStatus() == CarStatus.RENTED)
                .count();

        request.setAttribute("carCount", cars.size());
        request.setAttribute("availableCarCount", availableCarCount);
        request.setAttribute("rentedCarCount", rentedCarCount);
        request.setAttribute("parkingLotCount", parkingLots.size());

        request.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp")
                .forward(request, response);
    }
}
