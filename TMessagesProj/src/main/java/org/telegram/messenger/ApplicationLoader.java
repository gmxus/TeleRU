package org.telegram.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.util.Base64;
import org.teleru.ApplicationController;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import org.telegram.SQLite.DatabaseHandler;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.Favorite;
import org.telegram.ui.Components.ForegroundDetector;
import java.io.File;
import java.io.RandomAccessFile;

public class ApplicationLoader extends MultiDexApplication
{
    private static final String TAG = ApplicationLoader.class.getName();

    @SuppressLint("StaticFieldLeak")
    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;
    public static volatile long applicationMainThreadId;
    private static volatile boolean applicationInited = false;

    public static volatile boolean isScreenOn = false;
    public static volatile boolean mainInterfacePaused = true;
    public static volatile boolean mainInterfacePausedStageQueue = true;
    public static volatile long mainInterfacePausedStageQueueTime;

    public static DatabaseHandler databaseHandler;
    public static boolean SHOW_ANDROID_EMOJI;
    public static boolean KEEP_ORIGINAL_FILENAME;
    public static boolean USE_DEVICE_FONT;
    public static boolean isTeslaInstalled = false;
    public static boolean ENABLE_TAGS = true;

    private static void convertConfig()
    {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("dataconfig", Context.MODE_PRIVATE);
        if (preferences.contains("currentDatacenterId"))
        {
            SerializedData buffer = new SerializedData(32 * 1024);
            buffer.writeInt32(2);
            buffer.writeBool(preferences.getInt("datacenterSetId", 0) != 0);
            buffer.writeBool(true);
            buffer.writeInt32(preferences.getInt("currentDatacenterId", 0));
            buffer.writeInt32(preferences.getInt("timeDifference", 0));
            buffer.writeInt32(preferences.getInt("lastDcUpdateTime", 0));
            buffer.writeInt64(preferences.getLong("pushSessionId", 0));
            buffer.writeBool(false);
            buffer.writeInt32(0);
            try
            {
                String dataCentersString = preferences.getString("datacenters", null);
                if (dataCentersString != null)
                {
                    byte[] dataCentersBytes = Base64.decode(dataCentersString, Base64.DEFAULT);
                    if (dataCentersBytes != null)
                    {
                        SerializedData data = new SerializedData(dataCentersBytes);
                        buffer.writeInt32(data.readInt32(false));
                        buffer.writeBytes(dataCentersBytes, 4, dataCentersBytes.length - 4);
                        data.cleanup();
                    }
                }
            }
            catch (Exception e)
            {
                FileLog.e(e);
            }

            try
            {
                File file = new File(getFilesDirFixed(), "tgnet.dat");
                RandomAccessFile fileOutputStream = new RandomAccessFile(file, "rws");
                byte[] bytes = buffer.toByteArray();
                fileOutputStream.writeInt(Integer.reverseBytes(bytes.length));
                fileOutputStream.write(bytes);
                fileOutputStream.close();
            }
            catch (Exception e)
            {
                FileLog.e(e);
            }
            buffer.cleanup();
            preferences.edit().clear().commit();
        }
    }

    public static File getFilesDirFixed()
    {
        for (int a = 0; a < 10; a++)
        {
            File path = ApplicationLoader.applicationContext.getFilesDir();
            if (path != null)
                return path;
        }

        try
        {
            ApplicationInfo info = applicationContext.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        }
        catch (Exception e)
        {
            FileLog.e(e);
        }

        return new File("/data/data/" + ApplicationLoader.applicationContext.getPackageName() + "/files");
    }

