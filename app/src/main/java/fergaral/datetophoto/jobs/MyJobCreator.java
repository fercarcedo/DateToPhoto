package fergaral.datetophoto.jobs;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by Fer on 07/10/2017.
 */

public class MyJobCreator implements JobCreator {
    @Nullable
    @Override
    public Job create(@NonNull String tag) {
        switch (tag) {
            case ProcessPhotosJob.TAG:
                return new ProcessPhotosJob();
            default:
                return null;
        }
    }
}
