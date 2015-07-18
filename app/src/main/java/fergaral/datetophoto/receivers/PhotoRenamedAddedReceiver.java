package fergaral.datetophoto.receivers;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import fergaral.datetophoto.services.DetectAlreadyProcessedPhotosService;

/**
 * Created by fer on 23/06/15.
 */
public class PhotoRenamedAddedReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        startWakefulService(context, new Intent(context, DetectAlreadyProcessedPhotosService.class));
    }
}
