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

import org.apache.commons.codec.digest.HmacAlgorithms;

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
		String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(c.getTime());
		timestamp = timestamp.replaceAll("\\s+", "T")+"Z";
		timestamp = URLEncoder.encode(timestamp, CHARSET_UTF8);
		StringBuilder params = new StringBuilder(500).append("AccessKeyId=").append(id).append("&Action=").append(action);
		if("DescribeDomainRecords".equals(action))
			params.append("&DomainName=").append(domain);
		params.append("&Format=JSON");
		if(ACTION_UPDATE.equals(action))
			params.append("&RR=%2A&RecordId=").append(args[3]);
		params.append("&SignatureMethod=HMAC-SHA1&SignatureNonce=").append(signatureNonce).append("&SignatureVersion=1.0&Timestamp=").append(timestamp);
		if(ACTION_UPDATE.equals(action))
			params.append("&Type=TXT&Value=").append(args[4]);
		params.append("&Version=2015-01-09");
		String stringToSign = "GET&%2F&"+URLEncoder.encode(params.toString(), CHARSET_UTF8);
		String signature = SecurityHelper.encrypt(HmacAlgorithms.HMAC_SHA_1.getName(), CHARSET_UTF8, secret+"&", stringToSign);
		String urlEncodedSignature = URLEncoder.encode(signature, CHARSET_UTF8);
		System.out.println(stringToSign+"\r\n"+signature+"\r\n"+urlEncodedSignature);
		params.append("&Signature=").append(urlEncodedSignature);
		String url = HTTP_PREFIX+params.toString();
		System.out.println(url);
//		System.out.println(URLDecoder.decode("%2A", CHARSET_UTF8)+"---"+URLEncoder.encode("*", CHARSET_UTF8));
		SimplifiedHttpResponse response = HttpClientHelper.get(url, null, null);
		String result = response.getContent();
		System.out.println(result);
	}
}