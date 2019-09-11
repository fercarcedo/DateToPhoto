package fergaral.datetophoto.algorithms

import android.content.Context
import android.net.Uri
import java.io.Serializable

/**
 * Created by Fer on 18/10/2017.
 */

interface PhotoProcessedAlgorithm : Serializable {
    enum class Result {
        PROCESSED, NOT_PROCESSED, ERROR
    }

    fun isProcessed(context: Context, photoUri: Uri): Result
}
