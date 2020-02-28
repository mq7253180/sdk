package com.quincy.sdk.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;

public class MessageDigestHelper {
    private final static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6','7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    private final static String ALGORITHM_MD5 = "MD5";
    private final static String ALGORITHM_SHA1 = "SHA1";

    public static byte[] getByteArrayMD5(byte[] byteArray) throws NoSuchAlgorithmException {
        return digistByteArray(ALGORITHM_MD5, byteArray);
    }

    public static byte[] getByteArraySHA1(byte[] byteArray) throws NoSuchAlgorithmException {
    	return digistByteArray(ALGORITHM_SHA1, byteArray);
    }
    
    public static String getStringMD5(byte[] byteArray) throws NoSuchAlgorithmException {
    	return toHex(digistByteArray(ALGORITHM_MD5, byteArray));
    }
    
    public static String getStringSHA1(byte[] byteArray) throws NoSuchAlgorithmException {
    	return toHex(digistByteArray(ALGORITHM_SHA1, byteArray));
    }
    
    public static byte[] getStringMD5(File file) throws NoSuchAlgorithmException, IOException {
    	return digistFile(ALGORITHM_MD5, file);
    }
    
    public static byte[] getStringSHA1(File file) throws NoSuchAlgorithmException, IOException {
    	return digistFile(ALGORITHM_SHA1, file);
    }
    
    public static String getByteArrayMD5(File file) throws NoSuchAlgorithmException, IOException {
    	return toHex(getStringMD5(file));
    }
    
    public static String getByteArraySHA1(File file) throws NoSuchAlgorithmException, IOException {
    	return toHex(getStringSHA1(file));
    }

    private static byte[] digistByteArray(String algorithm, byte[] byteArray) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm).digest(byteArray);
    }

    private static byte[] digistFile(String algorithm, File file) throws IOException, NoSuchAlgorithmException {
    	MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
        FileInputStream in = null;
        try {
			in = new FileInputStream(file);
			FileChannel ch = in.getChannel();
	        //700000000 bytes are about 670M
	        int maxSize = 700000000;
	        long startPosition=0L;
	        long step=file.length()/maxSize;
	        if(step == 0) {
	            MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
	            messageDigest.update(byteBuffer);
	            return messageDigest.digest();
	        }
	        for(int i=0;i<step;i++) {
	            MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, startPosition,maxSize);
	            messageDigest.update(byteBuffer);
	            startPosition+=maxSize;
	        }
	        if(startPosition==file.length()) {
	            return messageDigest.digest();
	        }
	        MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, startPosition, file.length()-startPosition);
	        messageDigest.update(byteBuffer);
	        return messageDigest.digest();
		} finally {
			if(in!=null) {
				in.close();
			}
		}
    }

    private static String toHex(byte bytes[]) {
        return toHex(bytes, 0, bytes.length);
    }

    private static String toHex(byte bytes[], int m, int n) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bytes[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer sb) {
        char c0 = hexDigits[(bt & 0xf0) >> 4];
        char c1 = hexDigits[bt & 0xf];
        sb.append(c0).append(c1);
    }

    public static byte[] sign(PrivateKey privateKey, byte[] data) throws GeneralSecurityException {
	    Signature signature = Signature.getInstance("SHA1withRSA");
	    signature.initSign(privateKey);
	    signature.update(data);
	    return signature.sign();
	}

	public static boolean verify(Certificate certificate, byte[] sign, byte[] data) throws GeneralSecurityException {
        PublicKey publicKey = certificate.getPublicKey();
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(sign);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        long start = System.currentTimeMillis();
//        File big = new File("D:/Temp/Fedora-9-i386-DVD.iso");
//        File big = new File("D:/download/jboss-5.0.1.GA.zip");
//        String md5 = getFileMD5String(big);
//        int bt = 127;
//        char c0 = hexDigits[(bt & 0xf0) >> 4];
//        char c1 = hexDigits[bt & 0xf];
        String md5 = MessageDigestHelper.getStringMD5("aaa".getBytes());
        System.out.println("MD5: " + md5 + "\nDURATION: " + ((System.currentTimeMillis() - start) / 1000) + "s");
    }
}
