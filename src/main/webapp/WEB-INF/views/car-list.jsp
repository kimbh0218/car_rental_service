<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>차량 목록 | Car Rental</title>
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
    <c:choose>
        <c:when test="${not empty sessionScope['auth.adminId']}">
            <div class="header-actions">
                <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots">주차장 목록</a>
                <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars/new">차량 등록</a>
                <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars">차량 관리</a>
                <form action="${pageContext.request.contextPath}/auth/logout" method="post">
                    <button class="header-link" type="submit">로그아웃</button>
                </form>
            </div>
        </c:when>
        <c:when test="${not empty sessionScope['auth.memberId']}">
            <div class="header-actions">
                <a class="header-link" href="${pageContext.request.contextPath}/parking-lots">주차장으로 찾기</a>
                <a class="header-link" href="${pageContext.request.contextPath}/member/me">내 정보</a>
                <a class="header-link" href="${pageContext.request.contextPath}/member/reservations">내 예약</a>
                <form action="${pageContext.request.contextPath}/auth/logout" method="post">
                    <button class="header-link" type="submit">로그아웃</button>
                </form>
            </div>
        </c:when>
        <c:otherwise>
            <div class="header-actions">
                <a class="header-link" href="${pageContext.request.contextPath}/parking-lots">주차장으로 찾기</a>
                <a class="header-link" href="${pageContext.request.contextPath}/auth/member/login">회원 로그인</a>
                <a class="header-link" href="${pageContext.request.contextPath}/auth/admin/login">관리자 로그인</a>
            </div>
        </c:otherwise>
    </c:choose>
</header>

<main class="car-list-layout">
    <section class="car-page-heading" aria-labelledby="page-title">
        <p class="eyebrow">CAR RENTAL</p>
        <h1 id="page-title">차량 목록</h1>
        <p>차량 상태와 현재 위치를 확인하고, 원하는 시간대에 예약하세요.</p>
    </section>

    <c:choose>
        <c:when test="${empty cars}">
            <section class="empty-state" aria-live="polite">
                <h2>등록된 차량이 없습니다.</h2>
                <p>차량이 등록되면 이곳에서 확인할 수 있습니다.</p>
            </section>
        </c:when>
        <c:otherwise>
            <section class="car-grid" aria-label="차량 목록">
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
                                <c:choose>
                                    <c:when test="${car.carStatus eq 'AVAILABLE'}">
                                        <span class="car-status available">이용 가능</span>
                                    </c:when>
                                    <c:when test="${car.carStatus eq 'RENTED'}">
                                        <span class="car-status rented">대여 중</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="car-status unavailable">이용 불가</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <c:if test="${car.carStatus eq 'MAINTENANCE'}">
                                <p class="car-unavailable-reason">이용 불가 사유: 차량 정비 중</p>
                            </c:if>
                            <c:if test="${car.carStatus eq 'INACTIVE'}">
                                <p class="car-unavailable-reason">이용 불가 사유: 현재 운영하지 않는 차량</p>
                            </c:if>

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
                                    <dt>현재 주차장</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${empty carWithParkingLot.parkingLotName}">미지정</c:when>
                                            <c:otherwise><c:out value="${carWithParkingLot.parkingLotName}"/></c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                                <div>
                                    <dt>현재 위치</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${empty car.currentAddress}">확인 중</c:when>
                                            <c:otherwise><c:out value="${car.currentAddress}"/></c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                            </dl>

                            <c:if test="${car.carStatus eq 'AVAILABLE'}">
                                <c:url var="reservationUrl" value="/member/reservations/new">
                                    <c:param name="carId" value="${car.id}"/>
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
                                        <div class="card-actions">
                                            <a class="button secondary" href="${pageContext.request.contextPath}/auth/member/login">로그인 후 예약</a>
                                        </div>
                                    </c:when>
                                </c:choose>
                            </c:if>
                        </div>
                    </article>
                </c:forEach>
            </section>
        </c:otherwise>
    </c:choose>
</main>

<c:if test="${param.reservation eq 'created'}">
    <div class="reservation-complete-backdrop" role="presentation">
        <section class="reservation-complete-modal" role="dialog" aria-modal="true"
                 aria-labelledby="reservation-complete-title">
            <p class="eyebrow">RESERVATION COMPLETE</p>
            <h2 id="reservation-complete-title">예약이 완료되었습니다.</h2>
            <p>예약 정보를 확인하려면 내 예약으로 이동하세요.</p>
            <div class="form-actions">
                <a class="button" href="${pageContext.request.contextPath}/member/reservations">내 예약 보기</a>
                <a class="button secondary" href="${pageContext.request.contextPath}/cars">차량 목록 계속 보기</a>
            </div>
        </section>
    </div>
</c:if>
</body>
</html>
