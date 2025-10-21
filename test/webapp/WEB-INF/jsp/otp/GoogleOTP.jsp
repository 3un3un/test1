<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="mp" uri="/MetaPlusTags"%>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script type="text/javaScript" language="javascript">
</script>
</head>
<body>

<noscript class="noScriptTitle">자바스크립트를 지원하지 않는 브라우저에서는 일부 기능을 사용하실 수 없습니다.</noscript>
	<!-- ********** 여기서 부터 본문 내용 *************** -->
	<div class="content_title"></div>
	<div class="content" id="content">
		<div>
		<form action="/deveyeweb_new/otp/checkOtp.do" method="POST">
		<table>
			<tr>
				<th>QR 코드 스캔</th>
				<td>
					<img src="${QRKey.url}">				
				</td>
			</tr>
			<tr>
				<th>인증 키 입력</th>
				<td>
				<input type="hidden" name="encodedKey" value="${QRKey.encodedKey}">
					${QRKey.encodedKey}
					
				</td>
			</tr>
			<tr>
				<th>OTP 코드 입력</th>
				<td>
					<input type="text" name="otpCode">
				</td>
			</tr>
			<tr>
				<td>
					<button type="submit" id="btnChk">확인</button>
				</td>
			</tr>				
		</table>
		</form>
		</div>
	</div>
</body>
</html>