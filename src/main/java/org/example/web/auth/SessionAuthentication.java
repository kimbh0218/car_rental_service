package org.example.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.domain.Admin;
import org.example.domain.Member;

import java.util.Optional;

public final class SessionAuthentication {
    public static final String LOGIN_TYPE = "auth.loginType";
    public static final String MEMBER_ID = "auth.memberId";
    public static final String MEMBER_LICENSE_STATUS = "auth.memberLicenseStatus";
    public static final String ADMIN_ID = "auth.adminId";

    private SessionAuthentication() {
    }

    public static void loginMember(HttpServletRequest request, Member member) {
        HttpSession session = renewSession(request);
        session.setAttribute(LOGIN_TYPE, LoginType.MEMBER);
        session.setAttribute(MEMBER_ID, member.getId());
        session.setAttribute(MEMBER_LICENSE_STATUS, member.isLicenseStatus());
    }

    public static void loginAdmin(HttpServletRequest request, Admin admin) {
        HttpSession session = renewSession(request);
        session.setAttribute(LOGIN_TYPE, LoginType.ADMIN);
        session.setAttribute(ADMIN_ID, admin.getId());
    }

    public static boolean isMemberAuthenticated(HttpServletRequest request) {
        return loginType(request).filter(LoginType.MEMBER::equals).isPresent()
                && memberId(request).isPresent();
    }

    public static boolean isAdminAuthenticated(HttpServletRequest request) {
        return loginType(request).filter(LoginType.ADMIN::equals).isPresent()
                && adminId(request).isPresent();
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static void updateMemberLicenseStatus(
            HttpServletRequest request,
            boolean licenseStatus
    ) {
        HttpSession session = request.getSession(false);
        if (session != null && isMemberAuthenticated(request)) {
            session.setAttribute(MEMBER_LICENSE_STATUS, licenseStatus);
        }
    }

    private static HttpSession renewSession(HttpServletRequest request) {
        HttpSession currentSession = request.getSession(false);
        if (currentSession != null) {
            currentSession.invalidate();
        }
        return request.getSession(true);
    }

    private static Optional<LoginType> loginType(HttpServletRequest request) {
        return attribute(request, LOGIN_TYPE, LoginType.class);
    }

    private static Optional<Long> memberId(HttpServletRequest request) {
        return attribute(request, MEMBER_ID, Long.class);
    }

    private static Optional<Long> adminId(HttpServletRequest request) {
        return attribute(request, ADMIN_ID, Long.class);
    }

    private static <T> Optional<T> attribute(HttpServletRequest request, String name, Class<T> type) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(name);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }
}
