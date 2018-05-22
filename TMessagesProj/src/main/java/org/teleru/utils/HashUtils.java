package org.teleru.utils;

import java.security.MessageDigest;

@SuppressWarnings({"unused", "WeakerAccess"})
public class HashUtils
{
    public static String sha1(String input)
    {
        try
        {
            MessageDigest mDigest = MessageDigest.getInstance("SHA1");
            byte[] result = mDigest.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();

            for (byte aResult : result)
                sb.append(Integer.toString((aResult & 0xff) + 0x100, 16).substring(1));

            return sb.toString();
        }
        catch (Exception ignored) {}
        return null;
    }

    public static String md5(String string)
    {
        try
        {
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            byte[] result = mDigest.digest(string.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(result.length * 2);

            for (byte b : result)
            {
                int i = (b & 0xFF);
                if (i < 0x10)
                    sb.append('0');

                sb.append(Integer.toHexString(i));
            }

            return sb.toString();
        }
        catch (Exception ignored) {}
        return null;
    }
}