package org.example.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.config.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!databaseIsAvailable()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Database unavailable");
            return;
        }

        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("OK");
    }

    private boolean databaseIsAvailable() {
        try (Connection connection = DBConnection.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException | RuntimeException e) {
            return false;
        }
    }
}
