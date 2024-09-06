package com.quincy.sdk.helper;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.springframework.http.MediaType;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

public class HttpClientHelper {
//	private final static String ERR_MSG = "Abnormal HTTP Status Code: %s, URI: %s";

	public static String get(String url, Header[] headers) throws IOException {
		SimplifiedHttpResponse response = HttpClientHelper.get(url, headers, null);
		return response.getContent();
	}

	public static String post(String url, Header[] headers, List<NameValuePair> nameValuePairList) throws IOException {
		SimplifiedHttpResponse response = HttpClientHelper.post(url, headers, nameValuePairList, null);
		return response.getContent();
	}

	public static String post(String url, Header[] headers, String body) throws IOException {
		SimplifiedHttpResponse response = HttpClientHelper.post(url, headers, body, null);
		return response.getContent();
	}

	public static String post(String url, Header[] headers, HttpEntity entity) throws IOException {
		SimplifiedHttpResponse response = HttpClientHelper.post(url, headers, entity, null);
		return response.getContent();
	}

	public static SimplifiedHttpResponse get(String url, Header[] headers, String sessionKey) throws IOException {
		HttpGet httpGet = null;
		try {
			httpGet = new HttpGet(url);
			return getString(httpGet, headers, sessionKey);
		} finally {
			if(httpGet!=null)
				httpGet.abort();
		}
	}

	public static SimplifiedHttpResponse post(String url, Header[] headers, List<NameValuePair> nameValuePairList, String sessionKey) throws IOException {
		return post(url, headers, new UrlEncodedFormEntity(nameValuePairList, "UTF-8"), sessionKey);
	}

	public static SimplifiedHttpResponse post(String url, Header[] headers, String body, String sessionKey) throws IOException {
		return post(url, headers, new StringEntity(body), sessionKey);
	}

