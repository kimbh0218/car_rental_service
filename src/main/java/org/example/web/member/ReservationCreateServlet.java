package org.example.web.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CarDao;
import org.example.dao.ParkingLotDao;
import org.example.service.CarService;
import org.example.domain.Car;

import jakarta.servlet.http.HttpSession;
import org.example.dao.MemberDao;
import org.example.dao.RentalDao;
import org.example.dto.ReservationRequest;
import org.example.service.MemberService;
import org.example.service.RentalService;
import org.example.web.auth.SessionAuthentication;

import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.io.IOException;

@WebServlet ("/member/reservations/new")
public class ReservationCreateServlet extends HttpServlet {

    private final CarService carService = new CarService(
            new CarDao(), new ParkingLotDao(), new RentalDao()
    );

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Long memberId = (Long) session.getAttribute(SessionAuthentication.MEMBER_ID);
        boolean licenseStatus = memberService.findMemberById(memberId).isLicenseStatus();
        SessionAuthentication.updateMemberLicenseStatus(request, licenseStatus);
        if (!licenseStatus) {
            redirectToLicenseNotice(request, response);
            return;
        }

        String carIdValue = request.getParameter("carId");

        long carId;
        try {
            carId = Long.parseLong(carIdValue);
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "차량 ID가 올바르지 않습니다.");
            return;
        }

        Car car;
        try {
            car = carService.findCarById(carId);
        } catch (IllegalArgumentException e) {
            response.sendError (HttpServletResponse.SC_NOT_FOUND, "차량을 찾을 수 없습니다.");
            return;
        }

        request.setAttribute("car", car);
        request.setAttribute("startTime", request.getParameter("startTime"));
        request.setAttribute("returnTime", request.getParameter("returnTime"));
        request.setAttribute("passengerCount", request.getParameter("passengerCount"));

        request.getRequestDispatcher("/WEB-INF/views/reservation-form.jsp").forward(request, response);
    }

    private final RentalService rentalService = new RentalService(
            new CarDao(),
            new RentalDao(),
            new MemberDao(),
            new ParkingLotDao()
            );

    private final MemberService memberService = new MemberService(
            new MemberDao(), new org.example.security.PasswordHasher()
    );

    @Override
    protected void doPost (
            HttpServletRequest request, HttpServletResponse response
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Long memberId = (Long) session.getAttribute(SessionAuthentication.MEMBER_ID);

        String carIdValue = request.getParameter("carId");
        String startTimeValue = request.getParameter("startTime");
        String returnTimeValue = request.getParameter("returnTime");
        String passengerCountValue = request.getParameter("passengerCount");
        try {
            long carId = Long.parseLong(carIdValue);
            LocalDateTime startTime = LocalDateTime.parse(startTimeValue);
            LocalDateTime returnTime = LocalDateTime.parse(returnTimeValue);
            int passengerCount = Integer.parseInt(passengerCountValue);
            ReservationRequest reservationRequest =
                    new ReservationRequest(
                            memberId,
                            carId,
                            startTime,
                            returnTime,
                            passengerCount
                    );

            rentalService.reserve(reservationRequest);

            response.sendRedirect(
                    request.getContextPath() + "/cars?reservation=created"
            );
        } catch (
                DateTimeParseException | IllegalArgumentException | IllegalStateException e
        ) {
            String errorMessage =
                    (e instanceof NumberFormatException
                            || e instanceof DateTimeParseException)
                    ? "예약 정보를 올바르게 입력해주세요."
                            : e.getMessage();

            request.setAttribute("errorMessage", errorMessage);
            request.setAttribute("startTime", startTimeValue);
            request.setAttribute("returnTime", returnTimeValue);
            request.setAttribute("passengerCount", passengerCountValue);

            Car car;
            try {
                long carId = Long.parseLong(carIdValue);
                car = carService.findCarById(carId);
            } catch (NumberFormatException ignored) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "차량 정보가 올바르지 않습니다");
                return;
            } catch (IllegalArgumentException ignored) {
                response.sendError (
                        HttpServletResponse.SC_NOT_FOUND,
                        "차량을 찾을 수 없습니다."
                );
                return;
            }

            request.setAttribute("car", car);
            request.getRequestDispatcher("/WEB-INF/views/reservation-form.jsp").forward(request, response);

        }
    }

    private void redirectToLicenseNotice(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        response.sendRedirect(request.getContextPath() + "/member/me?licenseRequired=true");
    }

}
