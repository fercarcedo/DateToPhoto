package fergaral.datetophoto

import android.app.Application

import fergaral.datetophoto.works.ProcessPhotosWorker

/**
 * Created by Fer on 07/10/2017.
 */

class DateToPhoto : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        ProcessPhotosWorker.schedule()
    }

    companion object {
        lateinit var instance: DateToPhoto
            private set
    }
}
