<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>회원가입 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
    <a class="header-link" href="${pageContext.request.contextPath}/auth/member/login">로그인</a>
</header>

<main class="auth-layout">
    <section class="auth-card" aria-labelledby="page-title">
        <h1 id="page-title">회원가입</h1>
        <p class="intro">예약에 필요한 기본 정보를 입력해 주세요.</p>

        <form action="${pageContext.request.contextPath}/auth/member/signup" method="post">
            <div class="form-row">
                <label for="name">이름</label>
                <input id="name" name="name" type="text" autocomplete="name" maxlength="100"
                       value="<c:out value='${name}'/>" required>
            </div>

            <div class="form-row">
                <label for="login-id">로그인 ID</label>
                <input id="login-id" name="loginId" type="text" autocomplete="username"
                       minlength="4" maxlength="50" pattern="[A-Za-z0-9._-]+"
                       value="<c:out value='${loginId}'/>" required>
                <span class="hint">영문, 숫자, 점(.), 밑줄(_), 하이픈(-)을 사용할 수 있습니다.</span>
            </div>

            <div class="form-row">
                <label for="password">비밀번호</label>
                <input id="password" name="password" type="password" autocomplete="new-password"
                       minlength="8" required>
                <span class="hint">8자 이상 입력해 주세요.</span>
            </div>

            <div class="form-row">
                <label for="password-confirm">비밀번호 확인</label>
                <input id="password-confirm" name="passwordConfirm" type="password" autocomplete="new-password"
                       minlength="8" required>
            </div>

            <div class="checkbox-row">
                <input id="license-status" name="licenseStatus" type="checkbox" value="true"
                    ${licenseStatus ? 'checked' : ''}>
                <label for="license-status">운전면허를 보유하고 있습니다.</label>
            </div>

            <p class="form-error" role="alert" aria-live="polite"><c:out value="${errorMessage}"/></p>

            <div class="form-actions">
                <button class="button" type="submit">회원가입</button>
                <a class="button secondary" href="${pageContext.request.contextPath}/auth/member/login">취소</a>
            </div>
        </form>
    </section>
</main>
</body>
</html>
