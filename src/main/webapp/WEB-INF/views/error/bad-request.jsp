<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>잘못된 요청 | Car Rental</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css">
</head>
<body>
<header class="site-header"><a class="brand" href="${pageContext.request.contextPath}/">Car Rental</a></header>
<main class="auth-layout"><section class="auth-card error-card" aria-labelledby="page-title">
    <h1 id="page-title">요청 정보를 확인해주세요.</h1>
    <p class="intro">주소 또는 입력한 정보가 올바르지 않습니다. 다시 시도해주세요.</p>
    <div class="form-actions"><a class="button" href="${pageContext.request.contextPath}/">홈으로</a></div>
</section></main>
</body>
</html>
