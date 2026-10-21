<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>차량 예약 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
    <script src="${pageContext.request.contextPath}/static/js/page-state-refresh.js" defer
            data-auth-state="${authState}" data-auth-status-url="${pageContext.request.contextPath}/auth/status"></script>
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
    <div class="header-actions">
        <a class="header-link" href="${pageContext.request.contextPath}/member/me">내 정보</a>
        <a class="header-link" href="${pageContext.request.contextPath}/member/reservations">내 예약</a>
        <form action="${pageContext.request.contextPath}/auth/logout" method="post">
            <button class="header-link" type="submit">로그아웃</button>
        </form>
    </div>
</header>

<main class="auth-layout reservation-layout">
    <div class="reservation-workspace">
    <section class="auth-card reservation-card" aria-labelledby="page-title">
        <p class="eyebrow">RESERVATION</p>
        <h1 id="page-title">차량 예약</h1>
        <p class="intro">이용 일정을 선택해 주세요. 예약된 시간은 선택할 수 없습니다.</p>

        <section class="reservation-car-summary" aria-label="선택한 차량">
            <span>선택 차량</span>
            <strong><c:out value="${car.name}"/></strong>
            <p>픽업 위치: <c:out value="${car.currentAddress}"/></p>
            <p>최대 탑승 인원: <c:out value="${car.maxPassengerCount}"/>명</p>
            <p>1km당 요금: <c:out value="${car.pricePerKm}"/>원</p>
        </section>

        <form id="reservation-form" action="${pageContext.request.contextPath}/member/reservations/new" method="post">
            <input name="carId" type="hidden" value="<c:out value='${car.id}'/>">

            <div class="form-row">
                <span class="field-label">대여 시작 일시</span>
                <div class="date-time-fields">
                    <input id="start-date" type="date" required aria-label="대여 시작 날짜">
                    <select id="start-hour" required aria-label="대여 시작 시"></select>
                    <select id="start-minute" required aria-label="대여 시작 분"></select>
                </div>
                <input id="start-time" name="startTime" type="hidden"
                       value="<c:out value='${startTime}'/>">
            </div>

            <div class="form-row">
                <span class="field-label">반납 예정 일시</span>
                <div class="date-time-fields">
                    <input id="return-date" type="date" required aria-label="반납 예정 날짜">
                    <select id="return-hour" required aria-label="반납 예정 시"></select>
                    <select id="return-minute" required aria-label="반납 예정 분"></select>
                </div>
                <input id="return-time" name="returnTime" type="hidden"
                       value="<c:out value='${returnTime}'/>">
            </div>

            <div class="form-row">
                <label for="passenger-count">탑승 인원</label>
                <c:set var="passengerLimit" value="${car.maxPassengerCount gt 9 ? 9 : car.maxPassengerCount}"/>
                <div class="passenger-picker" data-max-passengers="<c:out value='${passengerLimit}'/>">
                    <input id="passenger-count" name="passengerCount" type="number"
                           min="1" max="<c:out value='${passengerLimit}'/>" required inputmode="numeric"
                           value="<c:out value='${passengerCount}'/>">
                    <button class="passenger-picker-toggle" type="button"
                            aria-label="탑승 인원 선택" aria-expanded="false">선택</button>
                    <div class="passenger-options" hidden aria-label="탑승 인원 선택"></div>
                </div>
            </div>

            <div class="form-row">
                <label for="return-location">픽업 및 반납 위치</label>
                <input id="return-location" type="text" readonly
                       value="<c:out value='${car.currentAddress}'/>">
                <span class="hint">차량은 픽업한 주차장으로 반납됩니다.</span>
            </div>

            <p class="form-error" role="alert" aria-live="polite"><c:out value="${errorMessage}"/></p>

            <div class="form-actions">
                <button class="button" type="submit">예약하기</button>
                <a class="button secondary" href="${pageContext.request.contextPath}/cars">목록으로</a>
            </div>
        </form>
    </section>

    <aside class="reservation-schedule" aria-live="polite" aria-labelledby="schedule-title">
        <p class="eyebrow">SCHEDULE</p>
        <h2 id="schedule-title">차량 예약 현황</h2>
        <p id="schedule-description" class="schedule-description">
            날짜를 선택하면 해당 기간의 예약 시간을 보여드립니다.
        </p>
        <ol id="schedule-timeline" class="schedule-timeline"></ol>
    </aside>
    </div>
