package fergaral.datetophoto.works

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

import java.util.concurrent.TimeUnit

import fergaral.datetophoto.utils.ProcessPhotos

/**
 * Created by Fer on 07/10/2017.
 */

class ProcessPhotosWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        ProcessPhotos().execute(applicationContext)
        return Result.success()
    }

    companion object {

        private const val TAG = "process_photos_job"

        fun schedule() {
            val constraintsBuilder = Constraints.Builder()
                .setRequiresCharging(true)
            val constraints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                constraintsBuilder.setRequiresDeviceIdle(true)
                    .build()
            } else {
                constraintsBuilder.build()
            }
            val workRequest = PeriodicWorkRequestBuilder<ProcessPhotosWorker>(1, TimeUnit.DAYS)
                .addTag(TAG)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance().enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, workRequest)
        }
    }
}
