package org.example.web.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class AccessDeniedPage {

    private AccessDeniedPage() {
    }

    public static void forward(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String title,
            String message,
            String actionUrl,
            String actionText,
            String notice
    ) throws ServletException, IOException {
        response.setStatus(status);
        request.setAttribute("errorStatus", status);
        request.setAttribute("errorTitle", title);
        request.setAttribute("errorMessage", message);
        request.setAttribute("primaryActionUrl", actionUrl);
        request.setAttribute("primaryActionText", actionText);
        request.setAttribute("errorNotice", notice);
        request.getRequestDispatcher("/WEB-INF/views/error/access-denied.jsp")
                .forward(request, response);
    }

    public static String loginUrl(
            HttpServletRequest request,
            String loginPath
    ) {
        String next = request.getRequestURI();
        if (request.getQueryString() != null && !request.getQueryString().isBlank()) {
            next += "?" + request.getQueryString();
        }

        return request.getContextPath()
                + loginPath
                + "?reason=login-required&next="
                + URLEncoder.encode(next, StandardCharsets.UTF_8);
    }
}
