<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>접근 안내 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
</head>
<body>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a>
</header>

<main class="auth-layout">
    <section class="auth-card error-card" aria-labelledby="page-title">
        <h1 id="page-title"><c:out value="${errorTitle}"/></h1>
        <p class="intro"><c:out value="${errorMessage}"/></p>
        <c:if test="${not empty errorNotice}">
            <p class="error-notice"><c:out value="${errorNotice}"/></p>
        </c:if>
        <div class="form-actions">
            <a class="button" href="<c:out value='${primaryActionUrl}'/>"><c:out value="${primaryActionText}"/></a>
            <a class="button secondary" href="${pageContext.request.contextPath}/">홈으로</a>
        </div>
    </section>
</main>
</body>
</html>
