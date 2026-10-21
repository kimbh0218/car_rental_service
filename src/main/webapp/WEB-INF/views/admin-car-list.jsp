<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>차량 관리 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/admin">Car Rental</a>
    <div class="header-actions">
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars/new">차량 등록</a>
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots">주차장 목록</a>
        <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots/new">주차장 등록</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="admin-car-list-layout">
    <section class="reservation-page-heading" aria-labelledby="page-title">
        <p class="eyebrow">OPERATOR</p>
        <h1 id="page-title">차량 관리</h1>
        <p>차량 상태와 현재 위치를 확인하고, 정비 상태와 주차장을 관리합니다.</p>
    </section>

    <c:if test="${not empty param.message}">
        <p class="form-message success" role="status"><c:out value="${param.message}"/></p>
    </c:if>
    <c:if test="${not empty param.error}">
        <p class="form-message error" role="alert"><c:out value="${param.error}"/></p>
    </c:if>

    <c:choose>
        <c:when test="${empty cars}">
            <section class="empty-state">
                <h2>등록된 차량이 없습니다.</h2>
                <p>차량 등록 후 이곳에서 상태를 관리할 수 있습니다.</p>
                <a class="button" href="${pageContext.request.contextPath}/admin/manage/cars/new">차량 등록</a>
            </section>
        </c:when>
        <c:otherwise>
            <section class="admin-car-grid" aria-label="관리 차량 목록">
                <c:forEach items="${cars}" var="carWithParkingLot">
                    <c:set var="car" value="${carWithParkingLot.car}"/>
                    <article class="admin-car-card">
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

                        <div class="admin-car-card-content">
                            <div class="admin-car-card-heading">
                                <div>
                                    <h2><c:out value="${car.name}"/></h2>
                                    <p>최대 <c:out value="${car.maxPassengerCount}"/>명</p>
                                    <p>1km당 <c:out value="${car.pricePerKm}"/>원</p>
                                </div>
                                <c:choose>
                                    <c:when test="${car.carStatus eq 'AVAILABLE'}">
                                        <span class="reservation-status completed">운영 가능</span>
                                    </c:when>
                                    <c:when test="${car.carStatus eq 'MAINTENANCE'}">
                                        <span class="reservation-status maintenance">정비 중</span>
                                    </c:when>
                                    <c:when test="${car.carStatus eq 'INACTIVE'}">
                                        <span class="reservation-status canceled">비활성</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="reservation-status in-progress">대여 중</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <dl class="reservation-details">
                                <div>
                                    <dt>현재 주차장</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${empty carWithParkingLot.parkingLotName}">대여 중</c:when>
                                            <c:otherwise><c:out value="${carWithParkingLot.parkingLotName}"/></c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                                <div>
                                    <dt>현재 위치</dt>
                                    <dd>
                                        <c:choose>
                                            <c:when test="${empty car.currentAddress}">대여 중</c:when>
                                            <c:otherwise><c:out value="${car.currentAddress}"/></c:otherwise>
                                        </c:choose>
                                    </dd>
                                </div>
                            </dl>

                            <form class="status-change-form"
                                  action="${pageContext.request.contextPath}/admin/manage/cars/image"
                                  method="post" enctype="multipart/form-data">
                                <input type="hidden" name="carId" value="<c:out value='${car.id}'/>">
                                <label for="image-${car.id}">차량 이미지</label>
                                <div>
                                    <input id="image-${car.id}" name="image" type="file"
                                           accept="image/jpeg,image/png" required>
                                    <button class="button" type="submit">
                                        ${empty carWithParkingLot.primaryImage ? '이미지 등록' : '이미지 변경'}
                                    </button>
                                </div>
                                <span class="hint">JPEG·PNG, 최대 5MB</span>
                            </form>

                            <form class="status-change-form"
                                  action="${pageContext.request.contextPath}/admin/manage/cars/price"
                                  method="post">
                                <input type="hidden" name="carId" value="<c:out value='${car.id}'/>">
                                <label for="price-per-km-${car.id}">1km당 요금</label>
                                <div>
                                    <input id="price-per-km-${car.id}" name="pricePerKm" type="number"
                                           min="1" step="1" required
                                           value="<c:out value='${car.pricePerKm}'/>">
                                    <button class="button" type="submit">요금 변경</button>
                                </div>
                            </form>

                            <c:choose>
                                <c:when test="${car.carStatus eq 'RENTED'}">
                                    <p class="admin-car-notice">대여 중인 차량은 상태와 주차장을 변경할 수 없습니다.</p>
                                </c:when>
                                <c:otherwise>
                                    <form class="status-change-form"
                                          action="${pageContext.request.contextPath}/admin/manage/cars/status"
                                          method="post">
                                        <input type="hidden" name="carId" value="<c:out value='${car.id}'/>">
                                        <label for="status-${car.id}">차량 상태</label>
                                        <div>
                                            <select id="status-${car.id}" name="carStatus">
                                                <option value="AVAILABLE" ${car.carStatus eq 'AVAILABLE' ? 'selected' : ''}>운영 가능</option>
                                                <option value="MAINTENANCE" ${car.carStatus eq 'MAINTENANCE' ? 'selected' : ''}>정비 중</option>
                                                <option value="INACTIVE" ${car.carStatus eq 'INACTIVE' ? 'selected' : ''}>비활성</option>
                                            </select>
                                            <button class="button" type="submit">변경</button>
                                        </div>
                                    </form>

                                    <form class="status-change-form"
                                          action="${pageContext.request.contextPath}/admin/manage/cars/parking-lot"
                                          method="post">
                                        <input type="hidden" name="carId" value="<c:out value='${car.id}'/>">
                                        <label for="parking-lot-${car.id}">현재 주차장</label>
                                        <div>
                                            <select id="parking-lot-${car.id}" name="parkingLotId">
                                                <c:forEach items="${parkingLots}" var="parkingLot">
                                                    <option value="<c:out value='${parkingLot.id}'/>"
                                                        <c:if test="${parkingLot.id eq car.currentParkingLotId}">selected</c:if>
                                                        <c:if test="${parkingLot.id ne car.currentParkingLotId and (not parkingLot.active or parkingLot.availableParkingCount le 0)}">disabled</c:if>>
                                                        <c:out value="${parkingLot.name}"/> · <c:out value="${parkingLot.address}"/>
                                                        <c:if test="${parkingLot.id ne car.currentParkingLotId and not parkingLot.active}">(비활성)</c:if>
                                                        <c:if test="${parkingLot.id ne car.currentParkingLotId and parkingLot.active and parkingLot.availableParkingCount le 0}">(여유 없음)</c:if>
                                                    </option>
                                                </c:forEach>
                                            </select>
                                            <button class="button" type="submit">이동</button>
                                        </div>
                                    </form>
                                </c:otherwise>
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
