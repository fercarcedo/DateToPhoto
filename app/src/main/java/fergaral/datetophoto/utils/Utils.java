package fergaral.datetophoto.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.algorithms.ColorAPIPhotoProcessedAlgorithm;
import fergaral.datetophoto.algorithms.PhotoProcessedAlgorithm;
import fergaral.datetophoto.algorithms.VisionAPIPhotoProcessedAlgorithm;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.services.ProcessPhotosService;
import fergaral.datetophoto.services.ProcessPhotosURIService;
import fergaral.datetophoto.services.TestDatestampDetectionAlgorithmService;
import fergaral.datetophoto.tasks.SearchForAlreadyProcessedPhotosTask;

/**
 * Created by Parejúa on 30/03/2015.
 */
public final class Utils {

    private static final String PERMISSION_NEVER_ASKED_KEY = "permissionneverasked";

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

    public static boolean overwritePhotos(Context context)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(context.getString(R.string.pref_overwrite_key), true);
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
        long firstTime = System.currentTimeMillis();
        String s = "";
        long startTime = System.currentTimeMillis();

        HashMap<String, Boolean> photosMap = new HashMap<>();
        int numPhotos = 0;

        long elapsedTime = System.currentTimeMillis() - startTime;
        s += "Creating ArrayList: " + elapsedTime + "\n";

        startTime = System.currentTimeMillis();

        for(String photo : photos) {
            photosMap.put(photo, false); //false significa que la foto no está fechada
        }

        elapsedTime = System.currentTimeMillis() - startTime;
        s += "Filling HashMap: " + elapsedTime + "\n";

        //Comprobamos si el nombre de la imagen está en la base de datos
        if(db == null || !db.isOpen())
            db = new DatabaseHelper(context).getReadableDatabase();

        String searchQuery = "SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " + DatabaseHelper.TABLE_NAME;

        startTime = System.currentTimeMillis();

        Cursor cursor = db.rawQuery(searchQuery, null);
        if(cursor.moveToFirst()) {
            do {
                String imageName = cursor.getString(0);

                if(photosMap.containsKey(imageName)) {
                    photosMap.put(imageName, true);
                    numPhotos++;
                }
            }while(cursor.moveToNext());
        }
        cursor.close();

        elapsedTime = System.currentTimeMillis() - startTime;
        s += "Iterating database: " + elapsedTime + "\n";

        startTime = System.currentTimeMillis();

        for(String image : photos) {
            File imageFile = new File(image);
            Folders.add(imageFile.getParentFile().getName(),
                    imageFile.getParentFile());

            if(PhotoUtils.incorrectFormat(image)) {
                photosMap.put(image, true);
                numPhotos++;
            }else {
                if(PhotoUtils.getName(image).startsWith("dtp-")) {
                    photosMap.put(image, true);
                    numPhotos++;
                }else if(photosMap.containsKey(PhotoUtils.getFileWithDate(image))) {
                    photosMap.put(image, true);
                    numPhotos++;
                }
            }
        }

        elapsedTime = System.currentTimeMillis() - startTime;
        s += "Final for loop O(n): " + elapsedTime + "\n";

        startTime = System.currentTimeMillis();

        ArrayList<String> photosWithoutDate = new ArrayList<>(numPhotos);

        for(String photo : photos) {
            File photoFile = new File(photo);

            if(photoFile.getParentFile().getName().equals("Date To Photo originals")) {
                //Miramos a ver si hay una foto con su mismo nombre
                String[] nameParts = photoFile.getName().split("-");
                String folderName = nameParts[0]; //Si está fechada, estará en esta carpeta
                String photoName = nameParts[1]; //Si está fechada, tendrá este nombre

                File folderFile = Folders.get(folderName);

                if((!photosMap.containsKey(photoFile.getPath()) || !photosMap.get(photoFile.getPath()))
                && !photosMap.containsKey(new File(folderFile.getPath(), photoName).getPath()))
                    photosWithoutDate.add(photo);
            }else if(photosMap.containsKey(photo) && !photosMap.get(photo)) {
                photosWithoutDate.add(photo);
                Log.d("TAGP", photo);
            }
        }

