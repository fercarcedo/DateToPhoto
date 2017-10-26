package fergaral.datetophoto.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;

import java.io.File;
import java.io.IOException;
import java.util.List;

import fergaral.datetophoto.algorithms.PhotoProcessedAlgorithm;
import fergaral.datetophoto.utils.NotificationUtils;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by Fer on 18/10/2017.
 */

public class DatestampDetectionTask {
    private Context context;
    private PhotoProcessedAlgorithm algorithm;

    public DatestampDetectionTask(Context context, PhotoProcessedAlgorithm algorithm) {
        this.context = context;
        this.algorithm = algorithm;
    }

    public float execute() {
        NotificationUtils notificationUtils = new NotificationUtils(context);
        notificationUtils.showProgressNotification("Ejecutando...");
        List<String> imagesToProcess = Utils.getImagesToProcess(context, new PhotoUtils(context).getCameraImages());
        int numCorrect = 0;
        int totalImages = 0;

        for (int i = 0; i < imagesToProcess.size(); i++) {
            String path = imagesToProcess.get(i);
            PhotoProcessedAlgorithm.Result result = algorithm.isProcessed(path);

            if (result != PhotoProcessedAlgorithm.Result.ERROR)
                totalImages++;

            if (result == PhotoProcessedAlgorithm.Result.PROCESSED) {
                if (exifFieldFound(path)) {
                    numCorrect++;
                }
            } else if (result == PhotoProcessedAlgorithm.Result.NOT_PROCESSED) {
                if (!exifFieldFound(path)) {
                    numCorrect++;
                }
            }

            notificationUtils.setNotificationProgress(imagesToProcess.size(), i + 1);
        }
        return (numCorrect / ((float)totalImages)) * 100;
    }

    private boolean exifFieldFound(String imagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            String makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            return makeExif != null && makeExif.startsWith("dtp-");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
