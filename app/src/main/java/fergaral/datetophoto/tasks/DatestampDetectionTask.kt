package fergaral.datetophoto.tasks

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import fergaral.datetophoto.algorithms.PhotoProcessedAlgorithm
import fergaral.datetophoto.utils.NotificationUtils
import fergaral.datetophoto.utils.Utils
import java.io.IOException

/**
 * Created by Fer on 18/10/2017.
 */

class DatestampDetectionTask(private val context: Context, private val algorithm: PhotoProcessedAlgorithm) {

    fun execute(): Float {
        val notificationUtils = NotificationUtils(context)
        notificationUtils.showProgressNotification("Ejecutando...")
        val imagesToProcess = Utils.getImagesToProcess(context)
        var numCorrect = 0
        var totalImages = 0

        for (i in imagesToProcess.indices) {
            val image = imagesToProcess[i]
            val result = algorithm.isProcessed(context, image.uri)

            if (result != PhotoProcessedAlgorithm.Result.ERROR)
                totalImages++

            if (result == PhotoProcessedAlgorithm.Result.PROCESSED) {
                if (exifFieldFound(image.uri)) {
                    numCorrect++
                }
            } else if (result == PhotoProcessedAlgorithm.Result.NOT_PROCESSED) {
                if (!exifFieldFound(image.uri)) {
                    numCorrect++
                }
            }

            notificationUtils.setNotificationProgress(imagesToProcess.size, i + 1)
        }
        return numCorrect / totalImages.toFloat() * 100
    }

    private fun exifFieldFound(uri: Uri): Boolean {
        try {
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE)
                return@exifFieldFound makeExif != null && makeExif.startsWith("dtp-")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }
}
