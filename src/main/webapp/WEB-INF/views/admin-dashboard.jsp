<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>운영자 메인 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/admin">Car Rental</a>
    <div class="header-actions">
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars">차량 관리</a>
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots">주차장 관리</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="admin-dashboard-layout">
    <section class="admin-home-hero" aria-labelledby="page-title">
        <p class="eyebrow">OPERATOR CONSOLE</p>
        <h1 id="page-title">차량과 주차장 운영을<br>한 화면에서 관리하세요.</h1>
        <p>현재 운영 현황을 확인하고, 필요한 관리 업무를 바로 시작할 수 있습니다.</p>
    </section>

    <section class="admin-summary-grid" aria-label="현재 운영 현황">
        <article class="admin-summary-card fleet">
            <span class="admin-summary-label">등록 차량</span>
            <strong>${carCount}<small>대</small></strong>
            <p>현재 관리 중인 전체 차량</p>
        </article>
        <article class="admin-summary-card available">
            <span class="admin-summary-label">대여 가능</span>
            <strong>${availableCarCount}<small>대</small></strong>
            <p>지금 이용 가능한 차량</p>
        </article>
        <article class="admin-summary-card rented">
            <span class="admin-summary-label">대여 진행 중</span>
            <strong>${rentedCarCount}<small>대</small></strong>
            <p>현재 회원이 이용 중인 차량</p>
        </article>
        <article class="admin-summary-card parking">
            <span class="admin-summary-label">운영 주차장</span>
            <strong>${parkingLotCount}<small>곳</small></strong>
            <p>차량을 배치할 수 있는 주차장</p>
        </article>
    </section>

    <section class="admin-work-section" aria-labelledby="car-work-title">
        <div class="admin-work-heading">
            <div>
                <p class="eyebrow">VEHICLE</p>
                <h2 id="car-work-title">차량 운영</h2>
            </div>
            <p>차량을 추가하거나 상태, 위치, 요금을 관리합니다.</p>
        </div>
        <div class="admin-work-grid">
            <a class="admin-work-card vehicle-register" href="${pageContext.request.contextPath}/admin/manage/cars/new">
                <span class="admin-work-number">01</span>
                <strong>새 차량 등록</strong>
                <p>차량명, 탑승 인원, 요금과 최초 주차장을 등록합니다.</p>
                <b>차량 추가하기 <span aria-hidden="true">→</span></b>
            </a>
            <a class="admin-work-card vehicle-manage" href="${pageContext.request.contextPath}/admin/manage/cars">
                <span class="admin-work-number">02</span>
                <strong>차량 정보 관리</strong>
                <p>운행 상태, 현재 주차장, 위치와 1km당 요금을 변경합니다.</p>
                <b>차량 관리하기 <span aria-hidden="true">→</span></b>
            </a>
        </div>
    </section>

    <section class="admin-work-section" aria-labelledby="parking-work-title">
        <div class="admin-work-heading">
            <div>
                <p class="eyebrow">PARKING LOT</p>
                <h2 id="parking-work-title">주차장 운영</h2>
            </div>
            <p>주차장을 등록하고 기본 정보와 수용 대수를 관리합니다.</p>
        </div>
        <div class="admin-work-grid">
            <a class="admin-work-card parking-register" href="${pageContext.request.contextPath}/admin/manage/parking-lots/new">
                <span class="admin-work-number">03</span>
                <strong>새 주차장 등록</strong>
                <p>주차장명, 주소와 차량을 수용할 최대 대수를 등록합니다.</p>
                <b>주차장 추가하기 <span aria-hidden="true">→</span></b>
            </a>
            <a class="admin-work-card parking-manage" href="${pageContext.request.contextPath}/admin/manage/parking-lots">
                <span class="admin-work-number">04</span>
                <strong>주차장 정보 관리</strong>
                <p>주차장 현황을 보고 이름, 주소와 최대 수용 대수를 수정합니다.</p>
                <b>주차장 관리하기 <span aria-hidden="true">→</span></b>
            </a>
        </div>
    </section>
</main>
</body>
</html>
