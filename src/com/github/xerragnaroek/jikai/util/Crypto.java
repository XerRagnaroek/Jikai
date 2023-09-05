package com.github.xerragnaroek.jikai.util;

import com.github.xerragnaroek.jikai.core.Secrets;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 *
 */
public class Crypto {
    private SecretKey key;
    private IvParameterSpec iv;
    private Cipher cipher;

    public Crypto() {
        try {
            key = new SecretKeySpec(Secrets.SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            iv = new IvParameterSpec(Secrets.IV_KEY.getBytes(StandardCharsets.UTF_8));
            cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
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
