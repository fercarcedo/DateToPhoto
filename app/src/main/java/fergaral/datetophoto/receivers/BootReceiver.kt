package fergaral.datetophoto.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import fergaral.datetophoto.jobs.ProcessPhotosJob

/**
 * Created by Parejúa on 24/07/2014.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ProcessPhotosJob.scheduleNow()
    }
}
