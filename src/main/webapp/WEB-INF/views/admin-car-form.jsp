<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>차량 등록 | Car Rental</title>
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
        <h1 id="page-title">차량 등록</h1>
        <p class="intro">차량 기본 정보와 최초 주차 위치를 등록합니다.</p>

        <form action="${pageContext.request.contextPath}/admin/manage/cars/new" method="post"
              enctype="multipart/form-data">
            <div class="form-row">
                <label for="name">차량명</label>
                <input id="name" name="name" type="text" maxlength="100" required
                       value="<c:out value='${name}'/>">
            </div>

            <div class="form-row">
                <label for="max-passenger-count">최대 탑승 인원</label>
                <input id="max-passenger-count" name="maxPassengerCount" type="number"
                       min="1" required value="<c:out value='${maxPassengerCount}'/>">
            </div>

            <div class="form-row">
                <label for="price-per-km">1km당 요금</label>
                <input id="price-per-km" name="pricePerKm" type="number"
                       min="1" step="1" required value="<c:out value='${pricePerKm}'/>">
                <span class="hint">정수 단위 원으로 입력하세요.</span>
            </div>

            <div class="form-row">
                <label for="image">차량 이미지</label>
                <input id="image" name="image" type="file" accept="image/jpeg,image/png">
                <span class="hint">선택 사항입니다. JPEG·PNG, 최대 5MB, 최대 4096px</span>
            </div>

            <div class="form-row">
                <label for="parking-lot-id">등록할 주차장</label>
                <select id="parking-lot-id" name="parkingLotId" required>
                    <option value="" disabled ${empty parkingLotId ? 'selected' : ''}>주차장을 선택하세요</option>
                    <c:forEach items="${parkingLots}" var="parkingLot">
                        <c:if test="${parkingLot.availableParkingCount gt 0}">
                            <option value="<c:out value='${parkingLot.id}'/>"
                                    ${parkingLot.id eq parkingLotId ? 'selected' : ''}>
                                <c:out value="${parkingLot.name}"/> ·
                                <c:out value="${parkingLot.address}"/> ·
                                여유 <c:out value="${parkingLot.availableParkingCount}"/>대
                            </option>
                        </c:if>
                    </c:forEach>
                </select>
                <span class="hint">주차 가능 공간은 등록 시점에 다시 확인됩니다.</span>
            </div>

            <p class="form-error" role="alert" aria-live="polite"><c:out value="${errorMessage}"/></p>

            <div class="form-actions">
                <button class="button" type="submit">차량 등록</button>
                <a class="button secondary" href="${pageContext.request.contextPath}/cars">목록으로</a>
            </div>
        </form>
    </section>
</main>
</body>
</html>
