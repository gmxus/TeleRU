package org.teleru.utils;

import org.json.JSONObject;

public class JObject extends JSONObject
{
    public void putInt(String key, int val)
    {
        putInternal(key, val);
    }

    public void putLong(String key, int val)
    {
        putInternal(key, val);
    }

    public void putFloat(String key, float val)
    {
        putInternal(key, val);
    }

    public void putString(String key, String val)
    {
        putInternal(key, val);
    }

    private void putInternal(String key, Object val)
    {
        try
        {
            put(key, val);
        }
        catch (Exception ignored) {}
    }
}
