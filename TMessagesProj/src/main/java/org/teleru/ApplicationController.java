package org.teleru;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import org.teleru.utils.HashUtils;
import org.telegram.messenger.ApplicationLoader;
import java.util.Locale;

public class ApplicationController
{
    public static final String TAG = ApplicationController.class.getName();

    private final static ApplicationController instance = new ApplicationController();

    public static ApplicationController getInstance()
    {
        return instance;
    }

    private SharedPreferences mainConfigs;


    SharedPreferences getMainConfigs()
    {
        return mainConfigs;
    }

    public String getId()
    {
        String id = getMainConfigs().getString(String.format("%s.id", TAG), null);
        if (id == null)
        {
            id = HashUtils.md5(String.format(Locale.US, "%s.%s.%d", Build.MODEL, Build.MANUFACTURER, System.currentTimeMillis()));
            getMainConfigs().edit().putString(String.format("%s.id", TAG), id).apply();
        }

        return id;
    }

    private ApplicationController()
    {
        mainConfigs = ApplicationLoader.applicationContext.getSharedPreferences(ApplicationController.class.getName(), Context.MODE_PRIVATE);
    }

    public void init()
    {
        ExportController.getInstance().init();
        ProxyController.getInstance().init();
        UpdaterController.getInstance().init();
        NetworkManager.getInstance().init();
    }

    public void cleanup()
    {
        ExportController.getInstance().cleanup();
        UpdaterController.getInstance().cleanup();
        ProxyController.getInstance().cleanup();
        NetworkManager.getInstance().cleanup();
    }
}
