package NaverWorksDev;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.SourceVersion;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;


public class BoardTransferApp {
	
	private static int iHttpCallCount = 0;
	
	private static String CLIENT_ID_RM = "41j55jsRkPdE0qTfbBh0";
	private static String CLIENT_SECRET_RM = "4CdpU16X_W";
	private static String SERVICE_ACCOUNT_RM = "eay9m.serviceaccount@remainsoft.co.kr";
	
	private static String CLIENT_ID_SJ = "mCoViy_GHH3u81RB6yK_";
	private static String CLIENT_SECRET_SJ = "Op5bxsSbHT";
	private static String SERVICE_ACCOUNT_SJ = "slorw.serviceaccount@sjlink.net";

	public static void main(String[] args) throws Exception {
		// 1. sj 토큰
		String assertionSj = generateJWT("SJ", CLIENT_ID_SJ, SERVICE_ACCOUNT_SJ);
		String jwtTokenSj = requestJWTToken(CLIENT_ID_SJ, CLIENT_SECRET_SJ, assertionSj);
		System.out.println("access_token_sj: " + jwtTokenSj);
		
		// 2. rm 토큰
		String assertionRm = generateJWT("RM", CLIENT_ID_RM, SERVICE_ACCOUNT_RM);
		String jwtTokenRm = requestJWTToken(CLIENT_ID_RM, CLIENT_SECRET_RM, assertionRm);
		System.out.println("access_token_rm: " + jwtTokenRm);
	
		
		// rm 게시판 삭제 - 보류
/*		List<Map<Long, String>> boardInfoListRm = viewBoards(jwtTokenRm); // 삭제할 boardInfo
		deletePosts(jwtTokenRm, boardInfoListRm);*/
		

		// 3. sj 게시판 목록 조회
		List<JSONObject> boardInfoListSj = viewBoards(jwtTokenSj);
		// boardName, boardIdSj, boardIdRm 맵핑		
		List<JSONObject> boardMappingList = registBoard(jwtTokenRm, boardInfoListSj);
		
		// 4. sj 게시글 목록 조회
		List<JSONObject> postData = viewPosts(jwtTokenSj, boardInfoListSj);
		
		// 5. sj 게시글 detail 조회
		List<JSONObject> detailedPosts = viewPostsDetail(jwtTokenSj, postData); // 게시글 detail 조회
		
		// 6. rm 보드에 sj 게시글 작성
		writePosts(jwtTokenRm, detailedPosts, boardMappingList);
		
		// 이미지 오류 테스트
		// writePosts(jwtTokenRm, detailedPosts);
		
	}
	public static void connectionDB() {
		// 드라이버가 등록되었는지 확인 - Class.forName(드라이버명)
		String driverName = "oracle.jdbc.OracleDriver";
		Connection conn = null;
		try {
			// JDBC Driver 등록
			Class.forName(driverName);

			// db와 연결하여 작업하는 Connection 객체 필요
			// getConnection(jdbcUrl, userid, password)
			// db서버와 연결해서 db에 sql명령을 주고 받는 에이전트
			conn = DriverManager.getConnection("jdbc:oracle:thin:@//localhost:1521/XE", "naverWorks", "naverWorks");
			System.out.println("디비 연결 성공");
		} catch (Exception e) {
			e.printStackTrace();
		} 
/*				finally {
					if (conn != null) {
						try {
							// 연결 끊기
							conn.close();
							System.out.println("연결 끊기");
						} catch (SQLException e) {
						}
					}
}*/
	}
/*	public static void connectionDB() {
		
	}*/
	
	// JWT 생성
	public static String generateJWT(String type, String clientId, String serviceAccount) throws Exception {
		
		String headerJson = objectToJson(getHeader());
		String headerBase64 = base64UrlEncode(headerJson.getBytes("UTF-8"));
		
		String payloadJson = objectToJson(getPayload(clientId, serviceAccount));
		String payloadBase64 = base64UrlEncode(payloadJson.getBytes("UTF-8"));
		
		String privateKeyPath = "";
		if ("RM".equals(type)) {
			privateKeyPath = "C:\\privatekey\\rm\\private_20250306184418.key";
		}
		else {
			privateKeyPath = "C:\\privatekey\\sj\\private_20250311142403.key";
		}
		String signatureBase64 = signWithRS256(loadPrivateKey(privateKeyPath), headerBase64 + "." + payloadBase64);
		
		return headerBase64 + "." + payloadBase64 + "." + signatureBase64;
	}
	
