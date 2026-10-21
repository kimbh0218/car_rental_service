package org.example.web.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.MemberDao;
import org.example.security.PasswordHasher;
import org.example.service.MemberService;

import java.io.IOException;


@WebServlet("/auth/member/signup")
public class MemberSignupServlet extends HttpServlet{

    private final MemberService memberService = new MemberService(new MemberDao(), new PasswordHasher());

    @Override
    protected void doGet(
            HttpServletRequest request, HttpServletResponse response
    ) throws ServletException, IOException {

        request.getRequestDispatcher(
                "/WEB-INF/views/member-signup.jsp").forward(request, response);
    }

    @Override
    protected void doPost(
        HttpServletRequest request, HttpServletResponse response
    ) throws ServletException, IOException {
        String name = request.getParameter("name");
        String loginId = request.getParameter("loginId");
        String rawPassword = request.getParameter("password");
        String passwordConfirm = request.getParameter("passwordConfirm");
        boolean licenseStatus = Boolean.parseBoolean(request.getParameter("licenseStatus"));

        if (!rawPassword.equals(passwordConfirm)) {
            request.setAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            request.setAttribute("name", name);
            request.setAttribute("loginId", loginId);
            request.setAttribute("licenseStatus", licenseStatus);
            request.getRequestDispatcher("/WEB-INF/views/member-signup.jsp").forward(request, response);
            return;
        }
        try {
            memberService.registerMember(name, licenseStatus, loginId, rawPassword);

            response.sendRedirect(
                    request.getContextPath() + "/auth/member/login"
            );
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("name", name);
            request.setAttribute("loginId", loginId);
            request.setAttribute("licenseStatus", licenseStatus);
            request.getRequestDispatcher("/WEB-INF/views/member-signup.jsp").forward(request, response);
        }

    }
}
