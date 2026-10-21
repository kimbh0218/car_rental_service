<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
    <c:choose>
        <c:when test="${not empty sessionScope['auth.memberId']}">
            <div class="header-actions">
                <a class="header-link" href="${pageContext.request.contextPath}/cars">차량 목록</a>
                <a class="header-link" href="${pageContext.request.contextPath}/member/reservations">내 예약</a>
                <a class="header-link" href="${pageContext.request.contextPath}/member/me">내 정보</a>
                <form action="${pageContext.request.contextPath}/auth/logout" method="post">
                    <button class="header-link" type="submit">로그아웃</button>
                </form>
            </div>
        </c:when>
        <c:otherwise>
            <div class="header-actions">
                <a class="header-link" href="${pageContext.request.contextPath}/auth/member/signup">회원가입</a>
                <a class="header-link" href="${pageContext.request.contextPath}/auth/member/login">회원 로그인</a>
                <a class="header-link" href="${pageContext.request.contextPath}/auth/admin/login">관리자 로그인</a>
            </div>
        </c:otherwise>
    </c:choose>
</header>

<main class="home-layout">
    <section class="home-hero" aria-labelledby="page-title">
        <p class="eyebrow">CAR RENTAL</p>
        <h1 id="page-title">필요한 시간과 장소에 맞는<br>차량을 찾아보세요.</h1>
        <p>이용 조건으로 바로 찾거나, 전체 차량을 먼저 둘러볼 수 있습니다.</p>
    </section>

    <section class="home-path-grid" aria-label="차량 찾기 방식">
        <a class="home-path-card primary" href="${pageContext.request.contextPath}/parking-lots">
            <span class="home-path-number">01</span>
            <strong>주차장으로 차량 찾기</strong>
            <p>대여 시작·반납 시각과 탑승 인원을 선택하면, 조건에 맞는 주차장과 차량을 찾습니다.</p>
            <b>이용 조건 선택하기</b>
        </a>

        <a class="home-path-card" href="${pageContext.request.contextPath}/cars">
            <span class="home-path-number">02</span>
            <strong>전체 차량 보기</strong>
            <p>차량별 최대 탑승 인원, 1km당 요금, 현재 주차장을 비교해 원하는 차량을 골라보세요.</p>
            <b>차량 목록 보기</b>
        </a>
    </section>

    <section class="home-flow" aria-labelledby="flow-title">
        <h2 id="flow-title">이용 흐름</h2>
        <ol>
            <li><strong>1. 이용 조건 선택</strong><span>시간, 장소, 탑승 인원을 정합니다.</span></li>
            <li><strong>2. 차량 선택</strong><span>조건에 맞는 차량 정보를 비교합니다.</span></li>
            <li><strong>3. 예약 완료</strong><span>예약할 때만 회원 로그인이 필요합니다.</span></li>
        </ol>
    </section>
</main>
</body>
</html>
