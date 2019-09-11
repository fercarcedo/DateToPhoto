package fergaral.datetophoto.algorithms

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import fergaral.datetophoto.utils.Point
import java.io.File
import java.util.concurrent.ExecutionException

/**
 * Created by Fer on 18/10/2017.
 */

class VisionAPIPhotoProcessedAlgorithm : PhotoProcessedAlgorithm {
    private val detector by lazy {
        FirebaseVision.getInstance()
            .onDeviceTextRecognizer
    }

    override fun isProcessed(context: Context, uri: Uri): PhotoProcessedAlgorithm.Result {
        val options = BitmapFactory.Options()
        options.inSampleSize = 2
        context.contentResolver.openInputStream(uri)?.let { inputStream ->
            val imageBitmap = BitmapFactory.decodeStream(inputStream, null, options) ?: return PhotoProcessedAlgorithm.Result.ERROR
            val firebaseImage = FirebaseVisionImage.fromBitmap(imageBitmap)
            var datestamped = false

            try {
                val recognizedText = Tasks.await(detector.processImage(firebaseImage))
                for (i in 0 until recognizedText.textBlocks.size) {
                    val boundingBox = recognizedText.textBlocks[i].boundingBox
                    if (boundingBox != null) {
                        val bottomRightPoint = Point(boundingBox.right, boundingBox.bottom)
                        if (bottomRightPoint.isInBottomRightCorner(imageBitmap.width, imageBitmap.height)) {
                            datestamped = true
                            break
                        }
                    }
                }
            } catch (e: ExecutionException) {
            } catch (e: InterruptedException) {
            }

            return if (datestamped) PhotoProcessedAlgorithm.Result.PROCESSED else PhotoProcessedAlgorithm.Result.NOT_PROCESSED
        }
        return PhotoProcessedAlgorithm.Result.NOT_PROCESSED
    }
}
