package com.matthalstead.gasami.cmep;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class PasswordUtils {

	private static final byte[] keyBytes = {
			(byte) 0x4e, (byte) 0xa8, (byte) 0xc3, (byte) 0x27, 
			(byte) 0x03, (byte) 0x75, (byte) 0x05, (byte) 0xca,
			(byte) 0x83, (byte) 0x19, (byte) 0x4b, (byte) 0x28,
			(byte) 0xf9, (byte) 0xea, (byte) 0x46, (byte) 0x34
	};

	private static final String ALGORITHM = "AES";

	public static String encrypt(String str) {
		if (str == null) {
			return null;
		}
		try {
			Cipher c = Cipher.getInstance(ALGORITHM);
			c.init(Cipher.ENCRYPT_MODE, generateKey());
			byte[] encVal = c.doFinal(str.getBytes());
			
			String encryptedValue = new String(Base64.encodeBase64URLSafe(encVal));
			return encryptedValue;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String decrypt(String str) {
		try {
			Key key = generateKey();
			Cipher c = Cipher.getInstance(ALGORITHM);
			c.init(Cipher.DECRYPT_MODE, key);
			byte[] decodedValue = Base64.decodeBase64(str.getBytes());
			byte[] decValue = c.doFinal(decodedValue);
			String decryptedValue = new String(decValue);
			return decryptedValue;
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Key generateKey() throws Exception {	
		Key key = new SecretKeySpec(keyBytes, ALGORITHM);
		return key;
	}

}
