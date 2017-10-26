package fergaral.datetophoto;

import android.app.Application;
import android.widget.Toast;

import com.evernote.android.job.JobManager;

import fergaral.datetophoto.jobs.MyJobCreator;
import fergaral.datetophoto.jobs.ProcessPhotosJob;

/**
 * Created by Fer on 07/10/2017.
 */

public class DateToPhoto extends Application {

    private static DateToPhoto instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        JobManager.create(this).addJobCreator(new MyJobCreator());
        ProcessPhotosJob.scheduleNow();
    }

    public static DateToPhoto getInstance() {
        return instance;
    }
}
