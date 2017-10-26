package fergaral.datetophoto.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import fergaral.datetophoto.algorithms.PhotoProcessedAlgorithm;
import fergaral.datetophoto.algorithms.VisionAPIPhotoProcessedAlgorithm;
import fergaral.datetophoto.tasks.DatestampDetectionTask;
import fergaral.datetophoto.utils.NotificationUtils;

/**
 * Created by Fer on 18/10/2017.
 */

public class TestDatestampDetectionAlgorithmService extends IntentService {
    public static final String ALGORITHM_KEY = "algorithm";

    public TestDatestampDetectionAlgorithmService() {
        super(TestDatestampDetectionAlgorithmService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        float accuracy = 0;
        try {
            startForeground(NotificationUtils.NOTIFICATION_ID,
                    new NotificationUtils(this).showProgressNotification("Probando algoritmo..."));
            accuracy = new DatestampDetectionTask(this, (PhotoProcessedAlgorithm) intent.getSerializableExtra(ALGORITHM_KEY)).execute();
        } catch (Exception e) {
            writeExceptionLog(e);
        } finally {
            stopForeground(true);
            String notificationText = "Algoritmo finalizado";
            new NotificationUtils(this).setUpNotification(2, false, false, String.valueOf(accuracy), String.valueOf(accuracy));
        }
    }

    private void writeExceptionLog(Exception e) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(
                    Environment.getExternalStorageDirectory().getPath() + File.separator + "Download" + File.separator + "dtperror.txt"
            ));

            e.printStackTrace(writer);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
}
