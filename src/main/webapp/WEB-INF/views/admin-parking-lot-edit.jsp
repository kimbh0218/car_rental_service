<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>주차장 관리 | Car Rental</title>
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
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots/new">주차장 등록</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="auth-layout">
    <section class="auth-card admin-form-card" aria-labelledby="page-title">
        <p class="eyebrow">OPERATOR</p>
        <h1 id="page-title">주차장 관리</h1>
        <p class="intro">주차장 기본 정보와 최대 수용 대수를 수정합니다.</p>

        <form action="${pageContext.request.contextPath}/admin/manage/parking-lots/edit" method="post">
            <input type="hidden" name="parkingLotId" value="<c:out value='${parkingLotId}'/>">

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
                <label for="max-parking-count">최대 수용 대수</label>
                <input id="max-parking-count" name="maxParkingCount" type="number"
                       min="1" required value="<c:out value='${maxParkingCount}'/>">
                <c:if test="${not empty occupiedCarCount}">
                    <span class="hint">현재 주차·대여 차량 합계는 <c:out value="${occupiedCarCount}"/>대입니다.</span>
                </c:if>
            </div>

            <p class="form-error" role="alert" aria-live="polite"><c:out value="${errorMessage}"/></p>

            <div class="form-actions">
                <button class="button" type="submit">저장</button>
                <a class="button secondary" href="${pageContext.request.contextPath}/admin/manage/parking-lots">목록으로</a>
            </div>
        </form>
    </section>
</main>
</body>
</html>
