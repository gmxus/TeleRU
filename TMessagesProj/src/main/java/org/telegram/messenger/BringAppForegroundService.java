package org.telegram.messenger;

import android.app.IntentService;
import android.content.Intent;

import org.telegram.ui.LaunchActivity;

public class BringAppForegroundService extends IntentService
{
    public BringAppForegroundService()
    {
        super("BringAppForegroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Intent intent2 = new Intent(this, LaunchActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent2);
    }
}