	public static SimplifiedHttpResponse post(String url, Header[] headers, HttpEntity entity, String sessionKey) throws IOException {
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);
			httpPost.setEntity(entity);
			return getString(httpPost, headers, sessionKey);
		} finally {
			if(httpPost!=null)
				httpPost.abort();
		}
	}

	/*public static byte[] getBytes(String url, Header[] headers) throws IOException {
		HttpClient client = new HttpClient();
		HttpMethodBase method = null;
		method = new GetMethod(url);
		try {
			int statusCode = client.executeMethod(method);
			if(statusCode!=200)
				throw new HttpResponseException(statusCode, String.format(ERR_MSG, statusCode, url));
			byte[] buf = method.getResponseBody();
			return buf;
		} finally {
			if(method!=null)
				method.abort();
		}
	}*/

	public static byte[] getBytes(String url, Header[] headers) throws IOException {
		HttpGet httpGet = null;
		InputStream in = null;
		try {
			httpGet = new HttpGet(url);
			HttpResponse response = send(httpGet, headers);
			in = response.getEntity().getContent();
			byte[] b = CommonHelper.input2bytes(in);
			return b;
		} finally {
			if(in!=null)
				in.close();
			if(httpGet!=null)
				httpGet.abort();
		}
	}

	public static void saveAsFile(String url, Header[] headers, String path) throws IOException {
		byte[] buf = getBytes(url, headers);
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(path));
			out.write(buf);
			out.flush();
		} finally {
			if(out!=null)
				out.close();
		}
	}

	private static SimplifiedHttpResponse getString(HttpUriRequest httpUriRequest, Header[] headers, String _sessionKey) throws IOException {
		InputStream in = null;
		try {
			HttpResponse response = send(httpUriRequest, headers);
			in = response.getEntity().getContent();
			String content = CommonHelper.input2String(in);
			String sessionId = null;
			if(_sessionKey!=null) {
				String sessionKey = _sessionKey.trim();
				if(sessionKey.length()>0) {
					Header[] cookieHeaders = response.getHeaders("Set-Cookie");
					if(cookieHeaders!=null&&cookieHeaders.length>0) {
						HeaderElement[] headerElements = cookieHeaders[0].getElements();
						if(headerElements!=null&&headerElements.length>0) {
							for(HeaderElement headerElement:headerElements) {
								if(headerElement.getName().equals(sessionKey)) {
									sessionId = headerElement.getValue();
									break;
								}
							}
						}
					}
				}
			}
			return new SimplifiedHttpResponse(content, sessionId);
		} finally {
			if(in!=null)
				in.close();
		}
	}

	private static HttpResponse send(HttpUriRequest httpUriRequest, Header[] headers) throws ClientProtocolException, IOException {
		if(headers!=null&&headers.length>0)
			httpUriRequest.setHeaders(headers);
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		HttpResponse response = httpClient.execute(httpUriRequest);
		int statusCode = response.getStatusLine().getStatusCode();
		if(statusCode==200) {
			return response;
		} else {
			InputStream in = null;
			String msg = null;
			try {
				in = response.getEntity().getContent();
				byte[] b = new byte[in.available()];
				in.read(b);
				msg = new String(b);
			} finally {
				in.close();
			}
			throw new HttpResponseException(statusCode, "\r\nRUL: "+httpUriRequest.getURI()+"\r\nMSG: "+msg);
		}
	}

	public interface UploadHandler {
		public void handle(String name, InputStream in) throws IOException;
	}

	public static void handleUpload(HttpServletRequest request, UploadHandler handler) throws IOException, ServletException {
		Collection<Part> parts = request.getParts();
		for(Part part:parts) {
			InputStream in = null;
			try {
				in = part.getInputStream();
				handler.handle(part.getSubmittedFileName(), in);
			} finally {
				if(in!=null)
					in.close();
			}
		}
		/*
		DiskFileItemFactory factory = new DiskFileItemFactory.Builder().get();
		JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload = new JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory>(factory);
		upload.setHeaderCharset(Charset.forName("UTF-8"));
		List<DiskFileItem> list = upload.parseRequest(request);
		for(DiskFileItem item:list) {
			InputStream in = null;
			try {
				in = item.getInputStream();
				handler.handle(item.getName(), in);
			} finally {
				if(in!=null)
					in.close();
			}
		}
		*/
	}

	public static SimplifiedHttpResponse uploadFromDownload(String downloadUrl, String uploadUrl, Header[] headers, String paramName, String filename, String sessionKey) throws IOException {
		HttpGet httpGet = null;
		InputStream in = null;
		try {
			httpGet = new HttpGet(downloadUrl);
			HttpResponse response = send(httpGet, headers);
			in = response.getEntity().getContent();
			MultipartEntityBuilder builder = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setCharset(Charset.forName("UTF-8")).addBinaryBody(paramName, in, ContentType.MULTIPART_FORM_DATA, filename);
			return HttpClientHelper.post(uploadUrl, headers, builder.build(), sessionKey);
		} finally {
			if(in!=null)
				in.close();
			if(httpGet!=null)
				httpGet.abort();
		}
	}

	public final static String FLAG_URI = "URI";
	public final static String FLAG_URL = "URL";

	public static String getRequestURIOrURL(HttpServletRequest request, String type) {
		StringBuffer url = FLAG_URI.equalsIgnoreCase(type)?new StringBuffer(300).append(request.getRequestURI()):request.getRequestURL();
		String s = null;
		Map<String, String[]> map = request.getParameterMap();
		if(map!=null&&map.size()>0) {
			Set<Entry<String, String[]>> set = map.entrySet();
			url.append("?");
			for(Entry<String, String[]> entry:set) {
				String[] values = entry.getValue();
				url.append(entry.getKey()).append("=").append((values!=null&&values.length>0)?values[0]:"").append("&");
			}
			s = url.substring(0, url.length()-1);
		} else
			s = url.toString();
		return s;
	}

	public static void output(HttpServletResponse response, String contentType, String contentTxt) throws IOException {
		PrintWriter out = null;
		try {
			response.setContentType(contentType);
			out = response.getWriter();
			out.println(contentTxt);
			out.flush();
		} finally {
			if(out!=null)
				out.close();
		}
	}

	private final static String APPLICATION_JSON_UTF8_VALUE = MediaType.APPLICATION_JSON_VALUE+";charset=UTF-8";

	public static void outputJson(HttpServletResponse response, String json) throws IOException {
		output(response, APPLICATION_JSON_UTF8_VALUE, json);
	}

	public static void main(String[] args) throws IOException {
//		List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>(4);
//		nameValuePairList.add(new BasicNameValuePair("msg_signature", "5c45ff5e21c57e6ad56bac8758b79b1d9ac89fd3"));
//		nameValuePairList.add(new BasicNameValuePair("timestamp", "1409659589"));
//		nameValuePairList.add(new BasicNameValuePair("nonce", "263014780"));
//		nameValuePairList.add(new BasicNameValuePair("echostr", "P9nAzCzyDtyTWESHep1vC5X9xho/qYX3Zpb4yKa9SKld1DsH3Iyt3tP3zNdtp+4RPcs8TgAE7OaBO+FZXvnaqQ=="));
//		System.out.println(HttpClientHelper.post("http://localhost:8080/wechat/opencallbackmode", null, nameValuePairList));
//		System.out.println(HttpClientHelper.post("http://www.maqiangcgq.com/wechat/opencallbackmode", null, nameValuePairList));
//		System.out.println(HttpClientHelper.post("http://www.maqiangcgq.com/wechat/assistant", null, nameValuePairList));
//		Header[] headers = new Header[]{new BasicHeader("x-requested-with", "XMLHttpRequest")};
//		System.out.println(HttpClientHelper.post("http://localhost:8080/test", headers, nameValuePairList));
//		HttpClientHelper.saveAsFile("http://resource.jlcedu.maqiangcgq.com/upload/exam/exam-s-zhaorq-mock2-en_us.jpg", null, "D:/tmp/exam-s-zhaorq-mock2-en_us.jpg");
		Header[] headers = null;
		SimplifiedHttpResponse response = null;

		response = HttpClientHelper.get("http://maqiang777.com/xxx/set?flag=xaseeere", headers, "SESSION");
		System.out.println(response.getContent()+"---"+response.getSessionId());

		headers = new Header[]{new BasicHeader("Cookie", "SESSION="+response.getSessionId())};
		response = HttpClientHelper.get("http://maqiang777.com/xxx/get", headers, null);
		System.out.println(response.getContent());
	}
}