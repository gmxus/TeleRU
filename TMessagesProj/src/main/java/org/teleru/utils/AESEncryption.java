package org.teleru.utils;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("WeakerAccess")
public class AESEncryption
{
    private static final String MODE = "AES";
    private static final String Transformation = "AES/CBC/NoPadding";
    private static final int DefaultKeySize = 256;
    private static final int IvSize = 16;

    public static byte[] generateKey(int keySize) throws Exception
    {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(keySize, new SecureRandom());

        return generator.generateKey().getEncoded();
    }

    public static byte[] generateIV()
    {
        byte[] iv = new byte[IvSize];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        return iv;
    }

    private byte[] key;
    private byte[] iv;
    private Cipher encryption;
    private Cipher decryption;
    private boolean isDisposed = false;

    public byte[] getKey()
    {
        return key;
    }

    public byte[] getIv()
    {
        return iv;
    }


    public AESEncryption() throws Exception
    {
        this(generateKey(DefaultKeySize), generateIV());
    }

    public AESEncryption(byte[] key, byte[] iv) throws Exception
    {
        this.key = key;
        this.iv = iv;
        SecretKeySpec keySpec = new SecretKeySpec(key, MODE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        encryption = Cipher.getInstance(Transformation);
        encryption.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        decryption = Cipher.getInstance(Transformation);
        decryption.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
    }

    public byte[] encrypt(byte[] data) throws Exception
    {
        if (isDisposed)
            throw new Exception("InvalidState");

        return encryption.doFinal(addPadBytes(encryption.getBlockSize(), data));
    }

    public byte[] decrypt(byte[] data) throws Exception
    {
        if (isDisposed)
            throw new Exception("InvalidState");

        return decryption.doFinal(data);
    }

    private byte[] addPadBytes(int blockSize, byte[] input)
    {
        byte padded[] = new byte[input.length + blockSize - (input.length % blockSize)];
        System.arraycopy(input, 0, padded, 0, input.length);

        for (int i = 0; i < blockSize - (input.length % blockSize); i++)
            padded[input.length + i] = 0;

        return padded;
    }

    public void dispose()
    {
        isDisposed = true;
        encryption = null;
        decryption = null;
    }
}
