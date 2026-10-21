package org.example.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.web.auth.LoginType;
import org.example.web.auth.SessionAuthentication;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationStateFilterTest {

    @Test
    void marksRequestAsGuestWithoutSession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getSession(false)).thenReturn(null);

        new AuthenticationStateFilter().doFilter(request, response, chain);

        verify(request).setAttribute("authState", "guest");
        verify(chain).doFilter(request, response);
    }

    @Test
    void marksRequestAsMemberForMemberSession() throws Exception {
        HttpServletRequest request = authenticatedRequest(LoginType.MEMBER, 1L);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new AuthenticationStateFilter().doFilter(request, response, chain);

        verify(request).setAttribute("authState", "member");
        verify(chain).doFilter(request, response);
    }

    @Test
    void marksRequestAsAdminForAdministratorSession() throws Exception {
        HttpServletRequest request = authenticatedRequest(LoginType.ADMIN, 2L);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new AuthenticationStateFilter().doFilter(request, response, chain);

        verify(request).setAttribute("authState", "admin");
        verify(chain).doFilter(request, response);
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
}
