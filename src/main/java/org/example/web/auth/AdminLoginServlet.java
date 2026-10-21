package org.example.web.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.AdminDao;
import org.example.domain.Admin;
import org.example.security.PasswordHasher;
import org.example.service.AdminService;

import java.io.IOException;

@WebServlet("/auth/admin/login")
public class AdminLoginServlet extends HttpServlet {
    private final AdminService adminService = new AdminService(new AdminDao(), new PasswordHasher());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,IOException {
        String loginId = request.getParameter("loginId");
        String rawPassword = request.getParameter("password");
        String next = safeNextPath(request, request.getParameter("next"));
        try {
            Admin admin = adminService.login(loginId, rawPassword);

            SessionAuthentication.loginAdmin(request,admin);
            response.sendRedirect(next == null
                    ? request.getContextPath() + "/admin"
                    : next);
        } catch (IllegalArgumentException e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("loginId", loginId);
            request.setAttribute("next", next);
            request.getRequestDispatcher("/WEB-INF/views/admin-login.jsp").forward(request, response);
        }
    }

    @Override
    protected  void doGet(
            HttpServletRequest request, HttpServletResponse response
    ) throws ServletException, IOException {

        request.setAttribute(
                "next",
                safeNextPath(request, request.getParameter("next"))
        );
        request.getRequestDispatcher("/WEB-INF/views/admin-login.jsp").forward(request, response);
    }

    private String safeNextPath(HttpServletRequest request, String next) {
        if (next == null || next.isBlank()
                || !next.startsWith("/")
                || next.startsWith("//")
                || next.contains("://")) {
            return null;
        }

        String contextPath = request.getContextPath();
        if (contextPath.isEmpty() || next.startsWith(contextPath + "/")) {
            return next;
        }

        return null;
    }
}
