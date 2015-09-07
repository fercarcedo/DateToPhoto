package fergaral.datetophoto.services;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.MyActivity;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.exif.ExifTag;
import fergaral.datetophoto.fragments.MaterialProgressDialogFragment;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.receivers.PowerConnectionReceiver;
import fergaral.datetophoto.utils.NotificationUtils;
import fergaral.datetophoto.utils.Utils;

public class ProcessPhotosService extends IntentService {

    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_CANCEL_CHARGER_DISCONNECTED = "cancel_charger_disconnected";
    public static final String CANCEL_SERVICE = "cancel";

    private MediaScannerConnection msConn;
    private boolean onBackground = true;
    private ResultReceiver receiver;
    private ProgressChangedListener secondListener;
    private String tempStr = "";
    private boolean dialogCancelled;
    private boolean cancelledCharger;
    private NotificationCompat.Builder mNotifBuilder;
    private NotificationManager mNotifManager;
    private long mNotifStartTime;
    private boolean running;
    private int total, actual;
    private float scale;
    private Paint paint;
    private SQLiteDatabase photosDb;
    private NotificationUtils mNotificationUtils;

    public ProcessPhotosService() {
        super("ProcessPhotosService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mNotificationUtils = new NotificationUtils(this);
        scale = getResources().getDisplayMetrics().density;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.YELLOW);
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
        photosDb = new DatabaseHelper(this).getWritableDatabase();

        receiver = intent.getParcelableExtra("receiver");
        onBackground = intent.getBooleanExtra("onBackground", true);

        if(onBackground) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean active = sharedPreferences.getBoolean(getString(R.string.pref_active_key), true);

            if (!active) {
                PowerConnectionReceiver.completeWakefulIntent(intent);

                if(photosDb != null)
                    photosDb.close();

                return;
            }
        }

