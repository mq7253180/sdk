package com.quincy.sdk.helper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.util.io.Streams;

import com.quincy.core.InnerConstants;

public class PGPSecurityHelper {
	private final static int TYPE_DECRYPTION = 1;
	private final static int TYPE_ENCRYPTION = 2;

    public static PGPSecretKeyRingCollection pgpSecretKeyRingCollection(String _privateKeyLocation) throws IOException, PGPException {
		String privateKeyLocation = CommonHelper.trim(_privateKeyLocation);
		if(privateKeyLocation!=null) {
			InputStream verifyKeyInput = null;
	    	try {
				verifyKeyInput = new FileInputStream(privateKeyLocation);
				PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(verifyKeyInput), InnerConstants.KEY_FINGER_PRINT_CALCULATOR);
				return pgpSec;
			} finally {
				if(verifyKeyInput!=null)
					verifyKeyInput.close();
			}
		} else
			return null;
    }

	public static PBESecretKeyDecryptor createPBESecretKeyDecryptor(String _password, int type) throws PGPException {
		String password = CommonHelper.trim(_password);
		PBESecretKeyDecryptor decryptor = null;
		if(password!=null) {
			if(type==TYPE_DECRYPTION) {
				decryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(password.toCharArray());
			} else if(type==TYPE_ENCRYPTION) {
				Security.addProvider(new BouncyCastleProvider());
				decryptor = new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(password.toCharArray());
			}
		}
		return decryptor;
	}

	public static PGPPublicKey initPGPPublicKey(String publicKeyLocation) throws IOException, PGPException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return readSecretKey(publicKeyLocation, new PGPSecretKeyHandler<PGPPublicKey>() {
			@Override
			public boolean is(PGPPublicKey key) {
				return key.isEncryptionKey();
			}

			@Override
			public Iterator<?> getKeyRings(InputStream input) throws IOException, PGPException {
				PGPPublicKeyRingCollection pgpPub = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(input), InnerConstants.KEY_FINGER_PRINT_CALCULATOR);
				return pgpPub.getKeyRings();
			}

			@Override
			public String getMethodName() {
				return "getPublicKeys";
			}
        });
    }

	private interface PGPSecretKeyHandler<T> {
		public boolean is(T t);
		public String getMethodName();
		public Iterator<?> getKeyRings(InputStream input) throws IOException, PGPException;
	}

	private static <T> T readSecretKey(String _ascLocation, PGPSecretKeyHandler<T> h) throws IOException, PGPException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String ascLocation = CommonHelper.trim(_ascLocation);
		if(ascLocation!=null) {
			// we just loop through the collection till we find a key suitable for
	        // encryption, in the real
	        // world you would probably want to be a bit smarter about this.
			InputStream input = null;
			try {
				input = new FileInputStream(ascLocation);
				Iterator<?> keyRingIter = h.getKeyRings(input);
		        while (keyRingIter.hasNext()) {
					Object keyRing = keyRingIter.next();
		            Method method = keyRing.getClass().getMethod(h.getMethodName());
		            Iterator<?> keyIter = (Iterator<?>)method.invoke(keyRing);
		            while(keyIter.hasNext()) {
		            	Object o = keyIter.next();
		            	@SuppressWarnings("unchecked")
						T t = (T)o;
		                if(h.is(t)) {
		                    return t;
		                }
		            }
		        }
		        throw new IllegalArgumentException("Can't find encryption key in key ring.");
			} finally {
				if(input!=null)
					input.close();
			}
		} else 
			return null;
    }

	public static byte[] decryptAndVerify(PGPSecretKeyRingCollection pgpSec, PBESecretKeyDecryptor decryptor, PGPPublicKey publicKey, byte[] encryptedMessage) throws IOException, PGPException {
		ByteArrayOutputStream actualOutput = null;
		InputStream input = null;
		try {
			input = PGPUtil.getDecoderStream(new ByteArrayInputStream(encryptedMessage));
			PGPObjectFactory pgpF = new PGPObjectFactory(input, new BcKeyFingerprintCalculator());
			PGPEncryptedDataList enc = null;
			Iterator<?> it = pgpF.iterator();
			while(it.hasNext()) {
				Object o = it.next();
				if(o instanceof PGPEncryptedDataList) {
					enc = (PGPEncryptedDataList)o;
					break;
				}
			}
			it = enc.getEncryptedDataObjects();
			PGPPublicKeyEncryptedData pbe = null;
			PGPPrivateKey privateKey = null;
			while(it.hasNext()) {
				pbe = (PGPPublicKeyEncryptedData)it.next();
				PGPSecretKey pgpSecKey = pgpSec.getSecretKey(pbe.getKeyID());
				privateKey = pgpSecKey.extractPrivateKey(decryptor);
			}
			if(privateKey==null)
				throw new PGPException("Secret key for message not found.");
			input.close();
			input = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(privateKey));
			PGPObjectFactory plainFact = new PGPObjectFactory(input, new BcKeyFingerprintCalculator());
			PGPOnePassSignatureList onePassSignatureList = null;
			PGPSignatureList signatureList = null;
			actualOutput = new ByteArrayOutputStream();
			Object message = null;
			while((message = plainFact.nextObject())!=null) {
				if(message instanceof PGPCompressedData) {
					//Decrypting the compressed payload
					PGPCompressedData compressedData = (PGPCompressedData) message;
					plainFact = new PGPObjectFactory(compressedData.getDataStream(), new BcKeyFingerprintCalculator());
					message = plainFact.nextObject();
					compressedData.getAlgorithm();
				}
				if(message instanceof PGPLiteralData) {
					Streams.pipeAll(((PGPLiteralData) message).getInputStream(), actualOutput);
				} else if(message instanceof PGPOnePassSignatureList) {
					onePassSignatureList = (PGPOnePassSignatureList)message;
				} else if(message instanceof PGPSignatureList) {
					signatureList = (PGPSignatureList)message;
				} else
					throw new PGPException("Unknown message type");
			}
			if(pbe.isIntegrityProtected()&&!pbe.verify())
				throw new PGPException("Data is integrity protected but integrity is lost.");
			byte[] outputBytes = actualOutput.toByteArray();
			if(publicKey!=null) {
				if(onePassSignatureList!=null&&signatureList!=null) {
					boolean signatureMatched = false;
					for(int i=0;i<onePassSignatureList.size();i++) {
						PGPOnePassSignature ops = onePassSignatureList.get(0);
						ops.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
						ops.update(outputBytes);
						PGPSignature signature = signatureList.get(i);
						if(ops.verify(signature)) {
//							this.hashAlgorithm = ops != null ? ops.getHashAlgorithm() : 0;
							signatureMatched = true;
							break;
						}
					}
					if(!signatureMatched)
						throw new PGPException("Signature verification failed.");
				} else
					throw new PGPException("Poor PGP. Signatures not found.");
			}
			return outputBytes;
		} finally {
			if(actualOutput!=null)
				actualOutput.close();
			if(input!=null)
				input.close();
		}	
	}

	private static PGPSecretKey readSecretKey(String ascLocation) throws IOException, PGPException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return readSecretKey(ascLocation, new PGPSecretKeyHandler<PGPSecretKey>() {
			@Override
			public boolean is(PGPSecretKey key) {
				return key.isSigningKey();
			}

			@Override
			public Iterator<?> getKeyRings(InputStream input) throws IOException, PGPException {
				PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(input), InnerConstants.KEY_FINGER_PRINT_CALCULATOR);
				return pgpSec.getKeyRings();
			}

			@Override
			public String getMethodName() {
				return "getSecretKeys";
			}
        });
    }

	private final static int DEFAULT_BUFFER_SIZE = 16*1024;
	private final static byte[] BUFFER = new byte[1<<16];

	public static byte[] encryptAndSign(PGPSecretKey pgpSec, PBESecretKeyDecryptor decryptor, PGPPublicKey publicKey, byte[] message) throws PGPException, IOException {
		PGPSignatureGenerator signatureGenerator = null;
		PGPOnePassSignature onePassSignature = null;
		if(pgpSec!=null) {
			String userid = (String)pgpSec.getPublicKey().getUserIDs().next();
			PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
			subpacketGenerator.setSignerUserID(false, userid);
			PGPPrivateKey privateKey = pgpSec.extractPrivateKey(decryptor);
			signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(publicKey.getAlgorithm(), HashAlgorithmTags.SHA256));
			signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
			signatureGenerator.setHashedSubpackets(subpacketGenerator.generate());
			//Generating OnePass Signature for signing using private Key
			onePassSignature = signatureGenerator.generateOnePassVersion(false);
		}
		BcPGPDataEncryptorBuilder dataEncryptor = new BcPGPDataEncryptorBuilder(PGPEncryptedData.AES_256);
		dataEncryptor.setWithIntegrityPacket(true);//encrypt.isCheckIntegrity()
		dataEncryptor.setSecureRandom(new SecureRandom());
		PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(dataEncryptor);
		encryptedDataGenerator.addMethod((new BcPublicKeyKeyEncryptionMethodGenerator(publicKey)));
		PGPCompressedDataGenerator compressedDataGenerator = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
		PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator(true);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		OutputStream finalOut = null;
		OutputStream encOut = null;
		OutputStream compressedOut = null;
		OutputStream literalOut = null;
		InputStream input = null;
		try {
			finalOut = new BufferedOutputStream(new ArmoredOutputStream(output), DEFAULT_BUFFER_SIZE);
			encOut = encryptedDataGenerator.open(finalOut, new byte[DEFAULT_BUFFER_SIZE]);
			compressedOut = new BufferedOutputStream(compressedDataGenerator.open(encOut));
			if(onePassSignature!=null)
				onePassSignature.encode(compressedOut);
			//Compressing the encrypted data
			literalOut = literalDataGenerator.open(compressedOut, PGPLiteralData.BINARY, "filename", new Date(), new byte[1 << 16]);
			input = new ByteArrayInputStream(message);
			int bytesRead = 0;
			while((bytesRead = input.read(BUFFER))!=-1) {
				literalOut.write(BUFFER, 0, bytesRead);
				//Signing the message after encryption
				if(signatureGenerator!=null)
					signatureGenerator.update(BUFFER, 0, bytesRead);
				literalOut.flush();
			}
			//Signing the message after encryption
			literalDataGenerator.close();
			if(signatureGenerator!=null)
				signatureGenerator.generate().encode(compressedOut);
			compressedOut.close();
			literalDataGenerator.close();
			compressedDataGenerator.close();
			encryptedDataGenerator.close();
			finalOut.close();
			return output.toByteArray();
		} finally {
			if(input!=null)
				input.close();
			if(literalOut!=null)
				literalOut.close();
			if(compressedOut!=null)
				compressedOut.close();
			if(encOut!=null)
				encOut.close();
			if(output!=null)
				output.close();
		}
	}

	public static void main(String[] args) throws IOException, PGPException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String privateKeyLocation = "D:/secret/payment/payment-prod-private.asc";
		PGPPublicKey dbsPublicKey = initPGPPublicKey("D:/secret/payment/DBS_IDEAL_RAPID_PROD_PGP_PUBLIC_KEY.ASC");
		String txt = "D:/secret/messages/icc.txt";
