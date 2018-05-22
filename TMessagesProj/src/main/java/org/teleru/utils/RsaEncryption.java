package org.teleru.utils;

import android.util.Base64;
import org.json.JSONObject;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;

@SuppressWarnings({"unused", "WeakerAccess"})
public class RsaEncryption
{
    private static final String Transformation = "RSA/ECB/PKCS1Padding";
    private static final int DefaultKeySize = 2048;

    public static JSONObject convertPublicKeyToJson(PublicKey key) throws Exception
    {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        RSAPublicKeySpec rsaPublicKeySpec = keyFactory.getKeySpec(key, RSAPublicKeySpec.class);
        JSONObject data = new JSONObject();
        data.put("modulus", Base64.encodeToString(rsaPublicKeySpec.getModulus().toByteArray(), Base64.NO_WRAP | Base64.URL_SAFE));
        data.put("exponent", Base64.encodeToString(rsaPublicKeySpec.getPublicExponent().toByteArray(), Base64.NO_WRAP | Base64.URL_SAFE));

        return data;
    }

    public static PublicKey convertPublicKeyFromJson(JSONObject data) throws Exception
    {
        byte[] modulesBytes = Base64.decode(data.getString("modulus"), Base64.NO_WRAP);
        byte[] exponentBytes = Base64.decode(data.getString("exponent"), Base64.NO_WRAP);
        BigInteger modulus = new BigInteger(1, modulesBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);
        RSAPublicKeySpec rsaPublicKeySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(rsaPublicKeySpec);
    }

    public static class RsaPublicEncryption
    {
        private Cipher encryption;
        private boolean isDisposed = false;


        public RsaPublicEncryption(PublicKey publicKey) throws Exception
        {
            encryption = Cipher.getInstance(Transformation);
            encryption.init(Cipher.ENCRYPT_MODE, publicKey);
        }

        public byte[] encrypt(byte[] data) throws Exception
        {
            if (isDisposed)
                throw new Exception("InvalidState");

            return encryption.doFinal(data);
        }
    }

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Cipher encryption;
    private Cipher decryption;
    private boolean isDisposed = false;

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public PublicKey getPublicKey()
    {
        return publicKey;
    }


    public RsaEncryption() throws Exception
    {
        this(DefaultKeySize);
    }

    public RsaEncryption(int keySize) throws Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();
        encryption = Cipher.getInstance(Transformation);
        encryption.init(Cipher.ENCRYPT_MODE, publicKey);
        decryption = Cipher.getInstance(Transformation);
        decryption.init(Cipher.DECRYPT_MODE, privateKey);
    }

    public byte[] encrypt(byte[] data) throws Exception
    {
        if (isDisposed)
            throw new Exception("InvalidState");

        return encryption.doFinal(data);
    }

    public byte[] decrypt(byte[] data) throws Exception
    {
        if (isDisposed)
            throw new Exception("InvalidState");

        return decryption.doFinal(data);
    }

    public void dispose()
    {
        isDisposed = true;
        encryption = null;
        decryption = null;
    }
}
