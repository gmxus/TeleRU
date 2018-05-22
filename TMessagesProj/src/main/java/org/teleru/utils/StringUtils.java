package org.teleru.utils;

import java.nio.charset.Charset;

@SuppressWarnings("ALL")
public class StringUtils
{
    public static final String empty = "";

    public static boolean isNullOrEmpty(String s)
    {
        return s == null || s.length() == 0;
    }

    public static boolean equals(String a, String b)
    {
        return (a == b) || (a != null && a.equals(b));
    }

    public static String getUTFString(byte[] data)
    {
        if (data == null)
            return null;

        if (data.length == 0)
            return empty;

        return new String(data, Charset.forName("UTF-8"));
    }

    public static byte[] getUTFBytes(String str)
    {
        if (str == null)
            return null;

        return str.getBytes(Charset.forName("UTF-8"));
    }
}