	// 인증 서버로 토큰 요청
	public static String requestJWTToken(String clientId, String clientSecret, String assertion) throws Exception {
		String tokenUrl = "https://auth.worksmobile.com/oauth2/v2.0/token";
		String grandType = "urn:ietf:params:oauth:grant-type:jwt-bearer";
		String contentType = "application/x-www-form-urlencoded;charset=UTF-8";
		String scope = "board calendar";

		String requestContent = "grant_type=" + grandType
		+ "&client_id=" + clientId
		+ "&client_secret=" + clientSecret
		+ "&assertion=" + assertion
		+ "&scope=" + scope;
		
		String responseContent = HttpSend(tokenUrl, "POST", contentType, null, requestContent);
		
		Map<String, String> data = jsonToObject(responseContent, Map.class);
		// System.out.println("access_token: " + data.get("access_token"));
		return data.get("access_token");
	}
	
	// JSON
	// rm 게시판 등록
	public static List<JSONObject> registBoard (String jwtToken, List<JSONObject> boardInfoList) throws Exception {
		String boardRequestUrl = "https://www.worksapis.com/v1.0/boards";
		String contentType = "application/json";
		
		List<JSONObject> boardMappingList = new ArrayList<>();
		System.out.println("boardInfoListSize: " + boardInfoList.size());
		JSONObject requestBody = new JSONObject();
		
		for(JSONObject boardInfo : boardInfoList) {
			long boardIdSj = boardInfo.getLong("boardId");
			String boardNameSj = boardInfo.getString("boardName");
			
			requestBody.put("boardName", boardNameSj);
			String responseBody = HttpSend(boardRequestUrl, "POST", contentType, jwtToken, requestBody.toString());
			
			// 생성된 게시판에서 rm boardId를 가져온 후 맵핑
			JSONObject jsonResponse = new JSONObject(responseBody);
			Long boardIdRm = jsonResponse.getLong("boardId"); // rm boardId
			
			JSONObject boardMap = new JSONObject();
			
			boardMap.put("boardName", boardNameSj);
			boardMap.put("boardIdSj", boardIdSj);
			boardMap.put("boardIdRm", boardIdRm);
			boardMappingList.add(boardMap);
		}
		System.out.println("boardMappingListSize: " + boardMappingList.size());
		
		return boardMappingList;
	}
	
	// JSON
	// 1. 게시판 목록 조회 - sj
	public static List<JSONObject> viewBoards(String jwtToken) throws Exception {
		String boardRequestUrl = "https://www.worksapis.com/v1.0/boards?count=200";
		List<JSONObject> boardInfoList = new ArrayList<>();
		
		String responseContent = HttpSend(boardRequestUrl, "GET", null, jwtToken, null);
		JSONObject jsonResponse = new JSONObject(responseContent);
		JSONArray boardsArray = jsonResponse.optJSONArray("boards");
		
		if (boardsArray != null) {
			for (int i = 37; i < boardsArray.length(); i++) {
				JSONObject board = boardsArray.getJSONObject(i);
				long boardId = board.getLong("boardId"); // boardId 추출
				String boardName = board.getString("boardName");
				
				JSONObject boardInfo = new JSONObject();
				boardInfo.put("boardId", boardId);
				boardInfo.put("boardName", boardName);

				boardInfoList.add(boardInfo);

				//System.out.println("boardId: " + boardId);
				//System.out.println("board: " + board);
			}
			System.out.println("boardLength: " + boardsArray.length());
		}
		
		return boardInfoList;
	}
		
