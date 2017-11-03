package fergaral.datetophoto.tasks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface

import java.io.File
import java.io.IOException

import fergaral.datetophoto.algorithms.PhotoProcessedAlgorithm
import fergaral.datetophoto.utils.NotificationUtils
import fergaral.datetophoto.utils.PhotoUtils
import fergaral.datetophoto.utils.Utils

/**
 * Created by Fer on 18/10/2017.
 */

class DatestampDetectionTask(private val context: Context, private val algorithm: PhotoProcessedAlgorithm) {

    fun execute(): Float {
        val notificationUtils = NotificationUtils(context)
        notificationUtils.showProgressNotification("Ejecutando...")
        val imagesToProcess = Utils.getImagesToProcess(context, PhotoUtils(context).cameraImages)
        var numCorrect = 0
        var totalImages = 0

        for (i in imagesToProcess.indices) {
            val path = imagesToProcess[i]
            val result = algorithm.isProcessed(path)

            if (result != PhotoProcessedAlgorithm.Result.ERROR)
                totalImages++

            if (result == PhotoProcessedAlgorithm.Result.PROCESSED) {
                if (exifFieldFound(path)) {
                    numCorrect++
                }
            } else if (result == PhotoProcessedAlgorithm.Result.NOT_PROCESSED) {
                if (!exifFieldFound(path)) {
                    numCorrect++
                }
            }

            notificationUtils.setNotificationProgress(imagesToProcess.size, i + 1)
        }
        return numCorrect / totalImages.toFloat() * 100
    }

    private fun exifFieldFound(imagePath: String): Boolean {
        try {
            val exifInterface = ExifInterface(imagePath)
            val makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE)
            return makeExif != null && makeExif.startsWith("dtp-")
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

    }
}
