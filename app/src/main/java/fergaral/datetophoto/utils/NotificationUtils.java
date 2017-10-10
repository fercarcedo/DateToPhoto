package fergaral.datetophoto.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.PhotosActivity;
import fergaral.datetophoto.activities.StoragePermissionDeniedFloatingActivity;
import fergaral.datetophoto.receivers.ActionCancelReceiver;

/**
 * Created by fer on 4/07/15.
 */
public class NotificationUtils {

    private static final int NOTIFICATION_ID = 1;

    private NotificationCompat.Builder mNotifBuilder;
    private NotificationManager mNotifManager;
    private long mNotifStartTime;
    private Context mContext;

    public NotificationUtils(Context context) {
        mContext = context;
    }

    private void setUpNotification(boolean onGoing, boolean hasActions, String contentText, String tickerText) {
        Intent resultIntent = new Intent(mContext, PhotosActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mNotifBuilder = new NotificationCompat.Builder(mContext);

        boolean lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        mNotifBuilder.setSmallIcon(R.drawable.ic_dtp_transp)
                .setContentTitle("Date To Photo")
                .setContentText(contentText)
                .setContentIntent(resultPendingIntent)
                .setTicker(tickerText)
                .setOngoing(onGoing)
                .setAutoCancel(true);

        if(lollipop) {
            mNotifBuilder.setColor(Color.parseColor("#1976D2"));
        }

        if(hasActions) {
            Intent cancelIntent = new Intent(mContext, ActionCancelReceiver.class);

            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(
                    mContext,
                    0,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            mNotifBuilder.addAction(R.drawable.ic_action_ic_clear_24px, "Cancelar", cancelPendingIntent);
        }

        mNotifManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifManager.notify(NOTIFICATION_ID, mNotifBuilder.build());
    }

    public void showProgressNotification(String startText) {
        setUpNotification(true, true, startText, "Procesando fotos...");
    }

    public void showSearchingPhotosNotification(String text) {
        setUpNotification(true, false, text, text);
    }

    public void setNotificationProgress(int total, int actual) {
        if(mNotifBuilder != null && mNotifManager != null) {
            long time = System.currentTimeMillis();

            if(time - mNotifStartTime >= 500) {
                mNotifBuilder.setProgress(total, actual, false);
                mNotifManager.notify(NOTIFICATION_ID, mNotifBuilder.build());
                mNotifStartTime = time;
            }
        }
    }

    public void endProgressNotification(String endText) {
        if(mNotifBuilder != null && mNotifManager != null) {
            setUpNotification(false, false, endText, "El proceso ha finalizado");
        }
    }

    public void showStandAloneNotification(String text) {
        boolean lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        Intent resultIntent = new Intent(mContext, PhotosActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(mContext);
        notifBuilder.setContentTitle("Date To Photo")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_dtp_transp)
                .setTicker(text)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        NotificationManager notifManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build());
    }

    public void showPermissionNotification() {
        boolean lollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        Intent resultIntent = new Intent(mContext, StoragePermissionDeniedFloatingActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                mContext,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(mContext);
        notifBuilder.setContentTitle("Date To Photo")
                .setContentText("Permiso necesario")
                .setSmallIcon(R.drawable.ic_dtp_transp)
                .setTicker("Permiso necesario")
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        NotificationManager notifManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build());
    }
}
