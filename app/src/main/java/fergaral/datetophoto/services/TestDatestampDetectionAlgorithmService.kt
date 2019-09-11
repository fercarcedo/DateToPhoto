package fergaral.datetophoto.services

import android.app.IntentService
import android.content.Intent
import android.os.Environment

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

import fergaral.datetophoto.algorithms.PhotoProcessedAlgorithm
import fergaral.datetophoto.algorithms.VisionAPIPhotoProcessedAlgorithm
import fergaral.datetophoto.tasks.DatestampDetectionTask
import fergaral.datetophoto.utils.NotificationUtils

/**
 * Created by Fer on 18/10/2017.
 */

private const val ALGORITHM_KEY = "algorithm"

class TestDatestampDetectionAlgorithmService : IntentService(TestDatestampDetectionAlgorithmService::class.java.simpleName) {

    override fun onHandleIntent(intent: Intent?) {
        var accuracy = 0f
        try {
            startForeground(NotificationUtils.NOTIFICATION_ID,
                    NotificationUtils(this).showProgressNotification("Probando algoritmo..."))
            accuracy = DatestampDetectionTask(this, intent!!.getSerializableExtra(ALGORITHM_KEY) as PhotoProcessedAlgorithm).execute()
        } finally {
            stopForeground(true)
            NotificationUtils(this).setUpNotification(2, false, false, accuracy.toString(), accuracy.toString())
        }
    }

    companion object {
        val ALGORITHM_KEY = "algorithm_key"
    }
}
