package com.quincy.sdk.helper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class AliyunDNTXTUpdate {
	private final static String HTTP_PREFIX = "https://alidns.aliyuncs.com/?";
	private final static String ACTION_UPDATE = "UpdateDomainRecord";

	public static void main(String[] args) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
		Properties pro = new Properties();
		InputStream in = null;
		String id = null;
		String secret = null;
		try {
			in = new FileInputStream(args[0]);
			pro.load(in);
			id = pro.getProperty("aliyun.id");
			secret = pro.getProperty("aliyun.secret");
		} finally {
			if(in!=null)
				in.close();
		}
		String signatureNonce = UUID.randomUUID().toString().replaceAll("-", "");
		Calendar c = Calendar.getInstance();
		int zoneOffset = c.get(Calendar.ZONE_OFFSET);
		c.add(Calendar.HOUR, -(zoneOffset/1000/3600));
		String _timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(c.getTime());
//		String _timestamp2 = _timestamp.replaceAll("\\s+", "%20").replaceAll(":", "%3A");
//		System.out.println(_timestamp2);
		String timestamp = _timestamp.replaceAll("\\s+", "T")+"Z";
		timestamp = URLEncoder.encode(timestamp, "UTF-8");
		String action = args[1];
		StringBuilder params = new StringBuilder(500);
		List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>(20);
		params.append("AccessKeyId=");
		params.append(id);
		params.append("&Action=");
		params.append(action);
		nameValuePairList.add(new BasicNameValuePair("AccessKeyId", id));
		nameValuePairList.add(new BasicNameValuePair("Action", action));
//		nameValuePairList.add(new BasicNameValuePair("", ));
		if("DescribeDomainRecords".equals(action)) {
			params.append("&DomainName=maqiangcgq.com");
			nameValuePairList.add(new BasicNameValuePair("DomainName", "maqiangcgq.com"));
		}
		params.append("&Format=JSON");
		nameValuePairList.add(new BasicNameValuePair("Format", "JSON"));
		if(ACTION_UPDATE.equals(action)) {
			String recordId = args[2];
			params.append("&RR=*");
			params.append("&RecordId=");
			params.append(recordId);
			nameValuePairList.add(new BasicNameValuePair("RR", "*"));
			nameValuePairList.add(new BasicNameValuePair("RecordId", recordId));
		}
		params.append("&SignatureMethod=HMAC-SHA1");
		params.append("&SignatureNonce=");
		params.append(signatureNonce);
		params.append("&SignatureVersion=1.0");
		params.append("&Timestamp=");
		params.append(timestamp);
		nameValuePairList.add(new BasicNameValuePair("SignatureMethod", "HMAC-SHA1"));
		nameValuePairList.add(new BasicNameValuePair("SignatureNonce", signatureNonce));
		nameValuePairList.add(new BasicNameValuePair("SignatureVersion", "1.0"));
		nameValuePairList.add(new BasicNameValuePair("Timestamp", timestamp));
		if(ACTION_UPDATE.equals(action)) {
			String value = args[3];
			params.append("&Type=TXT");
			params.append("&Value=");
			params.append(value);
			nameValuePairList.add(new BasicNameValuePair("Type", "TXT"));
			nameValuePairList.add(new BasicNameValuePair("Value", value));
		}
		params.append("&Version=2015-01-09");
		nameValuePairList.add(new BasicNameValuePair("Version", "2015-01-09"));
		String stringToSign = "GET&%2F&"+URLEncoder.encode(params.toString(), "UTF-8");
		String signature = SecurityHelper.encrypt(HmacAlgorithms.HMAC_SHA_1.getName(), "UTF-8", secret+"&", stringToSign);
		String urlEncodedSignature = URLEncoder.encode(signature, "UTF-8");
		System.out.println(stringToSign+"\r\n"+signature+"\r\n"+urlEncodedSignature);
		params.append("&Signature=");
		params.append(urlEncodedSignature);
		nameValuePairList.add(new BasicNameValuePair("Signature", urlEncodedSignature));
		String url = HTTP_PREFIX+params.toString();
		System.out.println(url);
//		System.out.println(URLDecoder.decode("+15%3A20%3A31", "UTF-8"));
		String result = HttpClientHelper.get(url, null);
		/*
		 * String result = null; try { result = HttpClientHelper.post(HTTP_PREFIX, null,
		 * nameValuePairList); } catch(Exception e) {
		 * System.out.println("========\r\n"+e.getMessage().indexOf(stringToSign));
		 * throw e; }
		 */
		System.out.println(result);
	}
}