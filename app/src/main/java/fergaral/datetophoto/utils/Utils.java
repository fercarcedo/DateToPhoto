package fergaral.datetophoto.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.twmacinta.util.MD5;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Checksum;

import fergaral.datetophoto.R;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.services.ProcessPhotosService;
import fergaral.datetophoto.services.ProcessPhotosURIService;

/**
 * Created by Parejúa on 30/03/2015.
 */
public class Utils {

    public static void write(String path, String text) {

        try {

            File file = new File(path);
            FileWriter filewriter = new FileWriter(file);
            BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
            PrintWriter printwriter = new PrintWriter(bufferedwriter);
            printwriter.write(text);

            bufferedwriter.close();
            printwriter.close();
        }catch(Exception e) {


        }
    }

    public static String[] getFoldersToProcess(Context context)
    {
        if(context == null)
            return new String[] {""};

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_folderstoprocess_key), "").split(FoldersListPreference.SEPARATOR);
    }

    public static boolean processSelectedFolder(Context context, String folderName)
    {
        if(context == null || folderName == null)
            return false;

        String[] foldersToProcess = getFoldersToProcess(context);

        for(int i=0; i<foldersToProcess.length; i++)
        {
            if(folderName.equals(foldersToProcess[i]))
                return true;
        }

        return false;
    }

    public static boolean overwritePhotos(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_overwrite_key), false);
    }


    /**
     * Method that returns the disk space usage by the photos with date and by the photos without date
     *
     * @return int[] array with the number of MB used by the photos with date and the number of MB used by the photos without date
     */
    public static int[] getDiskSpaceUsage(Context context)
    {
        PhotoUtils photoUtils = new PhotoUtils(context);
        ArrayList<String> cameraImages = photoUtils.getCameraImages();
        int nMBwithDate = 0, nMBwithoutDate = 0;
        for(String s : cameraImages)
        {
            File file = new File(s);

            long sizeInBytes = file.length();
            long sizeInMb = sizeInBytes / (1024 * 1024);

            if(file.getName().contains("dtp-"))
            {
                nMBwithDate += sizeInMb;
            }
            else
            {
                nMBwithoutDate += sizeInMb;
            }
        }

        return new int[]{nMBwithDate, nMBwithoutDate};
    }

    public static int getNumberOfPhotosWithDate(ArrayList<String> paths)
    {
        if(paths == null)
            return 0;

        int count = 0;

        for(String path : paths)
        {
            if(path.startsWith("dtp-"))
                count++;
        }

        return count;
    }

    public static ArrayList<String> getPhotosWithoutDate(ArrayList<String> photos)
    {
        ArrayList<String> photosWithoutDate = new ArrayList<String>();

        for(String path : photos)
        {
            File imgFile = new File(path);

            if (!Utils.isAlreadyDatestamped(imgFile))
            {
                File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

                if(!imgFileWithDate.exists())
                {
                    photosWithoutDate.add(path);
                }
            }
        }

        return photosWithoutDate;
    }

    public static ArrayList<String> getPhotosWithoutDate(Context context, ArrayList<String> photos, SQLiteDatabase db)
    {
        ArrayList<String> photosWithoutDate = new ArrayList<String>();

        for(String path : photos)
        {
            File imgFile = new File(path);

            if (!Utils.isAlreadyDatestamped(context, imgFile, db))
            {
                File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

                if(!imgFileWithDate.exists())
                {
                    photosWithoutDate.add(path);
                }
            }
        }

        return photosWithoutDate;
    }

    public static ArrayList<String> getImagesToProcess(Context context, ArrayList<String> photos)
    {
        ArrayList<String> imagesToProcess = new ArrayList<String>();

        for(String path : photos)
        {
            if(Utils.processSelectedFolder(context, PhotoUtils.getParentFolderName(path)))
                imagesToProcess.add(path);
        }

        return imagesToProcess;
    }

    public static boolean containsInteger(Integer integer, Integer[] integers) {
        for(int element : integers) {
            if(integer == element)
                return true;
        }

        return false;
    }

    public static float dpToPixels(float dp, Resources resources)
    {
        final float scale = resources.getDisplayMetrics().density;

        return dp* scale + 0.5f;
    }

    public static boolean isAlreadyDatestamped(File imgFile) {
        //Primero comprobamos si su extensión no es jpg o si su nombre empieza por dtp-, en cuyo caso la consideramos como fechada

        if(!(imgFile.getName().endsWith(".jpg") || imgFile.getName().endsWith(".jpeg")) || imgFile.getName().startsWith("dtp-"))
            return true;

        File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

        if(imgFileWithDate.exists())
            return true;

        return false;
    }

    /*public static boolean isAlreadyDatestamped(Context context, File imgFile, SQLiteDatabase photosDb) {
        //Primero comprobamos si su extensión no es jpg o si su nombre empieza por dtp-, en cuyo caso la consideramos como fechada

        if(!(imgFile.getName().endsWith(".jpg") || imgFile.getName().endsWith(".jpeg")) || imgFile.getName().startsWith("dtp-"))
            return true;

        File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

        if(imgFileWithDate.exists())
            return true;

        //Si la condición anterior fue falsa, entonces buscamos su nombre en la base de datos

        if(photosDb == null || !photosDb.isOpen())
            photosDb = new DatabaseHelper(context, DatabaseHelper.DB_NAME, null, 1).getReadableDatabase();

        Cursor cursor = photosDb.rawQuery("SELECT * FROM " + DatabaseHelper.TABLE_NAME + " WHERE path='" + imgFile.getAbsolutePath() + "' ", null);

        //Comprobamos si la consulta proporcionó resultados (dará, como mucho, 1 resultado)
        //Si hubo 1 resultado, retornamos true, y si no hubo ningún resultado, retornamos false

        boolean result = cursor.moveToFirst();

        cursor.close();

        return result;
    }*/

    public static boolean isAlreadyDatestamped(Context context, File imgFile, SQLiteDatabase photosDb) {
        //Primero comprobamos si su extensión no es jpg o png o si su nombre empieza por dtp-, en cuyo caso la consideramos como fechada

        if(!(imgFile.getName().endsWith(".jpg") || imgFile.getName().endsWith(".jpeg") || imgFile.getName().endsWith(".png"))
                || imgFile.getName().startsWith("dtp-"))
            return true;

        File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

        if(imgFileWithDate.exists())
            return true;

        //Comprobamos si el nombre de la imagen está en la base de datos
        if(photosDb == null || !photosDb.isOpen())
            photosDb = new DatabaseHelper(context, DatabaseHelper.DB_NAME, null, DatabaseHelper.CURRENT_VERSION).getReadableDatabase();

        String searchQuery = "SELECT * FROM " + DatabaseHelper.TABLE_NAME + " WHERE " + DatabaseHelper.PATH_COLUMN + "=?";

        Cursor cursor = photosDb.rawQuery(searchQuery, new String[] {imgFile.getAbsolutePath()});
        boolean result = cursor.moveToFirst();
        cursor.close();

        return result;
    }

    private static boolean isDataOnDb(String tableName, String dbFieldName, String dbFieldValue, SQLiteDatabase db) {
        String query = "Select * from " + tableName + " where " + dbFieldName + " = " + dbFieldValue;

        Cursor cursor = db.rawQuery(query, null);

        boolean result = false;

        if(cursor.getCount() > 0) {
            result = true;
        }

        cursor.close();

        return result;
    }

    public static boolean landscape(Context context) {

        if(context == null)
            return false;

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        return rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
    }

    public static String getRealPathFromURI(Context context, Uri contentUri) {

        if(context == null || contentUri == null)
            return null;

        String[] proj = {MediaStore.Images.Media.DATA};

        CursorLoader cursorLoader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(columnIndex);
    }

    public static void startProcessPhotosService(Context context, ProgressChangedListener listener, ArrayList<String> selectedPaths) {
        //Lanzamos el servicio con las imágenes seleccionadas
        PowerManager powerManager = (PowerManager) context.getSystemService(Activity.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ProcessPhotosWakeLock");

        MyResultReceiver myReceiver = new MyResultReceiver(new Handler());
        myReceiver.setReceiver(listener);
        myReceiver.setWakeLock(wakeLock);

        Intent intent = new Intent(context, ProcessPhotosService.class);
        intent.putExtra("receiver", myReceiver);
        intent.putExtra("onBackground", false);

        if(selectedPaths != null)
            intent.putStringArrayListExtra("cameraimages", selectedPaths);

        context.startService(intent);
    }

    public static void startProcessPhotosURIService(Context context, ProgressChangedListener listener, ArrayList<String> selectedPaths) {
        //Lanzamos el servicio con las imágenes seleccionadas
        PowerManager powerManager = (PowerManager) context.getSystemService(Activity.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ProcessPhotosWakeLock");

        MyResultReceiver myReceiver = new MyResultReceiver(new Handler());
        myReceiver.setReceiver(listener);
        myReceiver.setWakeLock(wakeLock);

        Intent intent = new Intent(context, ProcessPhotosURIService.class);
        intent.putExtra("receiver", myReceiver);
        intent.putExtra("onBackground", false);

        if(selectedPaths != null)
            intent.putStringArrayListExtra("cameraimages", selectedPaths);

        context.startService(intent);
    }

    /**
     * Si el número como long es igual al número como entero, entonces devuelve ese número como entero
     * Sino devuelve el siguiente entero
     *
     * @param value número a redondear
     */
    public static int roundUp(double value) {
        if(value == (int)value)
            return (int)value;

        return (int)value + 1;
    }

    public static String hashFile(File file, String algorithm)  throws NoSuchAlgorithmException, IOException{
        FileInputStream inputStream = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        byte[] bytesBuffer = new byte[1024];
        int bytesRead = -1;

        while((bytesRead = inputStream.read(bytesBuffer)) != -1) {
            digest.update(bytesBuffer, 0, bytesRead);
        }

        byte[] hashedBytes = digest.digest();

        return convertByteArrayToHexString(hashedBytes);
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for(int i=0; i<arrayBytes.length; i++) {
            stringBuffer.append(Integer.toString((arrayBytes[i] & 0xff) + 0x100, 16)
            .substring(1));
        }

        return stringBuffer.toString();
    }

    public static String generateMD5(File file) throws NoSuchAlgorithmException, IOException {
        return hashFile(file, "MD5");
    }

    public static void searchForAlreadyProcessedPhotos(final Context context, final ProgressChangedListener listener) {
        final ArrayList<String> imagesToProcess = getPhotosWithoutDate(
                getImagesToProcess(context, new PhotoUtils(context).getCameraImages()));

        new AsyncTask<Void, Integer, Void>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                listener.reportTotal(imagesToProcess.size());
            }

            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = new DatabaseHelper(context).getWritableDatabase();

                int progress = 0;

                for(String path : imagesToProcess) {
                    try {
                        ExifInterface exifInterface = new ExifInterface(path);

                        String makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE);

                        if(makeExif != null && makeExif.startsWith("dtp-")) {
                            ContentValues values = new ContentValues();
                            values.put(DatabaseHelper.PATH_COLUMN, path);

                            db.insert(DatabaseHelper.TABLE_NAME, null, values);
                        }

                        progress++;
                        publishProgress(progress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                db.close();

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                listener.onProgressChanged(values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                listener.reportEnd(false);
            }
        }.execute();
    }

    public static boolean containsEXIFMAKEdtp(String path) {
        fergaral.datetophoto.exif.ExifInterface exifInterface = new fergaral.datetophoto.exif.ExifInterface();
        try {
            exifInterface.readExif(path);
            String exifMake = exifInterface.getTagStringValue(fergaral.datetophoto.exif.ExifInterface.TAG_MAKE);

            return exifMake != null && exifMake.startsWith("dtp-");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void startActivityCompat(Activity activity, Intent intent) {

        if(activity == null || intent == null)
            return;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());
        }else{
            activity.startActivity(intent);
        }
    }

    public static void lockOrientation(Activity activity) {
        //Bloqueamos la rotación a vertical
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    public static void unlockOrientation(Activity activity) {
        //Desbloqueamos la rotación
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
 }
