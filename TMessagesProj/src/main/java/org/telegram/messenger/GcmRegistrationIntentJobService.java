package org.telegram.messenger;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class GcmRegistrationIntentJobService extends JobService
{
    public static final String TAG = GcmRegistrationIntentJobService.class.getName();


    @Override
    public boolean onStartJob(JobParameters params)
    {
        try
        {
            InstanceID instanceID = InstanceID.getInstance(this);
            final String token = instanceID.getToken(BuildVars.GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            AndroidUtilities.runOnUIThread(() ->
            {
                ApplicationLoader.postInitApplication();
                sendRegistrationToServer(token);
            });
        }
        catch (Exception e)
        {
            FileLog.e(e);
            if (params != null)
            {
                final int failCount = params.getExtras().getInt("failCount", 0);
                if (failCount < 60)
                {
                    AndroidUtilities.runOnUIThread(() ->
                    {
                        try
                        {
                            PersistableBundle extras = new PersistableBundle();
                            extras.putInt("failCount", failCount + 1);
                            ComponentName serviceComponent = new ComponentName(getApplicationContext(), GcmRegistrationIntentJobService.class);
                            JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
                            builder.setMinimumLatency(1000);
                            builder.setOverrideDeadline(2000);
                            builder.setRequiresCharging(false);
                            builder.setExtras(extras);
                            JobScheduler jobScheduler = getApplicationContext().getSystemService(JobScheduler.class);
                            if (jobScheduler != null)
                                jobScheduler.schedule(builder.build());
                        }
                        catch (Exception ignored)
                        {
                        }
                    }, failCount < 20 ? 10000 : 60000 * 30);
                }
            }
        }

        return true;
    }

    private void sendRegistrationToServer(final String token)
    {
        Utilities.stageQueue.postRunnable(() ->
        {
            UserConfig.pushString = token;
            UserConfig.registeredForPush = false;
            UserConfig.saveConfig(false);
            if (UserConfig.getClientUserId() != 0)
                AndroidUtilities.runOnUIThread(() -> MessagesController.getInstance().registerForPush(token));
        });
    }

    @Override
    public boolean onStopJob(JobParameters params)
    {
        return true;
    }
}
