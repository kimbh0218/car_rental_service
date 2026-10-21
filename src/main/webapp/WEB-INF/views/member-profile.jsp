<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>내 정보 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
    <div class="header-actions">
        <a class="header-link" href="${pageContext.request.contextPath}/member/reservations">내 예약</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="profile-layout">
    <section class="profile-card" aria-labelledby="page-title">
        <h1 id="page-title">내 정보</h1>
        <p class="intro">가입한 정보를 확인할 수 있습니다.</p>

        <dl class="profile-list">
            <div>
                <dt>이름</dt>
                <dd>${memberName}</dd>
            </div>
            <div>
                <dt>로그인 ID</dt>
                <dd>${loginId}</dd>
            </div>
            <div>
                <dt>운전면허</dt>
                <dd>
                    <span class="status-badge ${licenseStatus ? '' : 'missing'}">${licenseStatus ? '보유' : '미보유'}</span>
                </dd>
            </div>
        </dl>

        <c:if test="${not licenseStatus}">
            <form action="${pageContext.request.contextPath}/member/me/license" method="post">
                <button class="button" type="submit">운전면허 등록</button>
            </form>
        </c:if>
    </section>
</main>

<c:if test="${licenseRequired}">
    <div class="reservation-complete-backdrop" role="presentation">
        <section class="reservation-complete-modal" role="dialog" aria-modal="true"
                 aria-labelledby="license-required-title">
            <p class="eyebrow">LICENSE REQUIRED</p>
            <h2 id="license-required-title">운전면허 등록이 필요합니다.</h2>
            <p>예약을 진행하려면 먼저 운전면허를 등록해 주세요.</p>
            <div class="form-actions">
                <a class="button" href="${pageContext.request.contextPath}/member/me">확인</a>
            </div>
        </section>
    </div>
</c:if>
</body>
</html>
