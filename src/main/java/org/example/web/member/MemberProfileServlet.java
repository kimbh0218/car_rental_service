package org.example.web.member;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.MemberDao;
import org.example.security.PasswordHasher;
import org.example.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.example.web.auth.SessionAuthentication;
import java.io.IOException;
import org.example.domain.Member;
@WebServlet("/member/me")
public class MemberProfileServlet extends HttpServlet {

    private final MemberService memberService = new MemberService(new MemberDao(), new PasswordHasher());

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Long memberId = (Long) session.getAttribute(SessionAuthentication.MEMBER_ID);

        Member member = memberService.findMemberById(memberId);
        SessionAuthentication.updateMemberLicenseStatus(request, member.isLicenseStatus());

        request.setAttribute("memberName", member.getName());
        request.setAttribute("loginId", member.getLoginId());
        request.setAttribute("licenseStatus", member.isLicenseStatus());
        request.setAttribute("licenseRequired",
                !member.isLicenseStatus()
                        && "true".equals(request.getParameter("licenseRequired")));
        request.getRequestDispatcher("/WEB-INF/views/member-profile.jsp").forward(request, response);
    }
}
