package fergaral.datetophoto.jobs

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

/**
 * Created by Fer on 07/10/2017.
 */

class MyJobCreator : JobCreator {
    override fun create(tag: String): Job? {
        when (tag) {
            ProcessPhotosJob.TAG -> return ProcessPhotosJob()
            else -> return null
        }
    }
}
