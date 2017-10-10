package fergaral.datetophoto.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import fergaral.datetophoto.utils.ProcessPhotos;

public class ProcessPhotosService extends IntentService {

    private static boolean mRunning;

    public ProcessPhotosService() {
        super(ProcessPhotosService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        mRunning = true;
        ResultReceiver receiver = intent.getParcelableExtra("receiver");
        boolean onBackground = intent.getBooleanExtra("onBackground", true);
        ArrayList<String> cameraImages = intent.getStringArrayListExtra("cameraimages");
        new ProcessPhotos().execute(receiver, onBackground, cameraImages, this);
        mRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunning = false;
    }

    public static boolean isRunning() {
        return mRunning;
    }
}
