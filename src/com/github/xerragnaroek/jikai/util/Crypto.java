package com.github.xerragnaroek.jikai.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.github.xerragnaroek.jikai.core.Secrets;

/**
 * 
 */
public class Crypto {
	private SecretKey key;
	private IvParameterSpec iv;
	private Cipher cipher;

	public Crypto() {
		try {
			key = new SecretKeySpec(Secrets.SECRET_KEY.getBytes("UTF-8"), "AES");
			iv = new IvParameterSpec(Secrets.IV_KEY.getBytes("UTF-8"));
			cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | UnsupportedEncodingException e) {
			// shouldn't ever occur as these settings are tested
		}
	}

	private void setToMode(int mode) throws CryptoException {
		try {
			cipher.init(mode, key, iv);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new CryptoException("Failed changing cipher mode!", e);
		}
	}

	public void setToEncryption() throws CryptoException {
		setToMode(Cipher.ENCRYPT_MODE);
	}

	public void setToDecryption() throws CryptoException {
		setToMode(Cipher.DECRYPT_MODE);
	}

	public String encrypt(String str) throws CryptoException {
		try {
			byte[] encrypted = cipher.doFinal(str.getBytes());
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new CryptoException("Encryption failed!", e);
		}
	}

	public String decrypt(String encrypted) throws CryptoException {
		try {
			byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
			return new String(decryptedBytes);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new CryptoException("Decryption failed!", e);
		}
	}
}
