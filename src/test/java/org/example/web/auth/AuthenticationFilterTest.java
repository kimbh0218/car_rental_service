package org.example.web.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationFilterTest {

    @Test
    void memberFilterPassesAuthenticatedMemberToNextHandler() throws Exception {
        HttpServletRequest request = authenticatedRequest(LoginType.MEMBER, 1L);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new MemberAuthenticationFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void memberFilterForwardsGuestToMemberLoginPage() throws Exception {
        HttpServletRequest request = guestRequest("/car-rental/member/me", "tab=rentals");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestDispatcher("/WEB-INF/views/error/access-denied.jsp")).thenReturn(dispatcher);

        new MemberAuthenticationFilter().doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(request).setAttribute("errorTitle", "회원 로그인이 필요합니다.");
        verify(request).setAttribute(
                "primaryActionUrl",
                "/car-rental/auth/member/login?reason=login-required&next=%2Fcar-rental%2Fmember%2Fme%3Ftab%3Drentals"
        );
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void memberFilterRejectsAdministratorSession() throws Exception {
        HttpServletRequest request = authenticatedRequest(LoginType.ADMIN, 2L);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getContextPath()).thenReturn("/car-rental");
        when(request.getRequestDispatcher("/WEB-INF/views/error/access-denied.jsp")).thenReturn(dispatcher);

        new MemberAuthenticationFilter().doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(request).setAttribute("errorTitle", "회원 권한이 필요합니다.");
        verify(request).setAttribute("primaryActionUrl", "/car-rental/admin");
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void adminFilterPassesAuthenticatedAdministratorToNextHandler() throws Exception {
        HttpServletRequest request = authenticatedRequest(LoginType.ADMIN, 2L);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new AdminAuthenticationFilter().doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void adminFilterForwardsGuestToAdministratorLoginPage() throws Exception {
        HttpServletRequest request = guestRequest("/car-rental/admin/manage/cars", null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestDispatcher("/WEB-INF/views/error/access-denied.jsp")).thenReturn(dispatcher);

        new AdminAuthenticationFilter().doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(request).setAttribute("errorTitle", "운영자 로그인이 필요합니다.");
        verify(request).setAttribute(
                "primaryActionUrl",
                "/car-rental/auth/admin/login?reason=login-required&next=%2Fcar-rental%2Fadmin%2Fmanage%2Fcars"
        );
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void adminFilterRejectsMemberSession() throws Exception {
        HttpServletRequest request = authenticatedRequest(LoginType.MEMBER, 1L);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getContextPath()).thenReturn("/car-rental");
        when(request.getRequestDispatcher("/WEB-INF/views/error/access-denied.jsp")).thenReturn(dispatcher);

        new AdminAuthenticationFilter().doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(request).setAttribute("errorTitle", "운영자 권한이 필요합니다.");
        verify(request).setAttribute("primaryActionUrl", "/car-rental/auth/admin/login");
        verify(dispatcher).forward(request, response);
        verify(chain, never()).doFilter(request, response);
    }

    private HttpServletRequest authenticatedRequest(LoginType loginType, long accountId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAuthentication.LOGIN_TYPE)).thenReturn(loginType);
        if (loginType == LoginType.MEMBER) {
            when(session.getAttribute(SessionAuthentication.MEMBER_ID)).thenReturn(accountId);
        } else {
            when(session.getAttribute(SessionAuthentication.ADMIN_ID)).thenReturn(accountId);
        }
        return request;
    }

    private HttpServletRequest guestRequest(String requestUri, String queryString) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/car-rental");
        when(request.getRequestURI()).thenReturn(requestUri);
        when(request.getQueryString()).thenReturn(queryString);
        return request;
    }
}
