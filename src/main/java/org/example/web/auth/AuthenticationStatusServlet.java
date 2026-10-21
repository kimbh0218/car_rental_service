package org.example.web.auth;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/auth/status")
public class AuthenticationStatusServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String authState = SessionAuthentication.isAdminAuthenticated(request)
                ? "admin"
                : SessionAuthentication.isMemberAuthenticated(request)
                ? "member"
                : "guest";

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"authState\":\"" + authState + "\"}");
    }
}
