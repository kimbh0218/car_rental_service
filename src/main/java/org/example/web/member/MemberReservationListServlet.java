package org.example.web.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.dao.CarDao;
import org.example.dao.MemberDao;
import org.example.dao.ParkingLotDao;
import org.example.dao.RentalDao;
import org.example.service.RentalService;
import org.example.web.auth.SessionAuthentication;

import java.io.IOException;

@WebServlet("/member/reservations")
public class MemberReservationListServlet extends HttpServlet {
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
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Long memberId = (Long) session.getAttribute(SessionAuthentication.MEMBER_ID);

        request.setAttribute("rentals", rentalService.findMyRentals(memberId));
        request.getRequestDispatcher("/WEB-INF/views/member-reservation-list.jsp")
                .forward(request, response);
    }
}