	// JSON
	//2. 게시글 목록 조회 -sj
	public static List<JSONObject> viewPosts(String jwtToken, List<JSONObject> boardInfoList) throws Exception {
		String postRequestUrl = "https://www.worksapis.com/v1.0/boards/";
		List<JSONObject> postData = new ArrayList<>(); // post 전체 데이터
		
		// 전체 목록 조회
		if (boardInfoList != null) {
			for (JSONObject boardInfo : boardInfoList) {
				int count = 40; // default = 20
				String cursor = "";
				
				while (true) {
					long boardId = boardInfo.getLong("boardId");
					String requestUrl = postRequestUrl + boardId + "/posts?count=" + count + "&cursor=" + cursor;
					String responseContent = HttpSend(requestUrl, "GET", null, jwtToken, null);
					
					JSONObject jsonResponse = new JSONObject(responseContent);
					JSONArray postsArray = jsonResponse.optJSONArray("posts");
					
					if (postsArray != null) {
						for (int j = 0; j < postsArray.length(); j++) {
							JSONObject post = postsArray.getJSONObject(j);
							postData.add(post); // post 전체 데이터
						}
					}
					
					JSONObject responseMetaData = jsonResponse.optJSONObject("responseMetaData");
					if(responseMetaData == null || !responseMetaData.has("nextCursor") || responseMetaData.isNull("nextCursor")) {
						break;
					}
					cursor =  responseMetaData.getString("nextCursor");
				}
			}
		}
		
		/*
		 * 테스트용 -  특정 board 목록 조회
		 */
		/*List<Long> postIdList = new ArrayList<>();
		int count = 40; // default = 20
		String cursor = "";
		long boardId = 4070000000101796468L;
		while (true) {
			String requestUrl = postRequestUrl + boardId + "/posts?count=" + count + "&cursor=" + cursor;
			String responseContent = HttpSend(requestUrl, "GET", null, jwtToken, null);
			
			JSONObject jsonResponse = new JSONObject(responseContent);
			JSONArray postsArray = jsonResponse.optJSONArray("posts");
			
			if (postsArray != null) {
				for (int j = 0; j < postsArray.length(); j++) {
					JSONObject post = postsArray.getJSONObject(j);
					long postId = post.getLong("postId");
					postIdList.add(postId);
					postData.add(post); // post 전체 데이터
				}
			}
			
			JSONObject responseMetaData = jsonResponse.optJSONObject("responseMetaData");
			if(responseMetaData == null || !responseMetaData.has("nextCursor") || responseMetaData.isNull("nextCursor")) {
				break;
			}
			cursor =  responseMetaData.getString("nextCursor");
		}*/
		
		// System.out.println("postData: " + postData);
		System.out.println("postDataSize: " + postData.size());
		return postData;
	}
	
	// 3. 게시글 detail - sj
	public static List<JSONObject> viewPostsDetail(String jwtToken, List<JSONObject> postData) throws Exception {
		String postDetailRequestUrl = "https://www.worksapis.com/v1.0/boards/";
		List<JSONObject> detailedPosts = new ArrayList<>();
		
		if (postData != null) {
			for (JSONObject post : postData) {
				long boardId = post.getLong("boardId");
				long postId = post.getLong("postId");
				String requestUrl = postDetailRequestUrl + boardId + "/posts/" + postId;
				String responseContent = HttpSend(requestUrl, "GET", null, jwtToken, null);
				JSONObject postDetail = new JSONObject(responseContent);
				
				// 게시글 제목에 작성자, 작성일자 추가
				String title = postDetail.getString("title");
				String userName = postDetail.getString("userName");
				String createdTime = postDetail.getString("createdTime");
				// 작성일자 원하는 형식으로 변환
				OffsetDateTime offsetDateTime = OffsetDateTime.parse(createdTime);
				LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
				DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy.M.d HH:mm");
				String formattedDate = localDateTime.format(df);
			
				String newTitle = title + " [" + userName + " - " + formattedDate + "]";
				postDetail.put("title", newTitle);
				
				detailedPosts.add(postDetail);
			}
		}
		//System.out.println("detailedPosts: "+ detailedPosts);
		//System.out.println("detailedPostsSize: "+ detailedPosts.size());
		return detailedPosts;
	}
	
