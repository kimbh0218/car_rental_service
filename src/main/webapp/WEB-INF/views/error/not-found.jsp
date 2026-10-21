<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>페이지를 찾을 수 없음 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
</head>
<body>
<header class="site-header"><a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a></header>
<main class="auth-layout"><section class="auth-card error-card" aria-labelledby="page-title">
    <h1 id="page-title">페이지를 찾을 수 없습니다.</h1>
    <p class="intro">주소가 변경되었거나 존재하지 않는 페이지입니다.</p>
    <div class="form-actions"><a class="button" href="${pageContext.request.contextPath}/">홈으로</a></div>
</section></main>
</body>
</html>
