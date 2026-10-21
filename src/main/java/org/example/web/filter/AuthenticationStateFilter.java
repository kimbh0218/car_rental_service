package org.example.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.example.web.auth.SessionAuthentication;

import java.io.IOException;

@WebFilter("/*")
public class AuthenticationStateFilter implements Filter {

    @Override
    public void doFilter(
            jakarta.servlet.ServletRequest request,
            jakarta.servlet.ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String authState = SessionAuthentication.isAdminAuthenticated(httpRequest)
                ? "admin"
                : SessionAuthentication.isMemberAuthenticated(httpRequest)
                ? "member"
                : "guest";
        httpRequest.setAttribute("authState", authState);

        chain.doFilter(request, response);
    }
}
