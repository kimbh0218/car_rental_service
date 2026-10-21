package org.example.web.member;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.dao.MemberDao;
import org.example.security.PasswordHasher;
import org.example.service.MemberService;
import org.example.web.auth.SessionAuthentication;

import java.io.IOException;

@WebServlet("/member/me/license")
public class MemberLicenseRegistrationServlet extends HttpServlet {

    private final MemberService memberService = new MemberService(
            new MemberDao(), new PasswordHasher()
    );

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        HttpSession session = request.getSession(false);
        Long memberId = (Long) session.getAttribute(SessionAuthentication.MEMBER_ID);

        memberService.registerLicense(memberId);
        SessionAuthentication.updateMemberLicenseStatus(request, true);
        response.sendRedirect(request.getContextPath() + "/member/me");
    }
}
