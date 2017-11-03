package fergaral.datetophoto.algorithms

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.SparseArray

import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer

import java.io.File

import fergaral.datetophoto.DateToPhoto
import fergaral.datetophoto.utils.Point

/**
 * Created by Fer on 18/10/2017.
 */

class VisionAPIPhotoProcessedAlgorithm : PhotoProcessedAlgorithm {
    override fun isProcessed(photoPath: String): PhotoProcessedAlgorithm.Result {
        val context = DateToPhoto.instance
        val options = BitmapFactory.Options()
        options.inSampleSize = 2
        val imageBitmap = BitmapFactory.decodeFile(File(photoPath).absolutePath, options) ?: return PhotoProcessedAlgorithm.Result.ERROR
        val textRecognizer = TextRecognizer.Builder(context).build()
        var datestamped = false

        if (textRecognizer.isOperational) {
            val frame = Frame.Builder()
                    .setBitmap(imageBitmap)
                    .build()

            val textBlocks = textRecognizer.detect(frame)

            for (i in 0 until textBlocks.size()) {
                val boundingBox = textBlocks.valueAt(i).boundingBox
                val bottomRightPoint = Point(boundingBox.right, boundingBox.bottom)
                if (bottomRightPoint.isInBottomRightCorner(imageBitmap.width, imageBitmap.height)) {
                    datestamped = true
                    break
                }
            }
        }

        return if (datestamped) PhotoProcessedAlgorithm.Result.PROCESSED else PhotoProcessedAlgorithm.Result.NOT_PROCESSED
    }
}
