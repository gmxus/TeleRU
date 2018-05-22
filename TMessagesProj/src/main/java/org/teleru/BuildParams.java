package org.teleru;

import java.nio.charset.Charset;

@SuppressWarnings("WeakerAccess")
public class BuildParams
{
    public static final String APP_NAME = "TeleRU";

    private static byte[] SERVER_ADDRESS = new byte[] { 104, 116, 116, 112, 58, 47, 47, 116, 101, 108, 101, 114, 117, 46, 111, 114, 103 };

    public static final boolean DEBUG = false;

    public static final float VERSION_CODE = 1.0f;

    public static final int PROXY_CHECK_INTERVAL_TIME = 1000 * 60 * 2; // 2 min

    public static final int UPDATE_CHECK_INTERVAL_TIME = 1000 * 60 * 5; // 5 min

    public static final int DEFAULT_CONNECTION_TIME_OUT = 1000 * 10; // 10 sec

    public static String getServiceAddress()
    {
        return new String(SERVER_ADDRESS, Charset.forName("UTF-8"));
    }
}
