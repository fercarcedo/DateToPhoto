package fergaral.datetophoto.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import fergaral.datetophoto.receivers.PhotosObserver;

/**
 * Created by fer on 10/06/16.
 */
public class CheckPhotosService extends Service {
    private PhotosObserver mObserver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mObserver = new PhotosObserver(this, new Handler(Looper.getMainLooper()));

        getContentResolver()
                .registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    false,
                                    mObserver);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getContentResolver()
                .unregisterContentObserver(mObserver);
    }
}
