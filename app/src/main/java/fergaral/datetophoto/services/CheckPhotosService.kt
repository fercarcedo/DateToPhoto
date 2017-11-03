package fergaral.datetophoto.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore

import fergaral.datetophoto.receivers.PhotosObserver

/**
 * Created by fer on 10/06/16.
 */
class CheckPhotosService : Service() {
    private var mObserver: PhotosObserver? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mObserver = PhotosObserver(this, Handler(Looper.getMainLooper()))

        contentResolver
                .registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        false,
                        mObserver!!)

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        contentResolver
                .unregisterContentObserver(mObserver!!)
    }
}
