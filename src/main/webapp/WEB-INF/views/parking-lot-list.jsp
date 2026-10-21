<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>주차장으로 찾기 | Car Rental</title>
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
                <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/cars">차량 관리</a>
                <a class="header-link" href="${pageContext.request.contextPath}/admin/manage/parking-lots">주차장 목록</a>
                <form action="${pageContext.request.contextPath}/auth/logout" method="post">
                    <button class="header-link" type="submit">로그아웃</button>
                </form>
            </div>
        </c:when>
        <c:when test="${not empty sessionScope['auth.memberId']}">
            <div class="header-actions">
                <a class="header-link" href="${pageContext.request.contextPath}/cars">차량 목록</a>
                <a class="header-link" href="${pageContext.request.contextPath}/member/reservations">내 예약</a>
                <form action="${pageContext.request.contextPath}/auth/logout" method="post">
                    <button class="header-link" type="submit">로그아웃</button>
                </form>
            </div>
        </c:when>
        <c:otherwise>
            <div class="header-actions">
                <a class="header-link" href="${pageContext.request.contextPath}/auth/member/login">회원 로그인</a>
                <a class="header-link" href="${pageContext.request.contextPath}/auth/admin/login">관리자 로그인</a>
            </div>
        </c:otherwise>
    </c:choose>
</header>

<main class="car-list-layout">
    <section class="car-page-heading" aria-labelledby="page-title">
        <p class="eyebrow">PARKING LOT</p>
        <h1 id="page-title">주차장으로 차량 찾기</h1>
        <p>이용 조건을 먼저 선택하면, 대여 가능한 주차장과 차량 수를 확인할 수 있습니다.</p>
    </section>

    <section class="parking-search-card" aria-labelledby="search-title">
        <h2 id="search-title">이용 조건 선택</h2>
        <p>시간은 10분 단위로 선택할 수 있으며, 대여 시간은 최소 30분입니다.</p>

        <form class="parking-search-form" action="${pageContext.request.contextPath}/parking-lots" method="get">
            <div class="form-row">
                <span class="field-label">대여 시작 일시</span>
                <div class="date-time-fields">
                    <input id="start-date" type="date" required aria-label="대여 시작 날짜">
                    <select id="start-hour" required aria-label="대여 시작 시"></select>
                    <select id="start-minute" required aria-label="대여 시작 분"></select>
                </div>
                <input id="start-time" name="startTime" type="hidden" value="<c:out value='${startTime}'/>">
            </div>

            <div class="form-row">
                <span class="field-label">반납 예정 일시</span>
                <div class="date-time-fields">
                    <input id="return-date" type="date" required aria-label="반납 예정 날짜">
                    <select id="return-hour" required aria-label="반납 예정 시"></select>
                    <select id="return-minute" required aria-label="반납 예정 분"></select>
                </div>
                <input id="return-time" name="returnTime" type="hidden" value="<c:out value='${returnTime}'/>">
            </div>

            <div class="parking-search-actions">
                <label for="passenger-count">탑승 인원</label>
                <div class="passenger-picker" data-max-passengers="9">
                    <input id="passenger-count" name="passengerCount" type="number"
                           min="1" max="9" required inputmode="numeric"
                           value="<c:out value='${passengerCount}'/>">
                    <button class="passenger-picker-toggle" type="button"
                            aria-label="탑승 인원 선택" aria-expanded="false">선택</button>
                    <div class="passenger-options" hidden aria-label="탑승 인원 선택"></div>
                </div>
                <button class="button" type="submit">찾기</button>
            </div>

            <c:choose>
                <c:when test="${not empty errorMessage}">
                    <p class="form-error" role="alert" aria-live="polite"><c:out value="${errorMessage}"/></p>
                </c:when>
                <c:otherwise>
                    <p class="form-error" role="alert" aria-live="polite"><c:out value="${param.error}"/></p>
                </c:otherwise>
            </c:choose>
        </form>
    </section>

    <c:choose>
        <c:when test="${searched and empty errorMessage}">
            <section class="search-result-heading" aria-labelledby="result-title">
                <h2 id="result-title">주차장 검색 결과</h2>
                <p>각 주차장의 대여 가능 차량 수를 확인하세요.</p>
            </section>

            <section class="car-grid" aria-label="조건별 주차장 목록">
                <c:forEach items="${parkingLotAvailability}" var="availability">
                    <c:set var="parkingLot" value="${availability.parkingLot}"/>
                    <article class="car-card">
                        <div class="car-card-content">
                            <div class="car-card-heading">
                                <h2><c:out value="${parkingLot.name}"/></h2>
                                <c:choose>
                                    <c:when test="${availability.rentalAvailable}">
                                        <span class="car-status available">대여 가능 <c:out value="${availability.availableCarCount}"/>대</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="car-status unavailable">대여 불가</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>

                            <p class="parking-lot-address"><c:out value="${parkingLot.address}"/></p>
                            <c:if test="${availability.rentalAvailable}">
                                <p class="parking-lot-available-cars">
                                    대여 가능 차량:
                                    <c:forEach items="${availability.availableCarNames}" var="carName" varStatus="status">
                                        <c:if test="${not status.first}"> · </c:if><c:out value="${carName}"/>
                                    </c:forEach>
                                </p>
                            </c:if>

                            <c:choose>
                                <c:when test="${availability.rentalAvailable}">
                                    <c:url var="parkingLotDetailUrl" value="/parking-lots/detail">
                                        <c:param name="parkingLotId" value="${parkingLot.id}"/>
                                        <c:param name="startTime" value="${startTime}"/>
                                        <c:param name="returnTime" value="${returnTime}"/>
                                        <c:param name="passengerCount" value="${passengerCount}"/>
                                    </c:url>
                                    <c:choose>
                                        <c:when test="${not empty sessionScope['auth.memberId']}">
                                            <div class="card-actions">
                                                <a class="button" href="${parkingLotDetailUrl}">이 주차장에서 대여</a>
                                            </div>
                                        </c:when>
                                        <c:when test="${empty sessionScope['auth.adminId']}">
                                            <c:url var="loginUrl" value="/auth/member/login">
                                                <c:param name="next" value="${parkingLotDetailUrl}"/>
                                            </c:url>
                                            <div class="card-actions">
                                                <a class="button secondary" href="${loginUrl}">로그인 후 예약</a>
                                            </div>
                                        </c:when>
                                    </c:choose>
                                </c:when>
                                <c:otherwise>
                                    <div class="card-actions">
                                        <button class="button" type="button" disabled>대여 가능한 차량 없음</button>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </article>
                </c:forEach>
            </section>
        </c:when>
        <c:otherwise>
            <section class="empty-state parking-search-guide">
                <h2>대여 조건을 선택해주세요.</h2>
                <p>조건에 맞는 차량이 있는 주차장만 바로 대여할 수 있습니다.</p>
            </section>
        </c:otherwise>
    </c:choose>
