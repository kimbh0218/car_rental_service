package org.example.web.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/member/*")
public class MemberAuthenticationFilter implements Filter {

    @Override
    public void doFilter(
            jakarta.servlet.ServletRequest request,
            jakarta.servlet.ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (SessionAuthentication.isMemberAuthenticated(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        if (SessionAuthentication.isAdminAuthenticated(httpRequest)) {
            AccessDeniedPage.forward(
                    httpRequest,
                    httpResponse,
                    HttpServletResponse.SC_FORBIDDEN,
                    "회원 권한이 필요합니다.",
                    "운영자 계정으로는 회원 예약 메뉴를 이용할 수 없습니다.",
                    httpRequest.getContextPath() + "/admin",
                    "운영자 메인으로",
                    null
            );
            return;
        }

        AccessDeniedPage.forward(
                httpRequest,
                httpResponse,
                HttpServletResponse.SC_UNAUTHORIZED,
                "회원 로그인이 필요합니다.",
                "이 메뉴는 회원 로그인 후 이용할 수 있습니다.",
                AccessDeniedPage.loginUrl(httpRequest, "/auth/member/login"),
                "회원 로그인",
                null
        );
    }
}
