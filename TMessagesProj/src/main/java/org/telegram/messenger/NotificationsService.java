package org.telegram.messenger;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

public class NotificationsService extends Service
{
    public static final String TAG = NotificationsService.class.getName();


    @Override
    public void onCreate()
    {
        FileLog.e("service started");
        ApplicationLoader.postInitApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public void onDestroy()
    {
        FileLog.e("service destroyed");
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("Notifications", MODE_PRIVATE);
        if (preferences.getBoolean("pushService", true))
        {
            Intent intent = new Intent("org.telegram.start");
            sendBroadcast(intent);
        }
    }
}