</main>
<script>
    (() => {
        const form = document.querySelector(".parking-search-form");
        const startDate = document.getElementById("start-date");
        const startHour = document.getElementById("start-hour");
        const startMinute = document.getElementById("start-minute");
        const startTime = document.getElementById("start-time");
        const returnDate = document.getElementById("return-date");
        const returnHour = document.getElementById("return-hour");
        const returnMinute = document.getElementById("return-minute");
        const returnTime = document.getElementById("return-time");

        const pad = (value) => String(value).padStart(2, "0");
        const dateValue = (date) => date.getFullYear() + "-"
                + pad(date.getMonth() + 1) + "-" + pad(date.getDate());
        const today = () => dateValue(new Date());
        const minutes = ["00", "10", "20", "30", "40", "50"];

        const addHourOptions = (select, label) => {
            const placeholder = new Option(label + "을 선택하세요", "");
            placeholder.disabled = true;
            placeholder.selected = true;
            select.add(placeholder);

            for (let hour = 0; hour < 24; hour += 1) {
                const value = pad(hour);
                select.add(new Option(value + "시", value));
            }
        };

        const addMinuteOptions = (select, label) => {
            const placeholder = new Option(label + "을 선택하세요", "");
            placeholder.disabled = true;
            placeholder.selected = true;
            select.add(placeholder);

            minutes.forEach((minute) => select.add(new Option(minute + "분", minute)));
        };

        const restoreDateTime = (hidden, dateInput, hourSelect, minuteSelect) => {
            if (hidden.value.length >= 16) {
                dateInput.value = hidden.value.slice(0, 10);
                hourSelect.value = hidden.value.slice(11, 13);
                minuteSelect.value = hidden.value.slice(14, 16);
            }
        };

        const selectedDateTime = (dateInput, hourSelect, minuteSelect) => {
            if (!dateInput.value || !hourSelect.value || !minuteSelect.value) {
                return null;
            }
            return new Date(dateInput.value + "T" + hourSelect.value
                    + ":" + minuteSelect.value + ":00");
        };

        const setDateTime = (dateInput, hourSelect, minuteSelect, date) => {
            dateInput.value = dateValue(date);
            hourSelect.value = pad(date.getHours());
            minuteSelect.value = pad(date.getMinutes());
        };

        const nextTenMinute = () => {
            const date = new Date();
            date.setSeconds(0, 0);
            date.setMinutes(date.getMinutes() + (10 - date.getMinutes() % 10));
            return date;
        };

        const hasDateTime = (dateInput, hourSelect, minuteSelect) =>
            Boolean(dateInput.value && hourSelect.value && minuteSelect.value);

        const setSelectableOptions = (
                dateInput,
                hourSelect,
                minuteSelect,
                isAllowed
        ) => {
            for (const hourOption of hourSelect.options) {
                if (!hourOption.value) {
                    continue;
                }
                hourOption.disabled = !dateInput.value || !minutes.some((minute) =>
                    isAllowed(new Date(dateInput.value + "T" + hourOption.value
                            + ":" + minute + ":00"))
                );
            }

            if (hourSelect.selectedOptions[0]?.disabled) {
                hourSelect.value = "";
                minuteSelect.value = "";
            }

            for (const minuteOption of minuteSelect.options) {
                if (!minuteOption.value) {
                    continue;
                }
                minuteOption.disabled = !dateInput.value || !hourSelect.value
                        || !isAllowed(new Date(dateInput.value + "T" + hourSelect.value
                                + ":" + minuteOption.value + ":00"));
            }

            if (minuteSelect.selectedOptions[0]?.disabled) {
                minuteSelect.value = "";
            }
        };

        const syncHiddenValues = () => {
            startTime.value = hasDateTime(startDate, startHour, startMinute)
                    ? startDate.value + "T" + startHour.value + ":" + startMinute.value : "";
            returnTime.value = hasDateTime(returnDate, returnHour, returnMinute)
                    ? returnDate.value + "T" + returnHour.value + ":" + returnMinute.value : "";
        };

        const updateSelectableTimes = () => {
            const minimumDate = today();
            startDate.min = minimumDate;
            returnDate.min = startDate.value || minimumDate;

            if (startDate.value && returnDate.value && returnDate.value < startDate.value) {
                returnDate.value = "";
                returnHour.value = "";
                returnMinute.value = "";
            }

            const now = new Date();
            setSelectableOptions(
                    startDate,
                    startHour,
                    startMinute,
                    (candidate) => candidate > now
            );

            const selectedStart = selectedDateTime(startDate, startHour, startMinute);
            const minimumReturn = selectedStart
                    ? new Date(selectedStart.getTime() + 30 * 60 * 1000)
                    : null;
            setSelectableOptions(
                    returnDate,
                    returnHour,
                    returnMinute,
                    (candidate) => minimumReturn !== null && candidate >= minimumReturn
            );
            syncHiddenValues();
        };

        addHourOptions(startHour, "시");
        addMinuteOptions(startMinute, "분");
        addHourOptions(returnHour, "시");
        addMinuteOptions(returnMinute, "분");
        restoreDateTime(startTime, startDate, startHour, startMinute);
        restoreDateTime(returnTime, returnDate, returnHour, returnMinute);

        if (!hasDateTime(startDate, startHour, startMinute)
                && !hasDateTime(returnDate, returnHour, returnMinute)) {
            const defaultStart = nextTenMinute();
            const defaultReturn = new Date(defaultStart.getTime() + 2 * 60 * 60 * 1000);
            setDateTime(startDate, startHour, startMinute, defaultStart);
            setDateTime(returnDate, returnHour, returnMinute, defaultReturn);
        }

        updateSelectableTimes();

        startDate.addEventListener("change", updateSelectableTimes);
        startHour.addEventListener("change", updateSelectableTimes);
        startMinute.addEventListener("change", updateSelectableTimes);
        returnDate.addEventListener("change", updateSelectableTimes);
        returnHour.addEventListener("change", updateSelectableTimes);
        returnMinute.addEventListener("change", updateSelectableTimes);
        form.addEventListener("submit", updateSelectableTimes);

        const initializePassengerPicker = () => {
            const picker = document.querySelector(".passenger-picker");
            const input = picker.querySelector("input");
            const toggle = picker.querySelector(".passenger-picker-toggle");
            const options = picker.querySelector(".passenger-options");
            const maximum = Number(picker.dataset.maxPassengers);

            const validate = () => {
                if (input.value === "") {
                    input.setCustomValidity("");
                    return;
                }

                const value = Number(input.value);
                input.setCustomValidity(Number.isInteger(value) && value >= 1 && value <= maximum
                        ? ""
                        : "탑승 인원은 1명부터 " + maximum + "명까지 입력할 수 있습니다.");
            };

            const close = () => {
                options.hidden = true;
                toggle.setAttribute("aria-expanded", "false");
            };

            const renderOptions = () => {
                options.replaceChildren();
                for (let passengerCount = 1; passengerCount <= maximum; passengerCount += 1) {
                    const option = document.createElement("button");
                    option.type = "button";
                    option.className = "passenger-option";
                    option.textContent = passengerCount + "명";
                    option.classList.toggle("selected", Number(input.value) === passengerCount);
                    option.addEventListener("click", () => {
                        input.value = passengerCount;
                        validate();
                        close();
                        input.focus();
                    });
                    options.append(option);
                }
            };

            toggle.addEventListener("click", (event) => {
                event.stopPropagation();
                const willOpen = options.hidden;
                if (willOpen) {
                    renderOptions();
                }
                options.hidden = !willOpen;
                toggle.setAttribute("aria-expanded", String(willOpen));
            });
            input.addEventListener("input", validate);
            input.addEventListener("change", validate);
            document.addEventListener("click", (event) => {
                if (!picker.contains(event.target)) {
                    close();
                }
            });
            document.addEventListener("keydown", (event) => {
                if (event.key === "Escape") {
                    close();
                }
            });
        };

        initializePassengerPicker();
    })();
</script>
</body>
</html>