</main>
<script>
    (() => {
        const startDate = document.getElementById("start-date");
        const startHour = document.getElementById("start-hour");
        const startMinute = document.getElementById("start-minute");
        const startTime = document.getElementById("start-time");
        const returnDate = document.getElementById("return-date");
        const returnHour = document.getElementById("return-hour");
        const returnMinute = document.getElementById("return-minute");
        const returnTime = document.getElementById("return-time");
        const carId = document.querySelector("input[name='carId']").value;
        const scheduleDescription = document.getElementById("schedule-description");
        const scheduleTimeline = document.getElementById("schedule-timeline");
        const contextPath = "<c:out value='${pageContext.request.contextPath}'/>";

        let schedule = [];
        let scheduleRequestNumber = 0;
        let initialDefaultsAreActive = false;

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

        const hasDateTime = (dateInput, hourSelect, minuteSelect) =>
            Boolean(dateInput.value && hourSelect.value && minuteSelect.value);

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

        const overlapsReservation = (start, end) => schedule.some((item) => {
            const reservedStart = new Date(item.startTime);
            const reservedEnd = new Date(item.returnTime);
            return reservedStart < end && reservedEnd > start;
        });

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
                    (candidate) => {
                        const minimumEnd = new Date(candidate.getTime() + 30 * 60 * 1000);
                        return candidate > now && !overlapsReservation(candidate, minimumEnd);
                    }
            );

            const selectedStart = selectedDateTime(startDate, startHour, startMinute);
            const minimumReturn = selectedStart
                    ? new Date(selectedStart.getTime() + 30 * 60 * 1000)
                    : null;
            setSelectableOptions(
                    returnDate,
                    returnHour,
                    returnMinute,
                    (candidate) => minimumReturn !== null
                            && candidate >= minimumReturn
                            && !overlapsReservation(selectedStart, candidate)
            );
            syncHiddenValues();
        };

        const addDays = (dateValue, days) => {
            const date = new Date(dateValue + "T00:00:00");
            date.setDate(date.getDate() + days);
            return date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate());
        };

        const daysBetween = (from, to) => {
            const fromDate = new Date(from + "T00:00:00");
            const toDate = new Date(to + "T00:00:00");
            return Math.round((toDate - fromDate) / 86400000);
        };

        const findTwoHourDefaultStart = () => {
            const candidate = nextTenMinute();
            const searchEnd = new Date(candidate.getTime() + 7 * 24 * 60 * 60 * 1000);

            while (candidate < searchEnd) {
                const defaultReturn = new Date(candidate.getTime() + 2 * 60 * 60 * 1000);
                if (!overlapsReservation(candidate, defaultReturn)) {
                    return new Date(candidate);
                }
                candidate.setMinutes(candidate.getMinutes() + 10);
            }

            return null;
        };

        const formatDateTime = (value) => new Intl.DateTimeFormat("ko-KR", {
            month: "long",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false
        }).format(new Date(value));

        const renderSchedule = () => {
            scheduleTimeline.replaceChildren();

            if (schedule.length === 0) {
                const empty = document.createElement("li");
                empty.className = "schedule-empty";
                empty.textContent = "선택한 기간에 등록된 예약이 없습니다.";
                scheduleTimeline.append(empty);
                return;
            }

            schedule.forEach((item) => {
                const row = document.createElement("li");
                row.className = "schedule-item";

                const time = document.createElement("strong");
                time.textContent = formatDateTime(item.startTime)
                        + " ~ " + formatDateTime(item.returnTime);

                const status = document.createElement("span");
                status.textContent = item.status === "IN_PROGRESS" ? "대여 중" : "예약됨";

                row.append(time, status);
                scheduleTimeline.append(row);
            });
        };

        const loadSchedule = async () => {
            const from = startDate.value || returnDate.value || today();
            const lastDate = returnDate.value || startDate.value || addDays(from, 6);
            const days = Math.max(7, daysBetween(from, lastDate) + 1);
            const requestNumber = ++scheduleRequestNumber;

            scheduleDescription.textContent = "예약 현황을 불러오는 중입니다.";

            try {
                const url = contextPath + "/cars/schedule?carId="
                        + encodeURIComponent(carId)
                        + "&from=" + encodeURIComponent(from)
                        + "&days=" + encodeURIComponent(days);
                const response = await fetch(url, {headers: {"Accept": "application/json"}});

                if (!response.ok) {
                    throw new Error("schedule request failed");
                }

                const loadedSchedule = await response.json();
                if (requestNumber !== scheduleRequestNumber) {
                    return;
                }

                schedule = loadedSchedule;
                scheduleDescription.textContent = from + "부터 " + days + "일간의 예약 현황입니다.";
                renderSchedule();
                if (initialDefaultsAreActive) {
                    const defaultStart = findTwoHourDefaultStart();
                    if (defaultStart !== null) {
                        setDateTime(startDate, startHour, startMinute, defaultStart);
                        setDateTime(
                                returnDate,
                                returnHour,
                                returnMinute,
                                new Date(defaultStart.getTime() + 2 * 60 * 60 * 1000)
                        );
                    }
                    initialDefaultsAreActive = false;
                }
                updateSelectableTimes();
            } catch (error) {
                if (requestNumber !== scheduleRequestNumber) {
                    return;
                }

                schedule = [];
                scheduleDescription.textContent = "예약 현황을 불러오지 못했습니다. 다시 시도해 주세요.";
                renderSchedule();
                updateSelectableTimes();
            }
        };

        const refreshSchedule = () => {
            updateSelectableTimes();
            loadSchedule();
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
            initialDefaultsAreActive = true;
        }

        updateSelectableTimes();
        loadSchedule();

        const refreshAfterUserChange = () => {
            initialDefaultsAreActive = false;
            refreshSchedule();
        };
        const updateAfterUserChange = () => {
            initialDefaultsAreActive = false;
            updateSelectableTimes();
        };

        startDate.addEventListener("change", refreshAfterUserChange);
        returnDate.addEventListener("change", refreshAfterUserChange);
        startHour.addEventListener("change", updateAfterUserChange);
        startMinute.addEventListener("change", updateAfterUserChange);
        returnHour.addEventListener("change", updateAfterUserChange);
        returnMinute.addEventListener("change", updateAfterUserChange);

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