//		PGPPublicKey publicKey = initPGPPublicKey("D:/secret/payment/DSGJPMUAT-Public.asc");
//		String txt = "D:/secret/messages/mt942.txt";
		PGPPublicKey myPublicKey = initPGPPublicKey("D:/secret/payment/hcehk-pgp-Production.asc");
		String paymentLocation = "D:/secret/messages/are.txt";
		PGPSecretKeyRingCollection pgpSec = pgpSecretKeyRingCollection(privateKeyLocation);
		InputStream in = null;
		byte[] toDecrypt = null;
		byte[] toEncrypt = null;
		try {
			in = new FileInputStream(txt);
			toDecrypt = new byte[in.available()];
			in.read(toDecrypt);
			in.close();
			in = new FileInputStream(paymentLocation);
			toEncrypt = new byte[in.available()];
			in.read(toEncrypt);
		} finally {
			if(in!=null)
				in.close();
		}
		PBESecretKeyDecryptor decryptor = null;
		byte[] b = null;
		String pwd = "hcehk8926";

		decryptor = createPBESecretKeyDecryptor(pwd, TYPE_DECRYPTION);
		b = decryptAndVerify(pgpSec, decryptor, dbsPublicKey, toDecrypt);
		System.out.println(new String(b));

		PGPSecretKey pgpSec2 = readSecretKey(privateKeyLocation);
		decryptor = createPBESecretKeyDecryptor(pwd, TYPE_ENCRYPTION);
		b = encryptAndSign(pgpSec2, decryptor, dbsPublicKey, toEncrypt);
		String encrypted = new String(b);
		System.out.println(encrypted.indexOf("END PGP MESSAGE")+"=============\r\n"+encrypted);

		b = encryptAndSign(pgpSec2, decryptor, myPublicKey, toEncrypt);
		encrypted = new String(b);
		System.out.println("Self-Encrypted: \r\n"+encrypted);
		b = decryptAndVerify(pgpSec, decryptor, myPublicKey, b);
		System.out.println("自加密后解密: \r\n"+new String(b));
	}
}
