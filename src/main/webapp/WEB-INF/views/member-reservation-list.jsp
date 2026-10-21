<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>내 예약 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
    <div class="header-actions">
        <a class="header-link" href="${pageContext.request.contextPath}/member/me">내 정보</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="reservation-list-layout">
    <section class="reservation-page-heading" aria-labelledby="page-title">
        <p class="eyebrow">MY RESERVATIONS</p>
        <h1 id="page-title">내 예약</h1>
        <p>예약, 대여 중, 완료된 이용 내역을 확인할 수 있습니다.</p>
    </section>

    <c:if test="${not empty param.message}">
        <p class="form-message success" role="status"><c:out value="${param.message}"/></p>
    </c:if>
    <c:if test="${not empty param.error}">
        <p class="form-message error" role="alert"><c:out value="${param.error}"/></p>
    </c:if>

    <c:choose>
        <c:when test="${empty rentals}">
            <section class="empty-state">
                <h2>예약 내역이 없습니다.</h2>
                <p>차량 목록에서 원하는 차량을 예약해 보세요.</p>
                <a class="button" href="${pageContext.request.contextPath}/cars">차량 목록 보기</a>
            </section>
        </c:when>
        <c:otherwise>
            <section class="reservation-list" aria-label="내 예약 목록">
                <c:forEach items="${rentals}" var="rental">
                    <article class="reservation-item">
                        <div class="reservation-item-heading">
                            <div>
                                <h2><c:out value="${rental.carName}"/></h2>
                                <p><c:out value="${rental.rentalPeriod}"/></p>
                            </div>
                            <c:choose>
                                <c:when test="${rental.rentalStatus eq 'RESERVED'}">
                                    <span class="reservation-status reserved">예약됨</span>
                                </c:when>
                                <c:when test="${rental.rentalStatus eq 'IN_PROGRESS'}">
                                    <span class="reservation-status in-progress">대여 중</span>
                                </c:when>
                                <c:when test="${rental.rentalStatus eq 'COMPLETED'}">
                                    <span class="reservation-status completed">반납 완료</span>
                                </c:when>
                                <c:when test="${rental.rentalStatus eq 'CANCELED'}">
                                    <span class="reservation-status canceled">취소됨</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="reservation-status canceled">노쇼 자동 취소</span>
                                </c:otherwise>
                            </c:choose>
                        </div>

                        <dl class="reservation-details">
                            <div>
                                <dt>현재 주차장</dt>
                                <dd>
                                    <c:choose>
                                        <c:when test="${rental.rentalStatus eq 'IN_PROGRESS'}">대여 중</c:when>
                                        <c:when test="${empty rental.currentParkingLotName}">확인 중</c:when>
                                        <c:otherwise><c:out value="${rental.currentParkingLotName}"/></c:otherwise>
                                    </c:choose>
                                </dd>
                            </div>
                            <div>
                                <dt>현재 위치</dt>
                                <dd>
                                    <c:choose>
                                        <c:when test="${rental.rentalStatus eq 'IN_PROGRESS'}">대여 중</c:when>
                                        <c:when test="${empty rental.currentAddress}">확인 중</c:when>
                                        <c:otherwise><c:out value="${rental.currentAddress}"/></c:otherwise>
                                    </c:choose>
                                </dd>
                            </div>
                            <div>
                                <dt>대여 인원</dt>
                                <dd><c:out value="${rental.passengersCount}"/>명</dd>
                            </div>
                        </dl>

                        <div class="reservation-actions">
                            <c:if test="${rental.rentalStatus eq 'RESERVED'}">
                                <form action="${pageContext.request.contextPath}/member/reservations/cancel" method="post">
                                    <input type="hidden" name="rentalId" value="<c:out value='${rental.rentalId}'/>">
                                    <button class="button danger" type="submit">예약 취소</button>
                                </form>
                                <form action="${pageContext.request.contextPath}/member/rentals/start" method="post">
                                    <input type="hidden" name="rentalId" value="<c:out value='${rental.rentalId}'/>">
                                    <button class="button" type="submit"
                                            data-rental-start-button
                                            data-rental-id="<c:out value='${rental.rentalId}'/>"
                                            data-start-time="<c:out value='${rental.startTime}'/>"
                                            ${rental.startAvailable ? '' : 'disabled'}>대여 시작</button>
                                    <p class="rental-start-hint"
                                       data-rental-start-hint="<c:out value='${rental.rentalId}'/>"
                                       ${rental.startAvailable ? 'hidden' : ''}>
                                        예약 시작 시각부터 대여를 시작할 수 있습니다.
                                    </p>
                                </form>
                            </c:if>
                            <c:if test="${rental.rentalStatus eq 'IN_PROGRESS'}">
                                <form action="${pageContext.request.contextPath}/member/rentals/return" method="post">
                                    <input type="hidden" name="rentalId" value="<c:out value='${rental.rentalId}'/>">
                                    <button class="button" type="submit">반납</button>
                                </form>
                            </c:if>
                        </div>
                    </article>
                </c:forEach>
            </section>
        </c:otherwise>
    </c:choose>
</main>
<script>
    (() => {
        const startButtons = document.querySelectorAll("[data-rental-start-button]");

        const updateStartButtons = () => {
            const now = Date.now();

            startButtons.forEach((button) => {
                const startAt = new Date(button.dataset.startTime).getTime();
                const canStart = Number.isFinite(startAt) && now >= startAt;
                const hint = document.querySelector(
                    '[data-rental-start-hint="' + button.dataset.rentalId + '"]'
                );

                button.disabled = !canStart;
                if (hint) {
                    hint.hidden = canStart;
                }
            });
        };

        updateStartButtons();
        window.setInterval(updateStartButtons, 1000);
    })();
</script>
</body>
</html>
