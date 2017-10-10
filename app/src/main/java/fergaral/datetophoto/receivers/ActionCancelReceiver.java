package fergaral.datetophoto.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.PhotosActivity;
import fergaral.datetophoto.utils.NotificationUtils;

/**
 * Created by fer on 10/06/15.
 */
public class ActionCancelReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        String cancelText = context.getString(R.string.cancelling);
        new NotificationUtils(context).setUpNotification(false, false, cancelText, cancelText);
        Intent broadcastIntent = new Intent(PhotosActivity.INTENT_ACTION);
        broadcastIntent.putExtra("dialogcancelled", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }
}
