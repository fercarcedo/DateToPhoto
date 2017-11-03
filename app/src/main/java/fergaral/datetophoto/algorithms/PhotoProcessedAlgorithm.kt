package fergaral.datetophoto.algorithms

import java.io.Serializable

/**
 * Created by Fer on 18/10/2017.
 */

interface PhotoProcessedAlgorithm : Serializable {
    enum class Result {
        PROCESSED, NOT_PROCESSED, ERROR
    }

    fun isProcessed(photoPath: String): Result
}