	// 4. 게시글 작성 - remain
	public static void writePosts(String jwtTokenRm, List<JSONObject> detailedPosts, List<JSONObject> boardMappingList) throws Exception {
		String writePostRequestUrl = "https://www.worksapis.com/v1.0/boards/";
		String contentType = "application/json";
		int cnt = 0;
		int imgErrorCnt = 0;
		System.out.println("detailedPostsSize: "+ detailedPosts.size());
		System.out.println("boardMappingList: " + boardMappingList);
		// 역순
		if (detailedPosts != null) {
			for (int i = detailedPosts.size() ; i > 0 ; i --) {
				JSONObject index = detailedPosts.get(i-1);
				long boardIdSj = index.optLong("boardId", 1); // sj
				System.out.println("boardIdSj: " + boardIdSj);
				long boardIdRm = 1;
				System.out.println("boardIdRm: " + boardIdRm);
				
				//long boardIdRm = 4070000000160788659L; // 테스트용 rm 보드
				
				// sj의 boardId를 remain의 boardId로 변경
				for (JSONObject boardMap : boardMappingList) {
					long boardIdSjMap = boardMap.getLong("boardIdSj");
					if (boardIdSjMap == boardIdSj) {
						boardIdRm = boardMap.getLong("boardIdRm");
						System.out.println("boardIdRm2: " + boardIdRm);
						break;
					}
				}
				String requestUrl = writePostRequestUrl + boardIdRm + "/posts";
				
				JSONObject requestBody = new JSONObject();
				requestBody.put("title", index.getString("title"));
				requestBody.put("body", index.getString("body"));
				requestBody.put("sendNotifications", false);
				
				System.out.println("게시글 등록: " + index.getString("title"));

				try {
					String responseContent = HttpSend(requestUrl, "POST", contentType, jwtTokenRm, requestBody.toString());
				} catch (IOException e) {
					// src 치환
					cnt ++;
					String body = requestBody.getString("body");
					String regExp = " src=\"https://storage\\.worksmobile\\.com[^\"]*\"";
					
					if (body.matches("(?s).*" + regExp + ".*")) {
						imgErrorCnt ++;
						System.out.println("imgErrorCnt: " + imgErrorCnt);
					}
					
					String replaceBody = body.replaceAll(regExp, "");
					requestBody.put("body", replaceBody);
					
					String responseContent = HttpSend(requestUrl, "POST", contentType, jwtTokenRm, requestBody.toString());
					
					// boardId, 게시글 제목
					System.out.println("--------------------------------------------------------------------------------------------");
					System.out.println("error: " + e);
					System.out.println("boardIdSj: " + boardIdSj);
					System.out.println("boardIdRm: " + boardIdRm);
					System.out.println("postTitle: " + requestBody.getString("title"));
					System.out.println("--------------------------------------------------------------------------------------------");					
				}
			
			}
		}
		System.out.println("errorCnt: " + cnt);
	}
	
