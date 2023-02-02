package com.miraclegarden.payrobot;


import com.example.nativelib.NativeLib;
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.codec.binary.Base64;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @Classname ZzSecurityHelper
 * @Description TODO
 * @Date 2019/6/24 16:50
 * @Created by whd
 */
public class ShortcutEncryption {

    public static String java_openssl_encrypt(String data) {
        NativeLib nativeLib=new NativeLib();
        return java_openssl_encrypt(data, nativeLib.stringFromJNI1());
    }

    /**
     * 加密算法
     * @param data 数据
     * @param iv IV
     * @return
     */
    public static String java_openssl_encrypt(String data, String iv) {
        try {
            Cipher cipher = createCipher(iv, Cipher.ENCRYPT_MODE);
            return Base64.encodeBase64String(cipher.doFinal(data.getBytes()));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static String java_openssl_decrypt(String data) {
        NativeLib nativeLib=new NativeLib();
        return java_openssl_decrypt(data, nativeLib.stringFromJNI1());
    }


    /**
     * java_openssl_decrypt解密
     * @param data
     * @param iv
     * @return
     */
    public static String java_openssl_decrypt(String data, String iv) {
        try {

            Cipher cipher = createCipher(iv, Cipher.DECRYPT_MODE);
            return new String(cipher.doFinal(Base64.decodeBase64(data)));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {

            e.printStackTrace();

        }
        return null;

    }


    /**
     * 创建密码器Cipher
     * @param iv
     * @param mode
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    private static Cipher createCipher(String iv, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        NativeLib nativeLib=new NativeLib();
        byte[] key = nativeLib.stringFromJNI().getBytes();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
        cipher.init(mode, new SecretKeySpec(key, "AES"), ivParameterSpec);
        return cipher;
    }
}