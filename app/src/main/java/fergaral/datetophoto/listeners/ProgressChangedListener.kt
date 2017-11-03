package fergaral.datetophoto.listeners

import android.os.Parcelable

import java.io.Serializable

/**
 * Created by Parejúa on 23/03/2015.
 */
interface ProgressChangedListener : Serializable {
    fun reportTotal(total: Int)
    fun onProgressChanged(progress: Int)
    fun reportEnd(fromActionShare: Boolean)
}
