package fergaral.datetophoto.services

import android.Manifest
import android.app.IntentService
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.media.ExifInterface
import androidx.core.content.ContextCompat

import java.io.IOException

import fergaral.datetophoto.db.DatabaseHelper
import fergaral.datetophoto.utils.NotificationUtils
import fergaral.datetophoto.utils.PhotoUtils
import fergaral.datetophoto.utils.Utils

/**
 * Created by fer on 24/06/15.
 */
class DetectAlreadyProcessedPhotosService : IntentService("DetectAlreadyProcessedPhotosService") {

    override fun onHandleIntent(intent: Intent?) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //We don't have permission
            NotificationUtils(this).showPermissionNotification()
            return
        }

        val photosDb = DatabaseHelper(this).writableDatabase

        val imagesToProcess = Utils.getPhotosWithoutDate(this,
                Utils.getImagesToProcess(this, PhotoUtils(this).cameraImages), photosDb)

        for (path in imagesToProcess) {
            //Primero miramos a ver si el archivo está en la base de datos
            val cursor = photosDb.rawQuery("SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " + DatabaseHelper.TABLE_NAME
                    + " WHERE " + DatabaseHelper.PATH_COLUMN + "=?", arrayOf(path))
            if (!cursor.moveToFirst()) {
                //El archivo no estaba en la base de datos
                try {
                    val exifInterface = ExifInterface(path)
                    val makeTag = exifInterface.getAttribute(ExifInterface.TAG_MAKE)

                    if (makeTag != null && makeTag.startsWith("dtp-")) {
                        //Añadimos la ruta a la base de datos
                        val values = ContentValues()
                        values.put(DatabaseHelper.PATH_COLUMN, path)
                        photosDb.insert(DatabaseHelper.TABLE_NAME, null, values)
                    } else {
                        //Miramos a ver si empieza por dtp-
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }

            cursor.close()
        }

        photosDb.close()
    }
}
