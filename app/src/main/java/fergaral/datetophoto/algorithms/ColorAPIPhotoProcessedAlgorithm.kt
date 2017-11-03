package fergaral.datetophoto.algorithms

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

import fergaral.datetophoto.utils.Utils

/**
 * Created by Fer on 18/10/2017.
 */

class ColorAPIPhotoProcessedAlgorithm : PhotoProcessedAlgorithm {
    override fun isProcessed(photoPath: String): PhotoProcessedAlgorithm.Result {
        val myBitmap = BitmapFactory.decodeFile(File(photoPath).absolutePath) ?: return PhotoProcessedAlgorithm.Result.ERROR

        val upperLeft = myBitmap.getPixel(0, 0)
        val upperRight = myBitmap.getPixel(myBitmap.width - 1, 0)
        val lowerLeft = myBitmap.getPixel(0, myBitmap.height - 1)
        val lowerRight = myBitmap.getPixel(myBitmap.width - 1, myBitmap.height - 1)

        return if (Utils.getColor(upperLeft) == Color.BLUE && Utils.getColor(upperRight) == Color.RED
                && Utils.getColor(lowerLeft) == Color.GREEN && Utils.getColor(lowerRight) == Color.YELLOW)
            PhotoProcessedAlgorithm.Result.PROCESSED
        else
            PhotoProcessedAlgorithm.Result.NOT_PROCESSED
    }
}
