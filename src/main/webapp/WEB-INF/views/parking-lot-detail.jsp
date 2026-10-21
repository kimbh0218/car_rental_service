<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${parkingLot.name}"/> 차량 선택 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <c:choose>
        <c:when test="${not empty sessionScope['auth.adminId']}">
            <a class="brand" href="${pageContext.request.contextPath}/admin">Car Rental</a>
        </c:when>
        <c:otherwise>
            <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
        </c:otherwise>
    </c:choose>
    <div class="header-actions">
        <a class="header-link" href="${pageContext.request.contextPath}/parking-lots">조건 변경</a>
        <c:choose>
            <c:when test="${not empty sessionScope['auth.adminId']}">
                <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars">차량 관리</a>
                <form action="${pageContext.request.contextPath}/auth/logout" method="post">
                    <button class="header-link" type="submit">로그아웃</button>
                </form>
            </c:when>
            <c:when test="${not empty sessionScope['auth.memberId']}">
                <a class="header-link" href="${pageContext.request.contextPath}/member/reservations">내 예약</a>
                <form action="${pageContext.request.contextPath}/auth/logout" method="post">
                    <button class="header-link" type="submit">로그아웃</button>
                </form>
            </c:when>
            <c:otherwise>
                <a class="header-link" href="${pageContext.request.contextPath}/auth/member/login">회원 로그인</a>
            </c:otherwise>
        </c:choose>
    </div>
</header>

<main class="car-list-layout">
    <section class="car-page-heading" aria-labelledby="page-title">
        <p class="eyebrow">PARKING LOT</p>
        <h1 id="page-title"><c:out value="${parkingLot.name}"/></h1>
        <p><c:out value="${parkingLot.address}"/></p>
    </section>

    <section class="reservation-car-summary parking-lot-condition-summary" aria-label="선택한 이용 조건">
        <span>선택한 이용 조건</span>
        <strong><c:out value="${startTimeDisplay}"/> ~ <c:out value="${returnTimeDisplay}"/></strong>
        <p>탑승 인원 <c:out value="${passengerCount}"/>명</p>
    </section>

    <section class="search-result-heading" aria-labelledby="result-title">
        <h2 id="result-title">대여 가능한 차량</h2>
        <p>선택한 시간대에 이 주차장에서 출발할 수 있는 차량입니다.</p>
    </section>

    <c:choose>
        <c:when test="${empty cars}">
            <section class="empty-state">
                <h2>조건에 맞는 차량이 없습니다.</h2>
                <p>다른 시간대 또는 탑승 인원으로 다시 찾아보세요.</p>
                <a class="button" href="${pageContext.request.contextPath}/parking-lots">조건 변경</a>
            </section>
        </c:when>
        <c:otherwise>
            <section class="car-grid" aria-label="대여 가능한 차량 목록">
                <c:forEach items="${cars}" var="carWithParkingLot">
                    <c:set var="car" value="${carWithParkingLot.car}"/>
                    <article class="car-card">
                        <c:choose>
                            <c:when test="${not empty carWithParkingLot.primaryImage}">
                                <c:url var="carImageUrl" value="/images/cars/${carWithParkingLot.primaryImage.id}">
                                    <c:param name="v" value="${carWithParkingLot.primaryImage.storageKey}"/>
                                </c:url>
                                <img class="car-image"
                                     src="${carImageUrl}"
                                     alt="<c:out value='${car.name}'/> 차량 이미지">
                            </c:when>
                            <c:otherwise>
                                <div class="car-image-placeholder" aria-label="차량 이미지 없음">차량 이미지 없음</div>
                            </c:otherwise>
                        </c:choose>
                        <div class="car-card-content">
                            <div class="car-card-heading">
                                <h2><c:out value="${car.name}"/></h2>
                                <span class="car-status available">대여 가능</span>
                            </div>

                            <dl class="car-details">
                                <div>
                                    <dt>최대 탑승 인원</dt>
                                    <dd><c:out value="${car.maxPassengerCount}"/>명</dd>
                                </div>
                                <div>
                                    <dt>1km당 요금</dt>
                                    <dd><c:out value="${car.pricePerKm}"/>원</dd>
                                </div>
                                <div>
                                    <dt>픽업 위치</dt>
                                    <dd><c:out value="${car.currentAddress}"/></dd>
                                </div>
                            </dl>

                            <c:url var="reservationUrl" value="/member/reservations/new">
                                <c:param name="carId" value="${car.id}"/>
                                <c:param name="startTime" value="${startTime}"/>
                                <c:param name="returnTime" value="${returnTime}"/>
                                <c:param name="passengerCount" value="${passengerCount}"/>
                            </c:url>
                            <c:choose>
                                <c:when test="${not empty sessionScope['auth.memberId'] and sessionScope['auth.memberLicenseStatus']}">
                                    <div class="card-actions">
                                        <a class="button" href="${reservationUrl}">예약하기</a>
                                    </div>
                                </c:when>
                                <c:when test="${not empty sessionScope['auth.memberId']}">
                                    <c:url var="licenseRegistrationUrl" value="/member/me">
                                        <c:param name="licenseRequired" value="true"/>
                                    </c:url>
                                    <div class="card-actions">
                                        <a class="button secondary" href="${licenseRegistrationUrl}">면허 등록 후 예약하기</a>
                                    </div>
                                </c:when>
                                <c:when test="${empty sessionScope['auth.adminId']}">
                                    <c:url var="loginUrl" value="/auth/member/login">
                                        <c:param name="next" value="${reservationUrl}"/>
                                    </c:url>
                                    <div class="card-actions">
                                        <a class="button secondary" href="${loginUrl}">로그인 후 예약</a>
                                    </div>
                                </c:when>
                            </c:choose>
                        </div>
                    </article>
                </c:forEach>
            </section>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
