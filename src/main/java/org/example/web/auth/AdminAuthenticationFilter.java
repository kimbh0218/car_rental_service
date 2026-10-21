package org.example.web.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/admin/*")
public class AdminAuthenticationFilter implements Filter {

    @Override
    public void doFilter(
            jakarta.servlet.ServletRequest request,
            jakarta.servlet.ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (SessionAuthentication.isAdminAuthenticated(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        if (SessionAuthentication.isMemberAuthenticated(httpRequest)) {
            AccessDeniedPage.forward(
                    httpRequest,
                    httpResponse,
                    HttpServletResponse.SC_FORBIDDEN,
                    "운영자 권한이 필요합니다.",
                    "현재 회원 계정으로는 운영자 메뉴를 이용할 수 없습니다.",
                    httpRequest.getContextPath() + "/auth/admin/login",
                    "운영자 로그인",
                    "운영자로 로그인하면 현재 회원 세션은 종료됩니다."
            );
            return;
        }

        AccessDeniedPage.forward(
                httpRequest,
                httpResponse,
                HttpServletResponse.SC_UNAUTHORIZED,
                "운영자 로그인이 필요합니다.",
                "이 메뉴는 권한이 있는 운영자만 이용할 수 있습니다.",
                AccessDeniedPage.loginUrl(httpRequest, "/auth/admin/login"),
                "운영자 로그인",
                null
        );
    }
}
