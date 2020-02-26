package com.quincy.sdk.helper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.UUID;

public class AliyunDNTXTUpdate {
	private final static String HTTP_PREFIX = "https://alidns.aliyuncs.com/?";
	private final static String ACTION_UPDATE = "UpdateDomainRecord";
	private final static String CHARSET_UTF8 = "UTF-8";

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
		String action = args[1];
		String domain = args[2];
		String signatureNonce = UUID.randomUUID().toString().replaceAll("-", "");
		Calendar c = Calendar.getInstance();
		int zoneOffset = c.get(Calendar.ZONE_OFFSET);
		c.add(Calendar.HOUR, -(zoneOffset/1000/3600));
		String _timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(c.getTime());
		String timestamp = _timestamp.replaceAll("\\s+", "T")+"Z";
		timestamp = URLEncoder.encode(timestamp, CHARSET_UTF8);
		StringBuilder params = new StringBuilder(500);
		params.append("AccessKeyId=");
		params.append(id);
		params.append("&Action=");
		params.append(action);
		if("DescribeDomainRecords".equals(action)) {
			params.append("&DomainName=");
			params.append(domain);
		}
		params.append("&Format=JSON");
		if(ACTION_UPDATE.equals(action)) {
			String recordId = args[3];
			params.append("&RR=%2A");
			params.append("&RecordId=");
			params.append(recordId);
		}
		params.append("&SignatureMethod=HMAC-SHA1");
		params.append("&SignatureNonce=");
		params.append(signatureNonce);
		params.append("&SignatureVersion=1.0");
		params.append("&Timestamp=");
		params.append(timestamp);
		if(ACTION_UPDATE.equals(action)) {
			String value = args[4];
			params.append("&Type=TXT");
			params.append("&Value=");
			params.append(value);
		}
		params.append("&Version=2015-01-09");
		String stringToSign = "GET&%2F&"+URLEncoder.encode(params.toString(), CHARSET_UTF8);
		//HmacAlgorithms.HMAC_SHA_1.getName()
		String signature = SecurityHelper.encrypt("HmacSHA1", CHARSET_UTF8, secret+"&", stringToSign);
		String urlEncodedSignature = URLEncoder.encode(signature, CHARSET_UTF8);
		System.out.println(stringToSign+"\r\n"+signature+"\r\n"+urlEncodedSignature);
		params.append("&Signature=");
		params.append(urlEncodedSignature);
		String url = HTTP_PREFIX+params.toString();
		System.out.println(url);
//		System.out.println(URLDecoder.decode("%2A", CHARSET_UTF8)+"---"+URLEncoder.encode("*", CHARSET_UTF8));
		String result = HttpClientHelper.get(url, null);
		System.out.println(result);
	}
}