    public static void postInitApplication()
    {
        if (applicationInited)
            return;

        applicationInited = true;
        convertConfig();

        try
        {
            LocaleController.getInstance();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            final BroadcastReceiver mReceiver = new ScreenReceiver();
            applicationContext.registerReceiver(mReceiver, filter);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            isScreenOn = pm.isScreenOn();
            FileLog.e("screen state = " + isScreenOn);
        }
        catch (Exception e)
        {
            FileLog.e(e);
        }

        UserConfig.loadConfig();
        String deviceModel;
        String systemLangCode;
        String langCode;
        String appVersion;
        String systemVersion;
        String configPath = getFilesDirFixed().toString();

        try
        {
            systemLangCode = LocaleController.getSystemLocaleStringIso639();
            langCode = LocaleController.getLocaleStringIso639();
            deviceModel = Build.MANUFACTURER + Build.MODEL;
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            appVersion = pInfo.versionName + " (" + pInfo.versionCode + ")";
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        }
        catch (Exception e)
        {
            systemLangCode = "en";
            langCode = "";
            deviceModel = "Android unknown";
            appVersion = "App version unknown";
            systemVersion = "SDK " + Build.VERSION.SDK_INT;
        }

        if (systemLangCode.trim().length() == 0)
            langCode = "en";

        if (deviceModel.trim().length() == 0)
            deviceModel = "Android unknown";

        if (appVersion.trim().length() == 0)
            appVersion = "App version unknown";

        if (systemVersion.trim().length() == 0)
            systemVersion = "SDK Unknown";

        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", Activity.MODE_PRIVATE);
        boolean enablePushConnection = preferences.getBoolean("pushConnection", true);

        MessagesController.getInstance();
        ConnectionsManager.getInstance().init(BuildVars.BUILD_VERSION, TLRPC.LAYER, BuildVars.APP_ID,
                deviceModel, systemVersion, appVersion, langCode, systemLangCode, configPath,
                FileLog.getNetworkLogPath(), UserConfig.getClientUserId(), enablePushConnection);

        if (UserConfig.getCurrentUser() != null)
        {
            MessagesController.getInstance().putUser(UserConfig.getCurrentUser(), true);
            ConnectionsManager.getInstance().applyCountryPortNumber(UserConfig.getCurrentUser().phone);
            MessagesController.getInstance().getBlockedUsers(true);
            SendMessagesHelper.getInstance().checkUnsentMessages();
        }

        ApplicationLoader app = (ApplicationLoader) ApplicationLoader.applicationContext;
        app.initPlayServices();
        FileLog.e("app initied");

        ContactsController.getInstance().checkAppAccount();
        MediaController.getInstance();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        applicationContext = getApplicationContext();
        NativeLoader.initNativeLibs(ApplicationLoader.applicationContext);
        ConnectionsManager.native_setJava(Build.VERSION.SDK_INT == 14 || Build.VERSION.SDK_INT == 15);
        new ForegroundDetector(this);

        applicationHandler = new Handler(applicationContext.getMainLooper());
        applicationMainThreadId = Thread.currentThread().getId();
        //plus
        //long startTime = System.currentTimeMillis();
        databaseHandler = new DatabaseHandler(applicationContext);
        Favorite.getInstance();

        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("plusconfig", Activity.MODE_PRIVATE);
        SHOW_ANDROID_EMOJI = plusPreferences.getBoolean("showAndroidEmoji", false);
        KEEP_ORIGINAL_FILENAME = plusPreferences.getBoolean("keepOriginalFilename", false);
        USE_DEVICE_FONT = plusPreferences.getBoolean("useDeviceFont", false);
        isTeslaInstalled = AndroidUtilities.isAppInstalled(this, "com.teslacoilsw.notifier");
        ApplicationController.getInstance().init();
        startPushService();
    }

    public static void startPushService()
    {
        SharedPreferences preferences = applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);
        if (preferences.getBoolean("pushService", true))
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                ComponentName serviceComponent = new ComponentName(applicationContext, NotificationsJobService.class);
                JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
                builder.setMinimumLatency(1000);
                builder.setOverrideDeadline(2000);
                builder.setRequiresCharging(false);
                JobScheduler jobScheduler = applicationContext.getSystemService(JobScheduler.class);
                if (jobScheduler != null)
                    jobScheduler.schedule(builder.build());
            }
            else
                applicationContext.startService(new Intent(applicationContext, NotificationsService.class));
        }
        else
            stopPushService();
    }

    public static void stopPushService()
    {
        applicationContext.stopService(new Intent(applicationContext, NotificationsService.class));

        PendingIntent pIntent = PendingIntent.getService(applicationContext, 0, new Intent(applicationContext, NotificationsService.class), 0);
        AlarmManager alarm = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null)
            alarm.cancel(pIntent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        try
        {
            LocaleController.getInstance().onDeviceConfigurationChange(newConfig);
            AndroidUtilities.checkDisplaySize(applicationContext, newConfig);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void initPlayServices()
    {
        AndroidUtilities.runOnUIThread(() -> {
            if (checkPlayServices())
            {
                if (UserConfig.pushString != null && UserConfig.pushString.length() != 0)
                    FileLog.d("GCM regId = " + UserConfig.pushString);
                else
                    FileLog.d("GCM Registration not found.");

                try
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        ComponentName serviceComponent = new ComponentName(applicationContext, GcmRegistrationIntentJobService.class);
                        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
                        builder.setMinimumLatency(1000);
                        builder.setOverrideDeadline(2000);
                        builder.setRequiresCharging(false);
                        JobScheduler jobScheduler = applicationContext.getSystemService(JobScheduler.class);
                        if (jobScheduler != null)
                            jobScheduler.schedule(builder.build());
                    }
                    else
                    {
                        Intent intent = new Intent(applicationContext, GcmRegistrationIntentService.class);
                        startService(intent);
                    }
                }
                catch (Exception ignored) {}
            }
            else
                FileLog.d("No valid Google Play Services APK found.");
        }, 1000);
    }

    private boolean checkPlayServices()
    {
        try
        {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            return resultCode == ConnectionResult.SUCCESS;
        }
        catch (Exception e)
        {
            FileLog.e(e);
        }

        return true;
    }
}
