package com.quincy.sdk.helper;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class RSASecurityHelper {
	private static final String PUBLIC_KEY = "RSAPublicKey";
    private static final String PRIVATE_KEY = "RSAPrivateKey";
    public static final String PUBLIC_KEY_BASE64 = "RSAPublicKey_Base64";
    public static final String PRIVATE_KEY_BASE64 = "RSAPrivateKey_Base64";
	public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHMS_MD5_RSA = "MD5withRSA";
    public static final String SIGNATURE_ALGORITHMS_SHA1_RSA = "SHA1WithRSA";
    private final static int MAX_ENCRYPT_BLOCK = 117;
    private final static int MAX_DECRYPT_BLOCK = 128;

    public static PublicKey extractPublicKeyFromCer(String cerLocation) throws CertificateException, IOException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream in = null;
		try {
			in = new FileInputStream(cerLocation);
			Certificate c = cf.generateCertificate(in);
            PublicKey publicKey = c.getPublicKey();
            return publicKey;
		} finally {
			if(in!=null)
				in.close();
		}
	}

    public static PublicKey extractPublicKeyByStr(String _publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    	Decoder base64Decoder = Base64.getDecoder();
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		byte[] encodedPublicKey = base64Decoder.decode(_publicKey);
		PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey));
		return publicKey;
	}

    public static KeyStore loadKeyStore(String jksLocation, String jksPwd) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
    	KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    	InputStream in = null;
		try {
			in = new FileInputStream(jksLocation);
			keyStore.load(in, jksPwd.toCharArray());
			return keyStore;
		} finally {
			if(in!=null)
				in.close();
		}
	}

    public static SSLSocketFactory createSSLSocketFactory(KeyStore keyStore, String jksPwd) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, jksPwd.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.1");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        return sslSocketFactory;
	}

    public static Key extractKey(KeyStore keyStore, String jksPwd, String alias) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
		return keyStore.getKey(alias, jksPwd.toCharArray());
	}

    public static PrivateKey extractPrivateKey(String _privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    	Decoder base64Decoder = Base64.getDecoder();
		PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(base64Decoder.decode(_privateKey));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		PrivateKey privateKey = keyFactory.generatePrivate(priPKCS8);
		return privateKey;
	}

    //map对象中存放公私钥
    public static Map<String, Object> generateKeyPair() throws NoSuchAlgorithmException {
        //获得对象 KeyPairGenerator 参数 RSA 1024个字节
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        keyPairGen.initialize(1024, new SecureRandom());
        //通过对象 KeyPairGenerator 获取对象KeyPair
        KeyPair keyPair = keyPairGen.generateKeyPair();
        //通过对象 KeyPair 获取RSA公私钥对象RSAPublicKey RSAPrivateKey
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        //公私钥对象存入map中
        Encoder base64Encoder = Base64.getEncoder();
        Map<String, Object> keyMap = new HashMap<String, Object>(8);
        keyMap.put(PUBLIC_KEY, publicKey);
        keyMap.put(PRIVATE_KEY, privateKey);
        keyMap.put(PUBLIC_KEY_BASE64, base64Encoder.encode(publicKey.getEncoded()));
        keyMap.put(PRIVATE_KEY_BASE64, base64Encoder.encode(privateKey.getEncoded()));
        return keyMap;
    }

	public static RSAPublicKey loadPublicKeyByStr(String publicKeyStr) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		Decoder base64Decoder = Base64.getDecoder();
		byte[] buffer = base64Decoder.decode(publicKeyStr);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(buffer);
		return (RSAPublicKey) keyFactory.generatePublic(keySpec);
	}

	public static RSAPrivateKey loadPrivateKeyByStr(String privateKeyStr) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
		Decoder base64Decoder = Base64.getDecoder();
		byte[] buffer = base64Decoder.decode(privateKeyStr);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
		return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
	}

    public static byte[] crypt(Key key, byte[] plainTextData, int opmode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {  
		int MAX_CRYPT_BLOCK = -1;
		switch(opmode) {
			case Cipher.ENCRYPT_MODE: MAX_CRYPT_BLOCK = MAX_ENCRYPT_BLOCK;break;
			case Cipher.DECRYPT_MODE: MAX_CRYPT_BLOCK = MAX_DECRYPT_BLOCK;break;
			default:;
		}
		Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
		cipher.init(opmode, key);
		int loops = (plainTextData.length/MAX_CRYPT_BLOCK);
		if(plainTextData.length%MAX_CRYPT_BLOCK>0)
			loops++;
		ByteArrayOutputStream out = null;
		try {
			out = new ByteArrayOutputStream();
			for(int i=0;i<loops;i++) {
				int start = i*MAX_CRYPT_BLOCK;
				int remaining = plainTextData.length-start;
				int offset = remaining>MAX_CRYPT_BLOCK?MAX_CRYPT_BLOCK:remaining;
				out.write(cipher.doFinal(plainTextData, start, offset));
			}
			return out.toByteArray();
		} finally {
			if(out!=null)
				out.close();
		}
	}

	private static final char[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String byteArrayToString(byte[] data) {
		StringBuilder sb = new StringBuilder();
    	for(int i=0;i<data.length;i++) {
			// 取出字节的高四位 作为索引得到相应的十六进制标识符 注意无符号右移 
			sb.append(HEX_CHAR[(data[i] & 0xf0) >>> 4]);
			// 取出字节的低四位 作为索引得到相应的十六进制标识符 
			sb.append(HEX_CHAR[(data[i] & 0x0f)]);
			if(i<data.length-1)
				sb.append(' ');
		}
		return sb.toString();
	}

	private static String sign(PrivateKey privateKey, String algorithms, byte[] content) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException {
		Encoder base64Encoder = Base64.getEncoder();
		Signature signature = Signature.getInstance(algorithms);
		signature.initSign(privateKey);
		signature.update(content);
		byte[] signed = signature.sign();
		return new String(base64Encoder.encode(signed));
	}

	public static String sign(PrivateKey privateKey, String algorithms, String charset, String content) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException {
		return sign(privateKey, algorithms, content.getBytes(getDefaultCharset(charset)));
	}

	public static String sign(String _privateKey, String algorithms, String charset, String content) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException {
		PrivateKey privateKey = RSASecurityHelper.extractPrivateKey(_privateKey);
		return sign(privateKey, algorithms, content, charset);
	}

	public static boolean verify(PublicKey publicKey, String algorithms, String signatureStr, String content, String charset) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException {
		Decoder base64Decoder = Base64.getDecoder();
		Signature signature = Signature.getInstance(algorithms);
		signature.initVerify(publicKey);
		signature.update(content.getBytes(getDefaultCharset(charset)));
		boolean bverify = signature.verify(base64Decoder.decode(signatureStr));
		return bverify;
	}

	public static boolean verify(String _publicKey, String algorithms, String signatureStr, String content, String encode) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, SignatureException, IOException {
		PublicKey publicKey = RSASecurityHelper.extractPublicKeyByStr(_publicKey);
		return RSASecurityHelper.verify(publicKey, algorithms, signatureStr, content, encode);
	}

	private static String getDefaultCharset(String _charset) {
		String charset = CommonHelper.trim(_charset);
		return charset==null?"UTF-8":charset;
	}

	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, SignatureException, IOException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		String content = "ABCDEFGHIGKLMNOPQRSTUVWXYZ";
    	for(int i=0;i<3;i++) {
    		content += content;
    	}
    	String signatureAlgorithms = SIGNATURE_ALGORITHMS_SHA1_RSA;
    	String charset = "UTF-8";
    	Map<String, Object> keyMap = generateKeyPair();
        String publicKey = keyMap.get(PUBLIC_KEY_BASE64).toString().replaceAll("\r\n", "");
        String privateKey = keyMap.get(PRIVATE_KEY_BASE64).toString().replaceAll("\r\n", "");
        String signature = sign(privateKey, signatureAlgorithms, charset, content);
        byte[] encryption = crypt(loadPrivateKeyByStr(privateKey), content.getBytes(), Cipher.ENCRYPT_MODE);
        String decryption = new String(crypt(loadPublicKeyByStr(publicKey), encryption, Cipher.DECRYPT_MODE));

        System.out.println("Content: "+content+"\r\nSignature: "+signature);
        System.out.println("-----------------");
        System.out.println(verify(publicKey, signatureAlgorithms, signature, content, charset));
        System.out.println("-----------------");
        System.out.println("Encryption: "+new String(encryption));
        System.out.println("-----------------");
        System.out.println("Decryption: "+decryption);
        System.out.println("*****************");

        System.out.println(keyMap.get(PUBLIC_KEY));
        System.out.println("-----------------");
        System.out.println(keyMap.get(PRIVATE_KEY));
        System.out.println("==========================");
        System.out.println(publicKey.length()+": "+publicKey);
        System.out.println("-----------------");
        System.out.println(privateKey.length()+": "+privateKey);
	}
}
