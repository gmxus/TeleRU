package org.telegram.messenger;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

public class GcmInstanceIDListenerService extends InstanceIDListenerService
{

    @Override
    public void onTokenRefresh()
    {
        AndroidUtilities.runOnUIThread(() -> {
            ApplicationLoader.postInitApplication();
            Intent intent = new Intent(ApplicationLoader.applicationContext, GcmRegistrationIntentService.class);
            startService(intent);
        });
    }
}
