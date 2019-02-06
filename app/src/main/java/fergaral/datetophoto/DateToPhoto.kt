package fergaral.datetophoto

import android.app.Application
import android.widget.Toast

import com.evernote.android.job.JobManager

import fergaral.datetophoto.jobs.MyJobCreator
import fergaral.datetophoto.jobs.ProcessPhotosJob

/**
 * Created by Fer on 07/10/2017.
 */

class DateToPhoto : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        JobManager.create(this).addJobCreator(MyJobCreator())
        ProcessPhotosJob.scheduleNow()
    }

    companion object {
        lateinit var instance: DateToPhoto
            private set
    }
}
