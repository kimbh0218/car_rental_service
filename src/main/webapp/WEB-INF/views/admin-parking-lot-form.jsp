<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>주차장 등록 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/admin">Car Rental</a>
    <div class="header-actions">
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars">차량 관리</a>
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots">주차장 목록</a>
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars/new">차량 등록</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="auth-layout">
    <section class="auth-card admin-form-card" aria-labelledby="page-title">
        <p class="eyebrow">OPERATOR</p>
        <h1 id="page-title">주차장 등록</h1>
        <p class="intro">주차장 기본 정보와 수용 가능 차량 수를 등록합니다.</p>

        <form action="${pageContext.request.contextPath}/admin/manage/parking-lots/new" method="post">
            <div class="form-row">
                <label for="name">주차장명</label>
                <input id="name" name="name" type="text" maxlength="100" required
                       value="<c:out value='${name}'/>">
            </div>

            <div class="form-row">
                <label for="address">주소</label>
                <input id="address" name="address" type="text" maxlength="255" required
                       value="<c:out value='${address}'/>">
            </div>

            <div class="form-row">
                <label for="max-parking-count">최대 주차 가능 대수</label>
                <input id="max-parking-count" name="maxParkingCount" type="number"
                       min="1" required value="<c:out value='${maxParkingCount}'/>">
                <span class="hint">현재 주차 차량 수와 대여 차량 수는 0대로 시작합니다.</span>
            </div>

            <p class="form-error" role="alert" aria-live="polite"><c:out value="${errorMessage}"/></p>

            <div class="form-actions">
                <button class="button" type="submit">주차장 등록</button>
                <a class="button secondary" href="${pageContext.request.contextPath}/admin/manage/cars">차량 관리로</a>
            </div>
        </form>
    </section>
</main>
</body>
</html>