        elapsedTime = System.currentTimeMillis() - startTime;
        s += "Adding items to ArrayList: " + elapsedTime + "\n";

        long elapsedTimeTotal = System.currentTimeMillis() - firstTime;
        s += "TOTAL: " + elapsedTimeTotal;

        Utils.write(Environment.getExternalStorageDirectory().getPath() + File.separator + "Download"
                                                + File.separator + "dtptimephotowithoutdate.txt", s);

        return photosWithoutDate;
    }

    public static ArrayList<String> getImagesToProcess(Context context, ArrayList<String> photos, String folderName) {
        HashMap<String, Boolean> photosMap = new HashMap<>();
        int numPhotos = 0;

        for(String path : photos) {
            if(PhotoUtils.getParentFolderName(path).equals(folderName)) {
                photosMap.put(path, true);
                numPhotos++;
            }
        }

        ArrayList<String> imagesToProcess = new ArrayList<>(numPhotos);

        for(String path : photos) {
            if(photosMap.containsKey(path) && photosMap.get(path))
                imagesToProcess.add(path);
        }

        return imagesToProcess;
    }

    public static ArrayList<String> getImagesToProcess(Context context, List<String> photos)
    {
        String[] foldersToProcess = getFoldersToProcess(context);
        ArrayList<String> allFolders = PhotoUtils.getFolders(context);
        HashMap<String, Boolean> foldersMap = new HashMap<>();
        int numFolders = 0;

        for(String folderName : allFolders) {
            boolean process = contains(foldersToProcess, folderName);
            foldersMap.put(folderName, process);

            if(process)
                numFolders++;
        }

        ArrayList<String> imagesToProcess = new ArrayList<>(numFolders);

        for(String path : photos) {
            String folderName = PhotoUtils.getParentFolderName(path);
            if(foldersMap.containsKey(folderName) && foldersMap.get(folderName))
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

        if(!(imgFile.getName().toLowerCase().endsWith(".jpg") || imgFile.getName().toLowerCase().endsWith(".jpeg") ||
                imgFile.getName().toLowerCase().endsWith(".png")) || imgFile.getName().startsWith("dtp-"))
            return true;

        File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

        if(imgFileWithDate.exists())
            return true;

        //Comprobamos si el nombre de la imagen está en la base de datos
        if(photosDb == null || !photosDb.isOpen())
            photosDb = new DatabaseHelper(context).getReadableDatabase();

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
        boolean hasResult = cursor.moveToFirst();

        if(hasResult) {
            String result = cursor.getString(columnIndex);
            cursor.close();
            return result;
        }else{
            cursor.close();
            return null;
        }
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

    public static void searchForAlreadyProcessedPhotos(Context context, ProgressChangedListener listener) {
        final ArrayList<String> imagesToProcess = new PhotoUtils(context).getCameraImages();
        new SearchForAlreadyProcessedPhotosTask(listener, imagesToProcess, context).execute();
    }

    public static void testVisionDatestampDetectionAlgorithm(Context context) {
        testDatestampDetectionAlgorithm(context, new VisionAPIPhotoProcessedAlgorithm());
    }

    public static void testColorDatestampDetectionAlgorithm(Context context) {
        testDatestampDetectionAlgorithm(context, new ColorAPIPhotoProcessedAlgorithm());
    }

    private static void testDatestampDetectionAlgorithm(Context context, PhotoProcessedAlgorithm algorithm) {
        Intent intent = new Intent(context, TestDatestampDetectionAlgorithmService.class);
        intent.putExtra(TestDatestampDetectionAlgorithmService.ALGORITHM_KEY, algorithm);
        context.startService(intent);
    }

    public static void searchForAlreadyProcessedPhotos(Context context) {
        ArrayList<String> imagesToProcess = new PhotoUtils(context).getCameraImages();

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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        db.close();
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

    public static String getFormattedDate(Date date) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        if (df instanceof SimpleDateFormat)
        {
            SimpleDateFormat sdf = (SimpleDateFormat) df;
            // To show Locale specific short date expression with full year
            String pattern = sdf.toPattern().replaceAll("y+","yyyy")
                                            .replaceAll("d+", "dd")
                                            .replaceAll("M+", "MM");
            sdf.applyPattern(pattern);
            return sdf.format(date);
        }

        return df.format(date);
    }

    public static double getPhotosWithoutDate2(Context context, List<String> photos, SQLiteDatabase db)
    {
        LinkedList<String> photosWithoutDate = new LinkedList<>(photos);
        long startTimeMillis = System.currentTimeMillis();

        //Comprobamos si el nombre de la imagen está en la base de datos
        if(db == null || !db.isOpen())
            db = new DatabaseHelper(context).getReadableDatabase();

        String searchQuery = "SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " + DatabaseHelper.TABLE_NAME;

        Cursor cursor = db.rawQuery(searchQuery, null);
        if(cursor.moveToFirst()) {
            do {
                photosWithoutDate.remove(cursor.getString(0));
            }while(cursor.moveToNext());
        }
        cursor.close();

        LinkedList<String> result = new LinkedList<>(photosWithoutDate);

        for(int i=0; i < photosWithoutDate.size(); i++) {
            String image = photosWithoutDate.get(i);
            File imgFile = new File(image);
            if(!(imgFile.getName().toLowerCase().endsWith(".jpg") || imgFile.getName().toLowerCase().endsWith(".jpeg") ||
                    imgFile.getName().toLowerCase().endsWith(".png")) || imgFile.getName().startsWith("dtp-"))
                result.remove(image);
            else {
                File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

                if (imgFileWithDate.exists())
                    result.remove(image);
            }
        }

        long endTimeMillis = System.currentTimeMillis() - startTimeMillis;

        return endTimeMillis / 1000d;
    }

    private static void saveListStringToSharedPreferences(Context context, List<String> list) {
        /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(TAG, json);
        editor.commit();*/
    }

    private static void readListStringFromSharedPreferences(Context context, List<String> list) {
        /*
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(TAG, null);
        Type type = new TypeToken<ArrayList<ArrayObject>>() {}.getType();
        ArrayList<ArrayObject> arrayList = gson.from(json, type);
         */
    }

    private static boolean contains(String[] array, String value) {
        for(String element : array) {
            if(element.equals(value))
                return true;
        }

        return false;
    }

    public static boolean permissionNeverAsked(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(Utils.PERMISSION_NEVER_ASKED_KEY, true);
    }

    public static void setPermissionAsked(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Utils.PERMISSION_NEVER_ASKED_KEY, false);
        editor.apply();
    }

    public static int getColor(int rgb) {
       return getColor(rgbToHSV(rgb));
    }

    public static float[] rgbToHSV(int rgb) {
        float hsv[] = new float[3];
        Color.colorToHSV(rgb, hsv);

        return hsv;
    }

    public static int getColor(float[] hsb) {
        if      (hsb[1] < 0.1 && hsb[2] > 0.9) return Color.WHITE;
        else if (hsb[2] < 0.1) return Color.BLACK;
        else {
            float deg = hsb[0]*360;
            if      (deg >=   0 && deg <  30) return Color.RED;
            else if (deg >=  30 && deg <  90) return Color.YELLOW;
            else if (deg >=  90 && deg < 150) return Color.GREEN;
            else if (deg >= 150 && deg < 210) return Color.CYAN;
            else if (deg >= 210 && deg < 270) return Color.BLUE;
            else if (deg >= 270 && deg < 330) return Color.MAGENTA;
            else return Color.RED;
        }
    }

    public static String getColorName(int color) {
        switch(color) {
            case Color.WHITE:
                return "WHITE";
            case Color.BLACK:
                return "BLACK";
            case Color.RED:
                return "RED";
            case Color.YELLOW:
                return "YELLOW";
            case Color.GREEN:
                return "GREEN";
            case Color.CYAN:
                return "CYAN";
            case Color.BLUE:
                return "BLUE";
            case Color.MAGENTA:
                return "MAGENTA";
            default:
                return "UNDEFINED";
        }
    }
 }
