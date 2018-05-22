package org.teleru;

import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.teleru.utils.JObject;
import java.util.Locale;

@SuppressWarnings("unused")
public class UpdaterController implements Observers.IObserver
{
    private static final String TAG = UpdaterController.class.getName();

    private static UpdaterController instance = new UpdaterController();

    public static UpdaterController getInstance()
    {
        return instance;
    }

    private String updateUrl = null;
    private double updateVersion = 0;
    private boolean isAvailable = false;
    private boolean checkImmediately = false;
    private boolean isRunning = false;

    public String getUpdateUrl()
    {
        return updateUrl;
    }

    public double getUpdateVersion()
    {
        return updateVersion;
    }

    public boolean isAvailable()
    {
        return isAvailable;
    }


    private UpdaterController()
    {
        Observers.addObservers(this, Observers.NotifyId.ConnectionChanged);
    }

    public void init()
    {
        if (isRunning)
            return;

        SharedPreferences storage = ApplicationController.getInstance().getMainConfigs();
        isRunning = true;
        updateUrl = storage.getString(String.format("%s.updateUrl", TAG), null);
        updateVersion = storage.getFloat(String.format("%s.updateVersion", TAG), -1);
        isAvailable = storage.getBoolean(String.format("%s.isAvailable", TAG), false);

        if (isAvailable && updateVersion == BuildParams.VERSION_CODE)
        {
            updateUrl = null;
            updateVersion = -1;
            isAvailable = false;
            storage.edit()
                    .putString(String.format("%s.updateUrl", TAG), null)
                    .putFloat(String.format("%s.updateVersion", TAG), 0)
                    .putBoolean(String.format("%s.isAvailable", TAG), false)
                    .apply();
        }

        long lastCheckTime = storage.getLong(String.format("%s.lastCheckTime", TAG), -1);
        long dif = System.currentTimeMillis() - lastCheckTime;
        if (dif < 0 || dif > BuildParams.UPDATE_CHECK_INTERVAL_TIME)
        {
            checkImmediately = true;
            ApplicationLoader.applicationHandler.postDelayed(checkUpdateInterval, BuildParams.UPDATE_CHECK_INTERVAL_TIME);
        }
        else
            ApplicationLoader.applicationHandler.postDelayed(checkUpdateInterval, dif);
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
                    ApplicationLoader.applicationHandler.removeCallbacks(checkUpdate);
                    ApplicationLoader.applicationHandler.post(checkUpdate);
                }
                break;
        }
    }

    private Runnable checkUpdateInterval = new Runnable()
    {
        @Override
        public void run()
        {
            if (!isRunning)
                return;

            ApplicationLoader.applicationHandler.post(checkUpdate);
            ApplicationLoader.applicationHandler.postDelayed(checkUpdateInterval, BuildParams.UPDATE_CHECK_INTERVAL_TIME);
        }
    };

    private Runnable checkUpdate = new Runnable()
    {
        @Override
        public void run()
        {
            if (!isRunning || !NetworkManager.getInstance().isConnected())
                return;

            long lastCheckTime = System.currentTimeMillis() - ApplicationController.getInstance().getMainConfigs().
                    getLong(String.format(Locale.US, "%s.lastCheckTime", TAG), -1);

            if (lastCheckTime > 0 && lastCheckTime < (1000 * 60 * 2))
                return;

            JObject params = new JObject();
            params.putString("request", "checkUpdate");
            params.putString("platform", "android");
            params.putFloat("currentVersion", BuildParams.VERSION_CODE);
            NetworkManager.getInstance().sendRequest(params, (error, response) ->
            {
                if (error == null)
                {
                    try
                    {
                        boolean hasChanges = false;
                        String state = response.getString("state");
                        if ("updateReady".equalsIgnoreCase(state))
                        {
                            updateUrl = response.getString("updateUrl");
                            updateVersion = (float) response.getDouble("updateVersion");
                            isAvailable = true;
                            hasChanges = true;
                        }
                        else if (isAvailable)
                        {
                            updateUrl = null;
                            updateVersion = BuildParams.VERSION_CODE;
                            isAvailable = false;
                            hasChanges = true;
                        }

                        if (hasChanges)
                        {
                            saveSettings(updateUrl, (float) updateVersion, isAvailable, System.currentTimeMillis());
                            Observers.pushAsync(isAvailable ? Observers.NotifyId.UpdateAvailable : Observers.NotifyId.UpdateOk);
                        }

                        checkImmediately = false;
                        return;
                    }
                    catch (Exception ignored) {}
                }

                ApplicationLoader.applicationHandler.postDelayed(checkUpdate, 1000 * 2);
            });
        }
    };

    private void saveSettings(String updateUrl, float version, boolean isAvailable, long lastCheckTime)
    {
        ApplicationController.getInstance().getMainConfigs().edit()
                .putString(String.format(Locale.US, "%s.updateUrl", TAG), updateUrl)
                .putFloat(String.format(Locale.US, "%s.updateVersion", TAG), version)
                .putBoolean(String.format(Locale.US, "%s.isAvailable", TAG), isAvailable)
                .putLong(String.format(Locale.US, "%s.lastCheckTime", TAG), lastCheckTime)
                .apply();
    }

    public void cleanup()
    {
        if (!isRunning)
            return;

        isRunning = false;
        isAvailable = false;
        ApplicationLoader.applicationHandler.removeCallbacks(checkUpdateInterval);
        ApplicationLoader.applicationHandler.removeCallbacks(checkUpdate);
    }
}