	public static void writePosts(String jwtTokenRm, List<JSONObject> detailedPosts) throws Exception {
		String writePostRequestUrl = "https://www.worksapis.com/v1.0/boards/";
		String contentType = "application/json";
		int cnt = 0;
		int imgErrorCnt = 0;
		System.out.println("detailedPostsSize: "+ detailedPosts.size());
		// 역순
		if (detailedPosts != null) {
			for (int i = detailedPosts.size() ; i > 0 ; i --) {
				JSONObject index = detailedPosts.get(i-1);
				//long boardIdSj = index.optLong("boardId", 1); // sj
				//System.out.println("boardIdSj: " + boardIdSj);
				//long boardIdRm = 1;
				//System.out.println("boardIdRm: " + boardIdRm);
				
				long boardIdRm = 4070000000160788659L; // 테스트용 rm 보드
				
				// sj의 boardId를 remain의 boardId로 변경
/*				for (JSONObject boardMap : boardMappingList) {
					long boardIdSjMap = boardMap.getLong("boardIdSj");
					if (boardIdSjMap == boardIdSj) {
						boardIdRm = boardMap.getLong("boardIdRm");
						System.out.println("boardIdRm2: " + boardIdRm);
						break;
					}
				}*/
				String requestUrl = writePostRequestUrl + boardIdRm + "/posts";
				
				JSONObject requestBody = new JSONObject();
				requestBody.put("title", index.getString("title"));
				requestBody.put("body", index.getString("body"));
				requestBody.put("sendNotifications", false);
				
				System.out.println("게시글 등록: " + index.getString("title"));

				try {
					String responseContent = HttpSend(requestUrl, "POST", contentType, jwtTokenRm, requestBody.toString());
				} catch (IOException e) {
					// src 치환
					List<JSONObject> errorList = new ArrayList<>();
					cnt ++;
					String body = requestBody.getString("body");
					String regExp = " src=\"https://storage\\.worksmobile\\.com[^\"]*\"";
					
					if (body.matches("(?s).*" + regExp + ".*")) {
						imgErrorCnt ++;
						System.out.println("imgErrorCnt: " + imgErrorCnt);
						//errorList.add(boardIdRm:);
						
					}
					
					String replaceBody = body.replaceAll(regExp, "");
					requestBody.put("body", replaceBody);
					
					String responseContent = HttpSend(requestUrl, "POST", contentType, jwtTokenRm, requestBody.toString());
					
					// boardId, 게시글 제목
					System.out.println("--------------------------------------------------------------------------------------------");
					System.out.println("error: " + e);
					System.out.println("error: " + e.getMessage());
					//System.out.println("boardIdSj: " + boardIdSj);
					System.out.println("boardIdRm: " + boardIdRm);
					System.out.println("postTitle: " + requestBody.getString("title"));
					System.out.println("--------------------------------------------------------------------------------------------");					
					//errorList
					
				}
			
			}
		}
		System.out.println("imgErrorCnt: " + imgErrorCnt);
		System.out.println("errorCnt: " + cnt);
	}
	
	/*public static String HtmlParsing(String content) {
		Document doc = Jsoup.parse(TAG_STR2);
		Elements elements = doc.select("img");
		for (Element element : elements) {
			System.out.println(element.toString());
			String imagePath = element.attr("src");
			System.out.println(imagePath);
			
			String imageFile = ROOT_PATH + imagePath.replace("../../", "");
			int iParamIdx = imageFile.indexOf("?");
			if (iParamIdx > -1) {
				imageFile = imageFile.substring(0, iParamIdx);
			}
			
			System.out.println(imageFile);
			String base64 = convertBase64(imageFile);
			System.out.println(base64);
			element.attr("src", "data:image/png;base64," + base64);
		}
		System.out.println("----------------------");
		Element element2 = doc.select("body>div").first();
		System.out.println(element2.toString());
		
		
		return null;
	}*/
	
	private static PrivateKey loadPrivateKey(String filePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[2048];
		int bytesRead;
		while ((bytesRead = fis.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, bytesRead);
		}
		fis.close();

		byte[] keyBytes = buffer.toByteArray();

		String privateKeyPEM = new String(keyBytes, StandardCharsets.UTF_8)
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");

		byte[] decodedKey = Base64.getDecoder().decode(privateKeyPEM);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");

		return keyFactory.generatePrivate(keySpec);
	}

	public static Map<String, String> getHeader() {
		Map<String, String> headearData = new LinkedHashMap<String, String>();
		headearData.put("alg", "RS256");
		headearData.put("typ", "JWT");
		
		return headearData;
	}
	
	public static Map<String, Object> getPayload(String clientID, String serviceAccount) {
		Map<String, Object> payloadData = new HashMap<String, Object>();
		
		Date iat = new Date();
		Date exp = new Date(iat.getTime() + (60 * 60 * 1000));
		
		// Unix time
		long iatUnix = iat.getTime() / 1000;
		long expUnix = exp.getTime() / 1000;
		
		payloadData.put("iss", clientID);
		payloadData.put("sub", serviceAccount);
		payloadData.put("iat", iatUnix);
		payloadData.put("exp", expUnix);
		
		return payloadData;
	}
	
