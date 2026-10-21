<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>관리자 로그인 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
    <a class="header-link" href="${pageContext.request.contextPath}/auth/member/login">회원 로그인</a>
</header>

<main class="auth-layout">
    <section class="auth-card" aria-labelledby="page-title">
        <h1 id="page-title">관리자 로그인</h1>
        <p class="intro">운영 관리 기능은 권한이 있는 관리자만 이용할 수 있습니다.</p>

        <form action="${pageContext.request.contextPath}/auth/admin/login" method="post">
            <c:if test="${not empty next}">
                <input name="next" type="hidden" value="<c:out value='${next}'/>">
            </c:if>
            <div class="form-row">
                <label for="login-id">관리자 로그인 ID</label>
                <input id="login-id" name="loginId" type="text" autocomplete="username"
                       minlength="4" maxlength="50" required
                       value="<c:out value='${loginId}'/>">
            </div>

            <div class="form-row">
                <label for="password">비밀번호</label>
                <input id="password" name="password" type="password" autocomplete="current-password"
                       minlength="8" required>
            </div>

            <c:choose>
                <c:when test="${param.reason eq 'login-required'}">
                    <p class="form-error" role="alert" aria-live="polite">운영자 로그인 후 이용할 수 있습니다.</p>
                </c:when>
                <c:otherwise>
                    <p class="form-error" role="alert" aria-live="polite"><c:out value="${errorMessage}"/></p>
                </c:otherwise>
            </c:choose>

            <div class="form-actions">
                <button class="button" type="submit">관리자 로그인</button>
            </div>
        </form>
    </section>
</main>
</body>
</html>
