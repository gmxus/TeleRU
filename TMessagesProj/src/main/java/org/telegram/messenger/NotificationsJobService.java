package org.telegram.messenger;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NotificationsJobService extends JobService
{
    public static final String TAG = NotificationsJobService.class.getName();


    @Override
    public boolean onStartJob(JobParameters params)
    {
        ApplicationLoader.postInitApplication();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params)
    {
        return true;
    }
}
