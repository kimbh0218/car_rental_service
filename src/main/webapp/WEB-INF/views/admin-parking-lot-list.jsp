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
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots/new">주차장 등록</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="car-list-layout">
    <section class="car-page-heading" aria-labelledby="page-title">
        <p class="eyebrow">OPERATOR</p>
        <h1 id="page-title">주차장 관리</h1>
        <p>주차장 수용량과 현재 차량 현황을 확인합니다.</p>
    </section>

    <c:if test="${not empty param.message}">
        <p class="form-message success" role="status"><c:out value="${param.message}"/></p>
    </c:if>
    <c:if test="${not empty param.error}">
        <p class="form-message error" role="alert"><c:out value="${param.error}"/></p>
    </c:if>

    <c:choose>
        <c:when test="${empty parkingLots}">
            <section class="empty-state">
                <h2>등록된 주차장이 없습니다.</h2>
                <p>주차장을 먼저 등록하면 차량을 배치할 수 있습니다.</p>
                <a class="button" href="${pageContext.request.contextPath}/admin/manage/parking-lots/new">주차장 등록</a>
            </section>
        </c:when>
        <c:otherwise>
            <section class="car-grid" aria-label="관리 주차장 목록">
                <c:forEach items="${parkingLots}" var="parkingLot">
                    <article class="car-card">
                        <div class="car-card-content">
                            <div class="car-card-heading">
                                <h2><c:out value="${parkingLot.name}"/></h2>
                                <c:choose>
                                    <c:when test="${not parkingLot.active}">
                                        <span class="car-status unavailable">비활성</span>
                                    </c:when>
                                    <c:when test="${parkingLot.availableParkingCount gt 0}">
                                        <span class="car-status available">
                                            여유 <c:out value="${parkingLot.availableParkingCount}"/>대
                                        </span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="car-status unavailable">여유 없음</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <p class="parking-lot-address"><c:out value="${parkingLot.address}"/></p>

                            <dl class="car-details">
                                <div>
                                    <dt>최대 수용 대수</dt>
                                    <dd><c:out value="${parkingLot.maxParkingCount}"/>대</dd>
                                </div>
                                <div>
                                    <dt>현재 주차 차량</dt>
                                    <dd><c:out value="${parkingLot.currentParkingCount}"/>대</dd>
                                </div>
                                <div>
                                    <dt>대여 중 차량</dt>
                                    <dd><c:out value="${parkingLot.rentedCarCount}"/>대</dd>
                                </div>
                            </dl>

                            <div class="card-actions">
                                <a class="button" href="${pageContext.request.contextPath}/admin/manage/parking-lots/edit?parkingLotId=${parkingLot.id}">수정</a>
                                <form action="${pageContext.request.contextPath}/admin/manage/parking-lots/status" method="post">
                                    <input type="hidden" name="parkingLotId" value="<c:out value='${parkingLot.id}'/>">
                                    <c:choose>
                                        <c:when test="${parkingLot.active}">
                                            <input type="hidden" name="parkingLotStatus" value="INACTIVE">
                                            <button class="button danger" type="submit">주차장 비활성화</button>
                                        </c:when>
                                        <c:otherwise>
                                            <input type="hidden" name="parkingLotStatus" value="ACTIVE">
                                            <button class="button secondary" type="submit">주차장 활성화</button>
                                        </c:otherwise>
                                    </c:choose>
                                </form>
                            </div>
                        </div>
                    </article>
                </c:forEach>
            </section>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
