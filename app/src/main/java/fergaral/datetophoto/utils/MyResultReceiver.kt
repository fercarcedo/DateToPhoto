package fergaral.datetophoto.utils

import android.os.Bundle
import android.os.Handler
import android.os.Parcel
import android.os.Parcelable
import android.os.PowerManager
import android.os.ResultReceiver

import fergaral.datetophoto.listeners.ProgressChangedListener

/**
 * Created by Parejúa on 23/03/2015.
 */
class MyResultReceiver
/**
 * Create a new ResultReceive to receive results.  Your
 * [.onReceiveResult] method will be called from the thread running
 * <var>handler</var> if given, or from an arbitrary thread if null.
 *
 * @param handler
 */
(handler: Handler) : ResultReceiver(handler) {

    private var receiver: ProgressChangedListener? = null
    var wakeLock: PowerManager.WakeLock? = null
        set(wakeLock) {
            if (wakeLock != null)
                field = wakeLock

        }

    fun setReceiver(receiver: ProgressChangedListener) {
        this.receiver = receiver
    }

    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
        if (resultData.containsKey("releaseActivity"))
            receiver = null //Release activity so that it can be garbage collected

        if (resultData.containsKey("total")) {
            if (this.wakeLock != null)
                this.wakeLock!!.acquire()
            if (receiver != null)
                receiver!!.reportTotal(resultData.getInt("total"))
        }
        if (resultData.containsKey("progress")) {
            if (receiver != null)
                receiver!!.onProgressChanged(resultData.getInt("progress"))
        }
        if (resultData.containsKey("end")) {
            if (this.wakeLock != null && this.wakeLock!!.isHeld)
                this.wakeLock!!.release()
            if (receiver != null)
                receiver!!.reportEnd(false)
        }
        if (resultData.containsKey("endShared")) {
            if (this.wakeLock != null && this.wakeLock!!.isHeld)
                this.wakeLock!!.release()
            if (receiver != null)
                receiver!!.reportEnd(true)
        }
    }
}
