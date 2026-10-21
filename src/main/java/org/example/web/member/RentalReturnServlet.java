package org.example.web.member;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/member/rentals/return")
public class RentalReturnServlet extends HttpServlet {
    private final RentalService rentalService = new RentalService(
            new CarDao(),
            new RentalDao(),
            new MemberDao(),
            new ParkingLotDao()
    );
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession session = request.getSession(false);
        Long memberId = (Long) session.getAttribute(SessionAuthentication.MEMBER_ID);

        try {
            long rentalId = Long.parseLong(request.getParameter("rentalId"));
            rentalService.returnCar(rentalId, memberId);
            redirect(response, request, "message", "반납 처리가 완료되었습니다.");
        } catch (NumberFormatException e) {
            redirect(response, request, "error", "예약 정보가 올바르지 않습니다.");
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
                + "/member/reservations?"
                + parameter
                + "="
                + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }
}
