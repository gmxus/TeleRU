package org.teleru.utils;

import android.util.Base64;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Base64Utils
{
    public static String encode(byte[] data)
    {
        return Base64.encodeToString(data, Base64.NO_WRAP).replace('+', '-').replace('/', '_').replace('=', ',');
    }

    public static byte[] decode(String data)
    {
        return Base64.decode(data.replace('-', '+').replace('_', '/').replace(',', '='), Base64.NO_WRAP);
    }
}