        running = true;

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(running) {
                    secondListener = (ProgressChangedListener) intent.getSerializableExtra("listener");

                    if (secondListener != null) {
                        secondListener.reportTotal(total);
                        secondListener.onProgressChanged(actual);
                    }
                }
            }
        }, new IntentFilter(MyActivity.INTENT_QUERY_ACTION));

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent sendIntent = new Intent(MaterialProgressDialogFragment.PROGRESS_ACTION_SEND);
                sendIntent.putExtra(MaterialProgressDialogFragment.PROGRESS_KEY, actual);
                LocalBroadcastManager.getInstance(ProcessPhotosService.this).sendBroadcast(sendIntent);
            }
        }, new IntentFilter(MaterialProgressDialogFragment.PROGRESS_ACTION_QUERY));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showNotif = sharedPreferences.getBoolean(getString(R.string.pref_shownotification_key), true);
        boolean keepLargePhoto = sharedPreferences.getBoolean(getString(R.string.pref_keeplargephoto_key), true);

        if (onBackground) {
            //Si estamos ejecutando esto mientras el dispositivo se está cargando, y hay 0 fotos en la base
            //de datos (primer uso), buscamos fotos ya fechadas
            SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.PATH_COLUMN + " FROM " +
                    DatabaseHelper.TABLE_NAME, null);

            if (!cursor.moveToFirst()) {
                cursor.close();

                if(showNotif)
                    mNotificationUtils.showStandAloneNotification("Buscando fotos ya fechadas...");

                Utils.searchForAlreadyProcessedPhotos(this);
            }
        }

        if(showNotif)
            mNotificationUtils.showProgressNotification("Procesando tus fotos en segundo plano...");

        ArrayList<String> galleryImages;

        if(intent.getStringArrayListExtra("cameraimages") != null) {
            galleryImages = intent.getStringArrayListExtra("cameraimages");
        }else{
            galleryImages = getCameraImages(this);
        }

        if(galleryImages != null) {
            galleryImages = Utils.getPhotosWithoutDate(this, Utils.getImagesToProcess(this, galleryImages), photosDb);
        }

        if (galleryImages != null) {
            total = 0;
            actual = 0;
            total = galleryImages.size();

            if(receiver != null)
            {
                Bundle bundle = new Bundle();
                bundle.putInt("total", total);

                receiver.send(Activity.RESULT_OK, bundle);
            }

            LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    dialogCancelled = intent.getBooleanExtra("dialogcancelled", false);
                }
            }, new IntentFilter(MyActivity.INTENT_ACTION));

            LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(onBackground)
                        cancelledCharger = intent.getBooleanExtra(ProcessPhotosService.CANCEL_SERVICE, false);
                }
            }, new IntentFilter(ProcessPhotosService.ACTION_CANCEL_CHARGER_DISCONNECTED));

            boolean isHoneycomb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

            for (String s : galleryImages) {
                File imgFile = new File(s);
                if (imgFile.exists() && !dialogCancelled && !cancelledCharger) {


                    //if (!Utils.isAlreadyDatestamped(imgFile)) {


                    //File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

                    //if (!imgFileWithDate.exists() && Utils.processSelectedFolder(this, PhotoUtils.getParentFolderName(s))) {

                    //Primero obtenemos ancho y alto de la imagen
                    BitmapFactory.Options options = new BitmapFactory.Options();

                    //Al establecerlo a true, el decodificador retornará null, pero options tendrá el ancho y alto
                    //de ese bitmap
                    options.inJustDecodeBounds = true;

                    BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                    int imageWidth = options.outWidth;
                    int imageHeight = options.outHeight;

                    //Hay que asegurarse de que nuestro bitmap no exceda el tamaño de maxMemory
                    // (Runtime.getRuntime().maxMemory()), teniendo en cuenta que cada píxel ocupa 4 bytes
                    final long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
                    long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
                    long freeMemory = maxMemory - usedMemory;

                    long imageSize = (imageWidth * imageHeight * 4) / 1024; //Este valor era 12 unidades menos que el que debía ser
                    boolean wasLarge = false;

                    if (imageSize >= freeMemory) {
                        wasLarge = true;
                    }

                    keepLargePhoto = keepLargePhoto && wasLarge;

                    while (imageSize >= freeMemory) {
                        imageWidth /= 1.15;
                        imageHeight /= 1.15;
                        imageSize = (imageWidth * imageHeight * 4) / 1024;
                    }

                    long previousSize = (options.outWidth * options.outHeight * 4) / 1024;
                    //double reduction = (double)previousSize / imageSize;
                    int reduction = (int) Math.ceil((double) previousSize / imageSize);

                    options = new BitmapFactory.Options();

                    if(isHoneycomb)
                        options.inMutable = true;

                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    if (wasLarge)
                        options.inSampleSize = reduction;

                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                    if (myBitmap == null) {
                        actual++;

                        if (receiver != null) {
                            Bundle bundle = new Bundle();
                            bundle.putInt("progress", actual);

                            receiver.send(Activity.RESULT_OK, bundle);
                        }

                        if (secondListener != null)
                            secondListener.onProgressChanged(actual);

                        mNotificationUtils.setNotificationProgress(total, actual);

                        //Si la imagen no pudo ser decodificada, le añadimos dtp- al nombre para no volver a procesarla
                        imgFile.renameTo(new File(
                                imgFile.getParentFile().getAbsolutePath() + "/" + "dtp-" + imgFile.getName()));
                        scanPhoto(imgFile.getParentFile().getAbsolutePath() + "/" + imgFile.getName().substring(4));

                        continue;
                    }

                    tempStr += s + " -> ";

                    if(!isHoneycomb)
                        myBitmap = convertToMutable(myBitmap);

                    String date = "";
                    int rotation = ExifInterface.ORIENTATION_NORMAL;

                    try {
                        ExifInterface exifInterface = new ExifInterface(imgFile.getAbsolutePath());
                        date = getExifTag(exifInterface, ExifInterface.TAG_DATETIME, imgFile);
                        rotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Bitmap bitmap2 = writeDateOnBitmap(myBitmap, date, rotation);

                    //Este método sirve para comprobar la rotación con la segunda interfaz EXIF
                    testEXIFDate(imgFile.getAbsolutePath());

                    myBitmap = null;

                            /*if(Utils.overwritePhotos(this)) {
                                //CapturePhotoUtils.insertImage(getContentResolver(), bitmap2, imgFile.getName() + "-dtp.jpg", "generated using Date To Photo");
                                savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtpo-" + imgFile.getName(), imgFile, true
                                );
                            }else{
                                savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtp-" + imgFile.getName(), imgFile, true
                                );
                            }*/

                    if (Utils.overwritePhotos(this) && !keepLargePhoto) {
                        savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtpo-" + imgFile.getName(), imgFile, true
                        );

                        registerPhotoIntoDb(imgFile.getAbsolutePath());
                    } else {
                        savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtp-" + imgFile.getName(), imgFile, true
                        );
                    }

                    bitmap2 = null;

                           /* if(Utils.overwritePhotos(this))
                            {
                                PhotoUtils.deletePhoto(this, s);
                            }*/
                    //}
                    //}
                }

                actual++;

                if(receiver != null)
                {
                    Bundle bundle = new Bundle();
                    bundle.putInt("progress", actual);

                    receiver.send(Activity.RESULT_OK, bundle);
                }

                if(secondListener != null)
                    secondListener.onProgressChanged(actual);

                if(showNotif)
                    mNotificationUtils.setNotificationProgress(total, actual);

                imgFile = null;
            }
        }

        Utils.write(Environment.getExternalStorageDirectory().getPath() + "/Download/datetophotoimages.txt", tempStr);

        if(showNotif)
            mNotificationUtils.endProgressNotification("El proceso ha finalizado");

        running = false;

        if(photosDb != null)
            photosDb.close();

        if(onBackground) {
            PowerConnectionReceiver.completeWakefulIntent(intent);
        }


        if(receiver != null)
        {
            Bundle bundle = new Bundle();
            bundle.putString("end", "end");

            receiver.send(Activity.RESULT_OK, bundle);
        }

        if(secondListener != null)
            secondListener.reportEnd(false);

        if(dialogCancelled)
            dialogCancelled = false;
    }

    private String getExifTag(ExifInterface exif, String tag, File imgFile) {
        //De la forma: 2014:09:21 13:53:58
        String attribute = exif.getAttribute(tag);

        if(attribute != null) {
            /*String date = attribute.substring(0, attribute.indexOf(" ")); //Ejemplo: 2014:09:21
            StringBuffer buffer = new StringBuffer(date);

            for (int i = 0; i < date.length(); i++) {
                if (date.charAt(i) == ':')
                    buffer.setCharAt(i, '/');
            }

            String dateString = "";

            try {
                Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(buffer.toString());
                dateString = new SimpleDateFormat("dd-MM-yyyy").format(date1);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return dateString;*/

           int year = Integer.parseInt(attribute.substring(0, 4));
           int month = Integer.parseInt(attribute.substring(5, 7));
           int day = Integer.parseInt(attribute.substring(8, 10));

           //return day + "/" + month + "/" + year;

            //Return a localized date String
            Calendar cal = Calendar.getInstance();
            cal.set(year, month-1, day);

            return Utils.getFormattedDate(cal.getTime());
        }

        return Utils.getFormattedDate(new Date(imgFile.lastModified()));
    }

    public void showProgress(float prog) {

        int progress = (int) prog * 100;

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Date To Photo")
                .setContentText("Procesando tus fotos en un servicio...")
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(100, progress, true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notifBuilder.build());
    }

    public void showNotification(String text) {
        Intent resultIntent = new Intent(this, MyActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Date To Photo")
                .setContentText(text)
                .setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notifBuilder.build());
    }

    public Bitmap writeDateOnBitmap(Bitmap b, String text, int orientation) {

        Bitmap.Config bitmapConfig = b.getConfig();

        if(bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888;
        }

        //b = b.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(b);

        paint.setTextSize(Math.min(b.getWidth() / 20, b.getHeight() / 20));

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        //int x = (b.getWidth() - bounds.width()) / 2;
        //int y = (b.getHeight() + bounds.height()) / 2;

        double marginWidth = (b.getWidth() * 0.2) / 10;
        double marginHeight = (b.getHeight() * 0.2) / 10;
        int x = (int) (b.getWidth() - bounds.width() - marginWidth);
        int y = (int) (b.getHeight() - marginHeight);

        switch(orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_270:
            {
                canvas.save();

                x = bounds.height(); //altotexto
                y = (int) (b.getHeight() - bounds.width() - marginHeight); //alto - anchotexto

                canvas.rotate(-270, x, y);

                canvas.drawText(text, x, y, paint);

                canvas.restore();
                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_90:
            {
                canvas.save();

                /*x = b.getWidth() - bounds.height(); //ancho - altotexto
                y = (int) (bounds.width() + margin); //anchotexto*/

                x = b.getWidth() - bounds.height(); //ancho - altotexto
                y = (int) (bounds.width() + marginHeight); //anchotexto

                canvas.rotate(-90, x, y);


                canvas.drawText(text, x, y, paint);

                canvas.restore();
                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_180:
            {
                canvas.save();

                x = (int) (bounds.width() + marginWidth);
                y = (int) marginHeight;

                canvas.rotate(-180, x, y);

                canvas.drawText(text, x, y, paint);

                canvas.restore();
                break;
            }
            default:
            {
                canvas.drawText(text, x, y, paint);
                break;
            }
        }

        switch(orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_270: tempStr += "ORIENTATION_ROTATE_270"; break;
            case ExifInterface.ORIENTATION_TRANSVERSE: tempStr += "ORIENTATION_TRANSVERSE"; break;
            case ExifInterface.ORIENTATION_TRANSPOSE: tempStr += "ORIENTATION_TRANSPOSE"; break;
            case ExifInterface.ORIENTATION_ROTATE_90: tempStr += "ORIENTATION_ROTATE_90"; break;
            case ExifInterface.ORIENTATION_UNDEFINED: tempStr += "ORIENTATION_UNDEFINED"; break;
            case ExifInterface.ORIENTATION_ROTATE_180: tempStr += "ORIENTATION_ROTATE_180"; break;
            case ExifInterface.ORIENTATION_NORMAL: tempStr += "ORIENTATION_NORMAL"; break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: tempStr += "ORIENTATION_FLIP_HORIZONTAL"; break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: tempStr += "ORIENTATION_FLIP_VERTICAL"; break;
        }

        tempStr += "\n";

        //Toast.makeText(this, String.valueOf(b.getHeight()), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Height: "+ String.valueOf(b.getHeight()) + " Width: " + String.valueOf(b.getWidth()), Toast.LENGTH_LONG).show();
        return b;
    }

    public ArrayList<String> getCameraImages(Context context) {
        // Set up an array of the Thumbnail Image ID column we want
        String[] projection = {MediaStore.Images.Media.DATA};


        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);

        ArrayList<String> result = null;

        if(cursor != null) {
            result = new ArrayList<String>(cursor.getCount());


            if (cursor.moveToFirst()) {
                final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                do {
                    final String data = cursor.getString(dataColumn);
                    //Log.i("data :", data);
                    result.add(data);
                } while (cursor.moveToNext());
            }
            cursor.close();

            //String uri3 = result.get(2);

       /*File imgFile = new File(uri3);
       if(imgFile.exists()) {
           Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
           iv1.setImageBitmap(myBitmap);
       }*/
        }
        else
        {
            final Cursor cursor1 = context.getContentResolver().query(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    null);

            if(cursor1 != null) {
                result = new ArrayList<String>(cursor1.getCount());


                if (cursor1.moveToFirst()) {
                    final int dataColumn = cursor1.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    do {
                        final String data = cursor1.getString(dataColumn);
                        //Log.i("data :", data);
                        result.add(data);
                    } while (cursor1.moveToNext());
                }
                cursor1.close();
            }
            else
            {
                result = null;
            }
        }
        return result;
    }

    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            if(type == null)
                type = Bitmap.Config.ARGB_8888;

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }

    public void savePhoto(Bitmap bmp, String basePath, String name, File imageFrom, boolean keepOrientation)
    {
        File imageFileFolder = new File(basePath);

        FileOutputStream out = null;
        File imageFileName = new File(imageFileFolder, name);
        try
        {
            out = new FileOutputStream(imageFileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            moveEXIFdata(imageFrom, imageFileName, keepOrientation);

            if(imageFileName.getName().startsWith("dtpo-"))  //Si se cumple, sobreescribimos
                imageFileName.renameTo(new File(imageFileName.getParentFile().getAbsolutePath() + "/" + imageFileName.getName().substring(5)));

            scanPhoto(imageFileName.toString());
            out = null;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void moveEXIFdata(File imageFrom, File imageTo, boolean keepOrientation)

    {
        try {
            ExifInterface exifInterfaceFrom = new ExifInterface(imageFrom.getAbsolutePath());
            ExifInterface exifInterfaceTo = new ExifInterface(imageTo.getAbsolutePath());

            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME));
            else
            {
                Date lastModDate = new Date(imageFrom.lastModified());
                Calendar cal = Calendar.getInstance();
                cal.setTime(lastModDate);
                String year = getStringOfNumber(cal.get(Calendar.YEAR));
                String month = getStringOfNumber(cal.get(Calendar.MONTH) + 1);
                String day = getStringOfNumber(cal.get(Calendar.DAY_OF_MONTH));
                String hours = getStringOfNumber(cal.get(Calendar.HOUR));
                String minutes = getStringOfNumber(cal.get(Calendar.MINUTE));
                String seconds = getStringOfNumber(cal.get(Calendar.SECOND));
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, year+":"+month+":"+day+" "+hours+":"+minutes+":"+seconds);
            }
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FLASH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE) != null) {
                if(Utils.overwritePhotos(this))
                    exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, "dtp-" + exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE));
                else
                    exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE));
            }else{
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, "dtp-");
            }
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MODEL, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE));
            if(keepOrientation && exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_ORIENTATION, exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION));
            else
                exifInterfaceTo.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_UNDEFINED));

            exifInterfaceTo.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scanPhoto(final String imageFileName)
    {

       /*     msConn = new MediaScannerConnection(MyActivity.this,new MediaScannerConnection.MediaScannerConnectionClient()
        {
            public void onMediaScannerConnected()
            {
                msConn.scanFile(imageFileName, null);
            }
            public void onScanCompleted(String path, Uri uri)
            {
                msConn.disconnect();
            }
        });
        msConn.connect();*/

        /*Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.parse(imageFileName));
        sendBroadcast(intent);*/

        MediaScannerConnection.scanFile(
                getApplicationContext(),
                new String[]{imageFileName},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }

    public void moveEXIFdata(File imageFrom, File imageTo)
    {
        try {
            ExifInterface exifInterfaceFrom = new ExifInterface(imageFrom.getAbsolutePath());
            ExifInterface exifInterfaceTo = new ExifInterface(imageTo.getAbsolutePath());
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, exifInterfaceFrom.getAttribute(ExifInterface.TAG_DATETIME));
            else
            {
                Date lastModDate = new Date(imageFrom.lastModified());
                Calendar cal = Calendar.getInstance();
                cal.setTime(lastModDate);
                String year = getStringOfNumber(cal.get(Calendar.YEAR));
                String month = getStringOfNumber(cal.get(Calendar.MONTH) + 1);
                String day = getStringOfNumber(cal.get(Calendar.DAY_OF_MONTH));
                String hours = getStringOfNumber(cal.get(Calendar.HOUR_OF_DAY));
                String minutes = getStringOfNumber(cal.get(Calendar.MINUTE));
                String seconds = getStringOfNumber(cal.get(Calendar.SECOND));
                exifInterfaceTo.setAttribute(ExifInterface.TAG_DATETIME, year+":"+month+":"+day+" "+hours+":"+minutes+":"+seconds);
            }
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FLASH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FLASH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_FOCAL_LENGTH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_DATESTAMP));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, exifInterfaceFrom.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_LENGTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_LENGTH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_IMAGE_WIDTH, exifInterfaceFrom.getAttribute(ExifInterface.TAG_IMAGE_WIDTH));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MAKE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MAKE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_MODEL, exifInterfaceFrom.getAttribute(ExifInterface.TAG_MODEL));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_WHITE_BALANCE, exifInterfaceFrom.getAttribute(ExifInterface.TAG_WHITE_BALANCE));
            if(exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION) != null)
                exifInterfaceTo.setAttribute(ExifInterface.TAG_ORIENTATION, exifInterfaceFrom.getAttribute(ExifInterface.TAG_ORIENTATION));
            exifInterfaceTo.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getStringOfNumber(int number)
    {
        String numberString;
        if(number < 10)
        {
            numberString = "0" + String.valueOf(number);
        }
        else
        {
            numberString = String.valueOf(number);
        }

        return numberString;
    }

    private void registerPhotoIntoDb(String path) {
        if(photosDb == null || !photosDb.isOpen()) {
            photosDb = new DatabaseHelper(this).getWritableDatabase();
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.PATH_COLUMN, path);

        photosDb.insert(DatabaseHelper.TABLE_NAME, null, values);
    }

    /**
     * Este método obtiene la orientación de la foto con la otra interfaz EXIF y escribe otra línea
     * en el archivo de logs con la información
     *
     * @param imgPath ruta de la imagen para obtener su orientación de EXIF
     */
    private void testEXIFDate(String imgPath) {
        fergaral.datetophoto.exif.ExifInterface exifInterface = new fergaral.datetophoto.exif.ExifInterface();

        try {
            exifInterface.readExif(imgPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Object orientationObj = exifInterface.getTagValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION);
        int orientation = fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT; //ExifInterface.ORIENTATION_NORMAL

        if(orientationObj != null)
            orientation = exifInterface.getTagIntValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION);

        tempStr += imgPath + " -> ";

        switch(orientation) {
            case fergaral.datetophoto.exif.ExifInterface.Orientation.BOTTOM_LEFT:
                tempStr += "BOTTOM_LEFT";
                break;
            case fergaral.datetophoto.exif.ExifInterface.Orientation.BOTTOM_RIGHT:
                tempStr += "BOTTOM_RIGHT";
                break;
            case fergaral.datetophoto.exif.ExifInterface.Orientation.LEFT_BOTTOM:
                tempStr += "LEFT_BOTTOM";
                break;
            case fergaral.datetophoto.exif.ExifInterface.Orientation.LEFT_TOP:
                tempStr += "LEFT_TOP";
                break;
            case fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_BOTTOM:
                tempStr += "RIGHT_BOTTOM";
                break;
            case fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_TOP:
                tempStr += "RIGHT_TOP";
                break;
            case fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT:
                tempStr += "TOP_LEFT";
                break;
            case fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_RIGHT:
                tempStr += "TOP_RIGHT";
                break;
        }

        tempStr += "\n";
    }
}
