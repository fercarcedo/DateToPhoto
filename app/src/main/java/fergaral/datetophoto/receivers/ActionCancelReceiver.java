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
import fergaral.datetophoto.activities.MyActivity;

/**
 * Created by fer on 10/06/15.
 */
public class ActionCancelReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        showNotification(context, "Cancelando...", "Cancelando...");
        Intent broadcastIntent = new Intent(MyActivity.INTENT_ACTION);
        broadcastIntent.putExtra("dialogcancelled", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
    }

    private void showNotification(Context context, String text, String tickerText) {

        boolean lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("Date To Photo")
                .setContentText(text)
                .setSmallIcon(lollipop ? R.drawable.ic_dtp_transp : R.drawable.ic_launcher)
                .setTicker(tickerText);

        if(lollipop) {
            notificationBuilder.setColor(Color.parseColor("#1976D2"));
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}
