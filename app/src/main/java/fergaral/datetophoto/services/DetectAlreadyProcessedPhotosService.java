package fergaral.datetophoto.services;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.ExifInterface;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.PhotosActivity;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.receivers.PhotoRenamedAddedReceiver;
import fergaral.datetophoto.utils.NotificationUtils;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 24/06/15.
 */
public class DetectAlreadyProcessedPhotosService extends IntentService {

    public DetectAlreadyProcessedPhotosService() {
        super("DetectAlreadyProcessedPhotosService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //We don't have permission
            new NotificationUtils(this).showPermissionNotification();
            return;
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Date To Photo")
                .setSmallIcon(R.drawable.ic_dtp_transp)
                .setContentIntent(PendingIntent.getActivity(
                        this,
                        0,
                        new Intent(this, PhotosActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentText("A")
                .setTicker("Usb desconectado");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.notify(1, notificationBuilder.build());

        SQLiteDatabase photosDb = new DatabaseHelper(this).getWritableDatabase();

        List<String> imagesToProcess = Utils.getPhotosWithoutDate(this,
                Utils.getImagesToProcess(this, new PhotoUtils(this).getCameraImages()), photosDb);

        for(String path : imagesToProcess) {
            //Primero miramos a ver si el archivo está en la base de datos
            Cursor cursor = photosDb.rawQuery("SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " + DatabaseHelper.TABLE_NAME
                    + " WHERE " + DatabaseHelper.PATH_COLUMN + "=?", new String[] {path});
            if(!cursor.moveToFirst()) {
                //El archivo no estaba en la base de datos
                try {
                    ExifInterface exifInterface = new ExifInterface(path);
                    String makeTag = exifInterface.getAttribute(ExifInterface.TAG_MAKE);

                    if(makeTag != null && makeTag.startsWith("dtp-")) {
                        //Añadimos la ruta a la base de datos
                        ContentValues values = new ContentValues();
                        values.put(DatabaseHelper.PATH_COLUMN, path);
                        photosDb.insert(DatabaseHelper.TABLE_NAME, null, values);
                    }else{
                        //Miramos a ver si empieza por dtp-
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            cursor.close();
        }

        photosDb.close();

        PhotoRenamedAddedReceiver.completeWakefulIntent(intent);
    }
}
