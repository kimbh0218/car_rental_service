package org.example.web.car;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.service.CarService;
import org.example.domain.CarWithParkingLot;
import java.util.List;

import java.io.IOException;

@WebServlet("/cars")
public class CarListServlet extends HttpServlet {

    private final CarService carService = new CarService(
            new CarDao(), new ParkingLotDao(), new RentalDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        List<CarWithParkingLot> cars = carService.findAllVisibleCars();
        request.setAttribute("cars", cars);
        request.getRequestDispatcher("/WEB-INF/views/car-list.jsp").forward(request, response);
    }

}
