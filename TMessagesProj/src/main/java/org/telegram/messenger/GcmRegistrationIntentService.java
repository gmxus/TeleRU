package org.telegram.messenger;

import android.app.IntentService;
import android.content.Intent;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

public class GcmRegistrationIntentService extends IntentService
{
    public GcmRegistrationIntentService()
    {
        super("GcmRegistrationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        try
        {
            InstanceID instanceID = InstanceID.getInstance(this);
            //final String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            final String token = instanceID.getToken(BuildVars.GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            FileLog.d("GCM Registration Token: " + token);
            AndroidUtilities.runOnUIThread(() -> {
                ApplicationLoader.postInitApplication();
                sendRegistrationToServer(token);
            });
        }
        catch (Exception e)
        {
            FileLog.e(e);
            if (intent != null)
            {
                final int failCount = intent.getIntExtra("failCount", 0);
                if (failCount < 60)
                {
                    AndroidUtilities.runOnUIThread(() -> {
                        try
                        {
                            Intent intent1 = new Intent(ApplicationLoader.applicationContext, GcmRegistrationIntentService.class);
                            intent1.putExtra("failCount", failCount + 1);
                            startService(intent1);
                        }
                        catch (Exception e1)
                        {
                            FileLog.e(e1);
                        }
                    }, failCount < 20 ? 10000 : 60000 * 30);
                }
            }
        }
    }

    private void sendRegistrationToServer(final String token)
    {
        Utilities.stageQueue.postRunnable(() -> {
            UserConfig.pushString = token;
            UserConfig.registeredForPush = false;
            UserConfig.saveConfig(false);
            if (UserConfig.getClientUserId() != 0)
            {
                AndroidUtilities.runOnUIThread(() -> MessagesController.getInstance().registerForPush(token));
            }
        });
    }
}