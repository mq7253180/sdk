package com.quincy.sdk.helper;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecurityHelper {
	public static byte[] encrypt(String algorithms, String _charset, String _secret, byte[] toEncrypt) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		String charset = CommonHelper.trim(_charset);
		if(charset==null)
			charset = "UTF-8";
        byte[] secret = _secret.getBytes(charset);
        SecretKey secretKey = new SecretKeySpec(secret, algorithms);
        Mac mac = Mac.getInstance(algorithms);
        mac.init(secretKey);
        byte[] b = mac.doFinal(toEncrypt);
        return b;
    }

	public static byte[] encryptAsBase64(String algorithms, String charset, String secret, byte[] toEncrypt) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		byte[] b = encrypt(algorithms, charset, secret, toEncrypt);
		b = Base64.getEncoder().encode(b);
        return b;
    }

	public static String encrypt(String algorithms, String _charset, String secret, String _toEncrypt) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
		String charset = CommonHelper.trim(_charset);
		if(charset==null)
			charset = "UTF-8";
		byte[] toEncrypt = _toEncrypt.getBytes(charset);
		byte[] b = encryptAsBase64(algorithms, charset, secret, toEncrypt);
        String signature = new String(b);
        return signature;
    }
}