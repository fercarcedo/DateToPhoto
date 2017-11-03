package fergaral.datetophoto.jobs

import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest

import java.util.concurrent.TimeUnit

import fergaral.datetophoto.utils.ProcessPhotos

/**
 * Created by Fer on 07/10/2017.
 */

class ProcessPhotosJob : Job() {

    override fun onRunJob(params: Job.Params): Job.Result {
        ProcessPhotos().execute(context)
        scheduleForTomorrow()
        return Job.Result.SUCCESS
    }

    companion object {

        val TAG = "process_photos_job"

        fun scheduleNow() {
            schedule(1)
        }

        fun scheduleForTomorrow() {
            schedule(TimeUnit.DAYS.toMillis(1))
        }

        private fun schedule(delayMillis: Long) {
            if (!JobManager.instance().getAllJobRequestsForTag(TAG).isEmpty()) {
                // Job already scheduled
                return
            }

            JobRequest.Builder(ProcessPhotosJob.TAG)
                    .setExecutionWindow(delayMillis, delayMillis + TimeUnit.DAYS.toMillis(1))
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }
    }
}
