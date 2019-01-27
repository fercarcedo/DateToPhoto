package fergaral.datetophoto.utils

/**
 * Created by fer on 21/09/15.
 */
interface ProgressListener {
    fun reportTotal(total: Int)
    fun onProgressChanged(progress: Int, actual: Int)
    fun reportEnd(fromActionShare: Boolean, searchPhotos: Boolean = false)
}