	public static String objectToJson(Object object) throws IOException {
		ObjectMapper om = new ObjectMapper();
		
		return om.writeValueAsString(object);
	}
	
	public static <T> T jsonToObject(String jsonData, Class<T> objectType) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			//Object 에 존재하는 속성만 변환하고 없는 속성 변경시 에러 방지
			mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			return mapper.readValue(jsonData, objectType);
		} catch (Exception e) {
			return null;
		}
	}
	
	private static String signWithRS256(PrivateKey privateKey, String data) throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA");
		signature.initSign(privateKey);
		signature.update(data.getBytes("UTF-8"));
		byte[] signedBytes = signature.sign();
		
		return base64UrlEncode(signedBytes);
	}
	
	public static String base64UrlEncode(byte[] data) {
		return Base64.getUrlEncoder().encodeToString(data);
	}
	
	// request 요청
	public static String HttpSend(String reqURL, String methodType, String contentType, String accessToken, String contents) throws Exception {
		HttpURLConnection httpConn = null;
		try {
			try {
				Thread.sleep(1000); // 1초
				iHttpCallCount++;
				System.out.println(reqURL + ": " + iHttpCallCount);
				
				URL url = new URL(reqURL);
				URLConnection urlConn = url.openConnection();
				
				httpConn = (HttpURLConnection) urlConn;
				if(contentType != null) {
					httpConn.setRequestProperty("Content-Type", contentType);
				}
				httpConn.setRequestMethod(methodType);
				
				if (accessToken != null) {
					httpConn.setRequestProperty("Authorization", "Bearer " + accessToken);
				}
				if ("POST".equals(methodType)) {
					//아래의 옵션이 POST 전용인가?
					httpConn.setDoInput(true);
					httpConn.setDoOutput(true);  //post 용
					httpConn.setInstanceFollowRedirects(false);
				}
				
				//HTTPS 셋팅
				if ("https".equals(url.getProtocol())) {
					setHttpsContext((HttpsURLConnection) urlConn);
				}
				
			} catch (Exception e) {
				//throw new HttpException("createHttpConnect Exception", e);
			}
			
			try {
				//POST 방식 파라미터 기록
				if ("POST".equalsIgnoreCase(httpConn.getRequestMethod())) {
					if (contents != null && !contents.isEmpty()) {
						httpConn.setDoOutput(true); // true indicates POST request
						
						//sends POST data
						OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
						writer.write(contents);
						writer.flush();
						writer.close();
					}
				}
				//HTTP Connection Timeout 10초 
				httpConn.setConnectTimeout(30000);
				
				//응답 결과 받기
				int responseCode = httpConn.getResponseCode(); //실제 URL 호출 시점
				
				InputStream inputStream = httpConn.getInputStream(); //실제 URL 호출 시점
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
				
				String line = "";
				StringBuffer response = new StringBuffer();
				while ((line = reader.readLine()) != null) {
					response.append(line);
				}
				reader.close();
				
				for (Map.Entry<String, List<String>> header : httpConn.getHeaderFields().entrySet()) {
					for (String value : header.getValue()) {
						//System.out.println(header.getKey() + " : " + value);
					}
				}
				
				return response.toString();
			} catch (Exception e) {
				//throw new HTTPException("Send Http Exception", e);
				throw e;
			}
			
		} finally {
			if (httpConn != null) httpConn.disconnect();
		}
	}
	
	private static void setHttpsContext(HttpsURLConnection httpsConn) throws Exception {
		//호스트명 체크
		httpsConn.setHostnameVerifier(new HostnameVerifier() {
			
			@Override
			public boolean verify(String hostname, SSLSession session) {
				// TODO Auto-generated method stub
				return false;
			}
		});
		
		//인증서 체크
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, new TrustManager[]{
			new X509TrustManager() {
				
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					// TODO Auto-generated method stub
				}
			}
		}, null);
		httpsConn.setSSLSocketFactory(context.getSocketFactory());
	}
}
