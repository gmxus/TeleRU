package org.teleru.utils;

import android.util.Log;
import org.teleru.BuildParams;
import java.util.Locale;

@SuppressWarnings({"unused", "WeakerAccess"})
public class Logger
{
    public static void writeLine(Object obj, String message)
    {
        writeLine(obj.getClass().getName(), message);
    }

    public static void writeLine(String tag, String message)
    {
        if (BuildParams.DEBUG)
            Log.d("debug2", String.format(Locale.US, "[%s]: %s", tag, message));
    }
}
