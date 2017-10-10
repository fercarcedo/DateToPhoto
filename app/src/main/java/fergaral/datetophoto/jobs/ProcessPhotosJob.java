package fergaral.datetophoto.jobs;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

import java.util.concurrent.TimeUnit;

import fergaral.datetophoto.utils.ProcessPhotos;

/**
 * Created by Fer on 07/10/2017.
 */

public class ProcessPhotosJob extends Job {

    public static final String TAG = "process_photos_job";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        new ProcessPhotos().execute(getContext());
        scheduleForTomorrow();
        return Result.SUCCESS;
    }

    public static void scheduleNow() {
        schedule(1);
    }

    public static void scheduleForTomorrow() {
        schedule(TimeUnit.DAYS.toMillis(1));
    }

    private static void schedule(long delayMillis) {
        if (!JobManager.instance().getAllJobRequestsForTag(TAG).isEmpty()) {
            // Job already scheduled
            return;
        }

        new JobRequest.Builder(ProcessPhotosJob.TAG)
                .setExecutionWindow(delayMillis, delayMillis + TimeUnit.DAYS.toMillis(1))
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }
}
