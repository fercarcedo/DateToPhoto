package fergaral.datetophoto.receivers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;

import fergaral.datetophoto.services.ProcessPhotosService;

/**
 * Created by Parejúa on 24/07/2014.
 */
public class PowerConnectionReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
       //int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
      // boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

      /* NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Date To Photo")
            .setContentText("Procesando fotos...")
            .setProgress(100, 0, false);

       NotificationManager notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
       notifManager.notify(NOTIFICATION_ID, notifBuilder.build());*/

        /*SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String active = sharedPrefs.getString(context.getString(R.string.pref_active_key), context.getString(R.string.pref_active_default));

        if(active == "yes") {
            context.startService(new Intent(context, ProcessPhotosService.class));
        }*/


        /*NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Date To Photo")
                .setContentText("Encendiendo...")
                .setTicker("Date To Photo - Notificación");

        NotificationManager notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build());*/

        if(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
            Intent serviceIntent = new Intent(context, ProcessPhotosService.class);
            startWakefulService(context, serviceIntent);
        }else if(intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
            //Cancelamos el servicio, si se lanzó al poner el dispositivo a cargar
            Intent cancelIntent = new Intent(ProcessPhotosService.ACTION_CANCEL_CHARGER_DISCONNECTED);
            cancelIntent.putExtra(ProcessPhotosService.CANCEL_SERVICE, true);

            LocalBroadcastManager.getInstance(context).sendBroadcast(cancelIntent);
        }

        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
        {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.getApplicationContext().registerReceiver(null, ifilter);

            if(batteryStatus != null) {
                // Are we charging / charged?
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
                //int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                //int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                //float batteryPct = (level * 100) / (float)scale;

                if (isCharging) {
                    Intent serviceIntent = new Intent(context, ProcessPhotosService.class);
                    startWakefulService(context, serviceIntent);
                }
            }
        }
    }

}
