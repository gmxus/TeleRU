package org.teleru;

import android.content.SharedPreferences;
import org.telegram.messenger.NotificationCenter;
import org.teleru.utils.JObject;
import org.teleru.utils.StringUtils;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.ConnectionsManager;
import static android.content.Context.MODE_PRIVATE;

public class ProxyController implements Observers.IObserver,
        NotificationCenter.NotificationCenterDelegate
{
    public static final String TAG = ProxyController.class.getName();

    private static final ProxyController instance = new ProxyController();

    public static ProxyController getInstance()
    {
        return instance;
    }

    private String host = StringUtils.empty;
    private int port = 0;
    private String username = StringUtils.empty;
    private String password = StringUtils.empty;
    private boolean useForCalls = false;
    private boolean isAvailable = false;
    private boolean checkImmediately = false;
    private boolean isRunning = false;

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public boolean useForCalls()
    {
        return useForCalls;
    }

    public boolean isAvailable()
    {
        return isAvailable;
    }


    private ProxyController()
    {
        Observers.addObservers(this, Observers.NotifyId.ConnectionChanged);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.proxySettingsChanged);
    }

    public void init()
    {
        if (isRunning)
            return;

        isRunning = true;
        SharedPreferences storage = ApplicationController.getInstance().getMainConfigs();
        host = storage.getString(String.format("%s.host", TAG), StringUtils.empty);
        port = storage.getInt(String.format("%s.port", TAG), 0);
        password = storage.getString(String.format("%s.username", TAG), StringUtils.empty);
        password = storage.getString(String.format("%s.password", TAG), StringUtils.empty);
        useForCalls = storage.getBoolean(String.format("%s.useForCalls", TAG), false);
        isAvailable = storage.getBoolean(String.format("%s.isAvailable", TAG), false);

        long lastCheckTime = storage.getLong(String.format("%s.lastCheckTime", TAG), -1);
        long dif = System.currentTimeMillis() - lastCheckTime;
        if (dif < 0 || dif > BuildParams.PROXY_CHECK_INTERVAL_TIME)
        {
            checkImmediately = true;
            ApplicationLoader.applicationHandler.postDelayed(checkProxyInterval,
                    BuildParams.PROXY_CHECK_INTERVAL_TIME);
        }
        else
            ApplicationLoader.applicationHandler.postDelayed(checkProxyInterval, dif);

        setConnectionProxy();
    }

    @Override
    public void onNotifyReceive(Observers.NotifyId id, Object... args)
    {
        if (!isRunning)
            return;

        switch (id)
        {
            case ConnectionChanged:
                if (checkImmediately)
                {
                    ApplicationLoader.applicationHandler.removeCallbacks(checkProxy);
                    ApplicationLoader.applicationHandler.post(checkProxy);
                }
                break;
        }
    }

    private Runnable checkProxyInterval = new Runnable()
    {
        @Override
        public void run()
        {
            ApplicationLoader.applicationHandler.removeCallbacks(checkProxy);
            ApplicationLoader.applicationHandler.post(checkProxy);
            ApplicationLoader.applicationHandler.postDelayed(checkProxyInterval,
                    BuildParams.PROXY_CHECK_INTERVAL_TIME);
        }
    };

    private Runnable checkProxy = new Runnable()
    {
        @Override
        public void run()
        {
            if (!isRunning || !NetworkManager.getInstance().isConnected())
                return;

            JObject params = new JObject();
            params.putString("request", "getProxy");
            NetworkManager.getInstance().sendRequest(params, (error, response) ->
            {
                if (!isRunning)
                    return;

                if (error == null)
                {
                    try
                    {
                        String lastHost = host;
                        int lasPort = port;
                        String lastUsername = username;
                        String lastPassword = password;
                        boolean lastAvailable = isAvailable;

                        String state = response.getString("state");
                        if ("ok".equalsIgnoreCase(state))
                        {
                            host = response.getString("host");
                            port = response.getInt("port");
                            username = response.getString("username");
                            password = response.getString("password");
                            useForCalls = response.getBoolean("useForCalls");
                            isAvailable = true;
                        }
                        else
                        {
                            host = StringUtils.empty;
                            port = 0;
                            username = StringUtils.empty;
                            password = StringUtils.empty;
                            useForCalls = false;
                            isAvailable = false;
                        }

                        ApplicationController.getInstance().getMainConfigs().edit()
                                .putString(String.format("%s.host", TAG), host)
                                .putInt(String.format("%s.port", TAG), port)
                                .putString(String.format("%s.username", TAG), username)
                                .putString(String.format("%s.password", TAG), password)
                                .putBoolean(String.format("%s.useForCalls", TAG), useForCalls)
                                .putBoolean(String.format("%s.isAvailable", TAG), isAvailable)
                                .putLong(String.format("%s.lastCheckTime", TAG), System.currentTimeMillis())
                                .apply();

                        if (!StringUtils.equals(lastHost, host) ||
                                lasPort != port ||
                                !StringUtils.equals(lastUsername, username) ||
                                !StringUtils.equals(lastPassword, password) ||
                                lastAvailable != isAvailable)
                            setConnectionProxy();

                        checkImmediately = false;
                        return;
                    }
                    catch (Exception ignored) {}
                }

                ApplicationLoader.applicationHandler.postDelayed(checkProxy, 2000);
            });
        }
    };

    private void setConnectionProxy()
    {
        SharedPreferences storage = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", MODE_PRIVATE);
        boolean tlProxyEnabled = storage.getBoolean("proxy_enabled", false) && !StringUtils.isNullOrEmpty(storage.getString("proxy_ip", null));
        if (!tlProxyEnabled)
        {
            if (isAvailable)
                ConnectionsManager.native_setProxySettings(StringUtils.empty, 0, StringUtils.empty, StringUtils.empty);
        }
    }

    @Override
    public void didReceivedNotification(int id, Object... args)
    {
        if (id == NotificationCenter.proxySettingsChanged)
            setConnectionProxy();
    }

    public void cleanup()
    {
        if (!isRunning)
            return;

        isRunning = false;
        host = null;
        port = 0;
        username = null;
        password = null;
        useForCalls = false;
        isAvailable = false;
        checkImmediately = false;
        ApplicationLoader.applicationHandler.removeCallbacks(checkProxyInterval);
        ApplicationLoader.applicationHandler.removeCallbacks(checkProxy);
    }
}