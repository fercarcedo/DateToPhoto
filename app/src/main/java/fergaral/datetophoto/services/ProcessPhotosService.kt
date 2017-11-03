package fergaral.datetophoto.services

import android.app.IntentService
import android.content.Intent
import android.os.ResultReceiver

import java.util.ArrayList

import fergaral.datetophoto.R
import fergaral.datetophoto.utils.NotificationUtils
import fergaral.datetophoto.utils.ProcessPhotos

class ProcessPhotosService : IntentService(ProcessPhotosService::class.java!!.getSimpleName()) {

    override fun onHandleIntent(intent: Intent?) {
        try {
            startForeground(NotificationUtils.NOTIFICATION_ID,
                    NotificationUtils(this).showProgressNotification("Procesando fotos..."))
            isRunning = true
            val receiver = intent!!.getParcelableExtra<ResultReceiver>("receiver")
            val onBackground = intent.getBooleanExtra("onBackground", true)
            val cameraImages = intent.getStringArrayListExtra("cameraimages")
            ProcessPhotos().execute(receiver, onBackground, cameraImages, this)
        } finally {
            stopForeground(true)
            val notificationText = getString(R.string.process_has_finished)
            NotificationUtils(this).setUpNotification(2, false, false, notificationText, notificationText)
            isRunning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    companion object {

        var isRunning: Boolean = false
            private set
    }
}
