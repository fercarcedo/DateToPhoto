package fergaral.datetophoto.services;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.MyActivity;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.exif.ExifInterface;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.receivers.PowerConnectionReceiver;
import fergaral.datetophoto.utils.NotificationUtils;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.Utils;

public class ProcessPhotosURIService extends IntentService {

    private static final int NOTIFICATION_ID = 1;

    private MediaScannerConnection msConn;
    private boolean onBackground = true;
    private ResultReceiver receiver;
    private String tempStr = "";
    private boolean dialogCancelled;
    private NotificationCompat.Builder mNotifBuilder;
    private NotificationManager mNotifManager;
    private long mNotifStartTime;
    private boolean running, showNotif;
    private int total, actual;
    private ProgressChangedListener secondListener;
    private NotificationUtils mNotificationUtils;
    private boolean wasLarge;
    private boolean wasPathFound = true;

    public ProcessPhotosURIService() {
        super("ProcessPhotosURIService");
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

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
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

    @Override
    protected void onHandleIntent(Intent intent) {

        mNotificationUtils = new NotificationUtils(this);
        receiver = intent.getParcelableExtra("receiver");
        onBackground = intent.getBooleanExtra("onBackground", true);

        if (onBackground) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean active = sharedPreferences.getBoolean(getString(R.string.pref_active_key), true);

            if (!active) {
                PowerConnectionReceiver.completeWakefulIntent(intent);
                return;
            }
        }

        running = true;

        /*LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if (running) {
                    myReceiver = intent.getParcelableExtra("receiver");

                    if (myReceiver != null) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("total", total);

                        myReceiver.send(Activity.RESULT_OK, bundle);

                        bundle = new Bundle();
                        bundle.putInt("progress", actual);

                        myReceiver.send(Activity.RESULT_OK, bundle);
                    }
                }
            }
        }, new IntentFilter(MyActivity.INTENT_QUERY_ACTION));*/

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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        showNotif = sharedPreferences.getBoolean(getString(R.string.pref_shownotification_key), true);

        if (showNotif)
            mNotificationUtils.showProgressNotification("Procesando tus fotos en segundo plano...");

        ArrayList<Uri> galleryImages = new ArrayList<>();

        if (intent.getStringArrayListExtra("cameraimages") != null) {
            ArrayList<String> galleryImagesString = intent.getStringArrayListExtra("cameraimages");

            for (String uriString : galleryImagesString) {
                galleryImages.add(Uri.parse(uriString));
            }

            Log.d("MIDEBUG", String.valueOf(galleryImages.size()));
        } else {
            return;
        }

        total = 0;
        actual = 0;
        total = galleryImages.size();

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                dialogCancelled = intent.getBooleanExtra("dialogcancelled", false);
            }
        }, new IntentFilter(MyActivity.INTENT_ACTION));

        if (receiver != null) {
            Bundle bundle = new Bundle();
            bundle.putInt("total", total);

            receiver.send(Activity.RESULT_OK, bundle);
        }

        if (secondListener != null) {
            secondListener.reportTotal(total);
        }

        boolean isHoneycomb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

        for (Uri uri : galleryImages) {
            if (!dialogCancelled) {

                //Primero comprobamos si su extensión es soportada
                ContentResolver contentResolver = getContentResolver();
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                String fileExtension = mime.getExtensionFromMimeType(contentResolver.getType(uri));

                if(fileExtension != null) {
                    if (!(fileExtension.equals("jpeg") || fileExtension.equals("jpg") || fileExtension.equals("png")))
                        continue;
                }else{
                    //Podría ser que la URI fuese de la forma file:/// (porque las de la forma content:/// pasan siempre
                    //por el if). Intentamos ver si no acaba en alguno de los formatos admitidos

                    String uriString = uri.toString();

                    if(!(uriString.endsWith(".jpeg") || uriString.endsWith(".jpg") || uriString.endsWith(".png"))) {
                        continue;
                    }
                }

                Bitmap myBitmap = null;

                try {
                    //myBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    myBitmap = decodeUri(this, uri);
                }catch(IOException e) {
                    e.printStackTrace();
                    continue;
                }

                if (myBitmap == null) {
                    actual++;

                    if (receiver != null) {
                        Bundle bundle = new Bundle();
                        bundle.putInt("progress", actual);

                        receiver.send(Activity.RESULT_OK, bundle);
                    }

                    if (secondListener != null) {
                        secondListener.onProgressChanged(actual);
                    }

                    mNotificationUtils.setNotificationProgress(total, actual);

                    continue;
                }

                tempStr += uri.toString() + " -> ";

                if(!isHoneycomb)
                    myBitmap = convertToMutable(myBitmap);

                String date = "";
                String exifDate = "";
                int orientationAndroidExif = android.media.ExifInterface.ORIENTATION_NORMAL;
                //int rotation = ExifInterface.Orientation

                try {
                    ExifInterface exifInterface = new ExifInterface();
                    exifInterface.readExif(getContentResolver().openInputStream(uri));
                    date = getExifDate(exifInterface);
                    exifDate = exifInterface.getTagStringValue(ExifInterface.TAG_DATE_TIME);

                    //Obtenemos la rotación de la imagen y la convertimos a las constantes de android.media.ExifInterface
                    Object orientationObj = exifInterface.getTagValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION);
                    int orientation = fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT; //ExifInterface.ORIENTATION_NORMAL

                    if(orientationObj != null)
                        orientation = exifInterface.getTagIntValue(fergaral.datetophoto.exif.ExifInterface.TAG_ORIENTATION);


                    switch(orientation) {
                        case fergaral.datetophoto.exif.ExifInterface.Orientation.BOTTOM_LEFT:
                            orientationAndroidExif = android.media.ExifInterface.ORIENTATION_ROTATE_180;
                            break;
                        case fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_BOTTOM:
                            orientationAndroidExif = android.media.ExifInterface.ORIENTATION_ROTATE_270;
                            break;
                        case fergaral.datetophoto.exif.ExifInterface.Orientation.RIGHT_TOP:
                            orientationAndroidExif = android.media.ExifInterface.ORIENTATION_ROTATE_90;
                            break;
                        case fergaral.datetophoto.exif.ExifInterface.Orientation.TOP_LEFT:
                            orientationAndroidExif = android.media.ExifInterface.ORIENTATION_NORMAL;
                            break;
                    }

                    if(exifDate != null) {
                        exifDate = exifDate.replaceAll(" ", "").replaceAll(":", "");
                    }else{
                        //Si no hay fecha EXIF, le ponemos de nombre la fecha de hoy, con el mismo formato
                        exifDate = getCurrentDate("");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(exifDate.equals("")) {
                    exifDate = getCurrentDate("");
                }

                if(date.equals("")) {
                    date = getCurrentLocalizedDate();
                }

                Bitmap bitmap2 = writeDateOnBitmap(myBitmap, date, orientationAndroidExif);

                myBitmap = null;

                File file2 = new File(Environment.getExternalStorageDirectory().getPath() + "/DateToPhoto");

                if(!file2.exists())
                    file2.mkdir();

                boolean hasRealPath = false;
                String filePath = "";

                try {
                    filePath = Utils.getRealPathFromURI(this, uri);

                    if (filePath != null) {
                        //Hay ruta real
                        hasRealPath = true;
                        SQLiteDatabase db = new DatabaseHelper(this).getWritableDatabase();
                        ContentValues values = new ContentValues();
                        values.put(DatabaseHelper.PATH_COLUMN, filePath);

                        db.insert(DatabaseHelper.TABLE_NAME, null, values);
                        db.close();
                    } else {
                        Log.d("TAG", "No hay ruta real");
                    }
                } catch (Exception e) {
                    //No hay ruta real, pasamos
                    Log.d("TAG", "No hay ruta real");
                }

                if(!hasRealPath) {
                    wasPathFound = false;
                    savePhoto(bitmap2, Environment.getExternalStorageDirectory().getPath() + "/DateToPhoto", "dtp-" + exifDate + ".jpg");
                }else{
                    File imgFile = new File(filePath);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    boolean overwritePhotos = prefs.getBoolean(getString(R.string.pref_overwrite_key), true);
                    boolean keepLargePhoto = prefs.getBoolean(getString(R.string.pref_keeplargephoto_key), true);
                    keepLargePhoto = keepLargePhoto && wasLarge;

                    if(overwritePhotos && !keepLargePhoto) {
                        savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), imgFile.getName());
                    }else{
                        savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtp-" + imgFile.getName());
                    }
                }

                try {
                    android.media.ExifInterface exifInterface;

                    if(!hasRealPath) {
                        exifInterface = new android.media.ExifInterface(new File(
                                Environment.getExternalStorageDirectory().getPath() + "/DateToPhoto/" + "dtp-" + exifDate + ".jpg").getAbsolutePath());
                    }else{
                        File imgFile = new File(filePath);
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                        boolean overwritePhotos = prefs.getBoolean(getString(R.string.pref_overwrite_key), true);
                        boolean keepLargePhoto = prefs.getBoolean(getString(R.string.pref_keeplargephoto_key), true);
                        keepLargePhoto = keepLargePhoto && wasLarge;

                        if(overwritePhotos && !keepLargePhoto) {
                            exifInterface = new android.media.ExifInterface(imgFile.getAbsolutePath());
                        }else{
                            exifInterface = new android.media.ExifInterface(
                                    imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName()
                            );
                        }
                    }
                    exifInterface.setAttribute(android.media.ExifInterface.TAG_MAKE, "dtp-");
                    exifInterface.setAttribute(android.media.ExifInterface.TAG_ORIENTATION,
                            String.valueOf(orientationAndroidExif));

                    exifInterface.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                bitmap2 = null;

                            /*if(Utils.overwritePhotos(this))
                            {
                                PhotoUtils.deletePhoto(this, s);
                            }*/
            }

            actual++;

            if (receiver != null) {
                Bundle bundle = new Bundle();
                bundle.putInt("progress", actual);

                receiver.send(Activity.RESULT_OK, bundle);
            }

            if (secondListener != null) {
                secondListener.onProgressChanged(actual);
            }

            if (showNotif)
                mNotificationUtils.setNotificationProgress(total, actual);
        }

        end(intent);
    }

    private String getExifDate(ExifInterface exif) {
        //De la forma: 2014:09:21 13:53:58
        String attribute = exif.getTagStringValue(ExifInterface.TAG_DATE_TIME);

        if (attribute != null) {
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

            Calendar cal = Calendar.getInstance();
            cal.set(year, month-1, day);

            return Utils.getFormattedDate(cal.getTime());
        }

        return getCurrentLocalizedDate();
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

    public ArrayList getCameraImages(Context context) {
        // Set up an array of the Thumbnail Image ID column we want
        String[] projection = {MediaStore.Images.Media.DATA};


        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);

        ArrayList<String> result = null;

        if (cursor != null) {
            result = new ArrayList<String>(cursor.getCount());


            if (cursor.moveToFirst()) {
                final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                do {
                    final String data = cursor.getString(dataColumn);
                    Log.i("data :", data);
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
        } else {
            final Cursor cursor1 = context.getContentResolver().query(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    null);

            if (cursor1 != null) {
                result = new ArrayList<String>(cursor1.getCount());


                if (cursor1.moveToFirst()) {
                    final int dataColumn = cursor1.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    do {
                        final String data = cursor1.getString(dataColumn);
                        Log.i("data :", data);
                        result.add(data);
                    } while (cursor1.moveToNext());
                }
                cursor1.close();
            } else {
                result = null;
            }
        }
        return result;
    }

    public void savePhoto(Bitmap bmp, String basePath, String name) {
        File imageFileFolder = new File(basePath);

        FileOutputStream out = null;
        File imageFileName = new File(imageFileFolder, name);
        try {
            out = new FileOutputStream(imageFileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            scanPhoto(imageFileName.toString());
            out = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void scanPhoto(final String imageFileName) {

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

    private String getStringOfNumber(int number) {
        String numberString = null;
        if (number < 10) {
            numberString = "0" + String.valueOf(number);
        } else {
            numberString = String.valueOf(number);
        }

        return numberString;
    }

    private void end(Intent intent) {
        Utils.write(Environment.getExternalStorageDirectory().getPath() + "/Download/datetophotoimages.txt", tempStr);

        if (showNotif)
            mNotificationUtils.endProgressNotification("El proceso ha finalizado");

        running = false;

        if (onBackground) {
            PowerConnectionReceiver.completeWakefulIntent(intent);
        }


        if (receiver != null) {
            Bundle bundle = new Bundle();
            bundle.putString("endShared", "endShared");

            receiver.send(Activity.RESULT_OK, bundle);
        }

        if (secondListener != null) {
            secondListener.reportEnd(!wasPathFound);
        }

        if (dialogCancelled)
            dialogCancelled = false;
    }

    public Bitmap decodeUri(Context c, Uri uri)
            throws FileNotFoundException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, options);

        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        //Hay que asegurarse de que nuestro bitmap no exceda el tamaño de maxMemory
        // (Runtime.getRuntime().maxMemory()), teniendo en cuenta que cada píxel ocupa 4 bytes
        final long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
        long freeMemory = maxMemory - usedMemory;

        long imageSize = (imageWidth * imageHeight * 4) / 1024; //Este valor era 12 unidades menos que el que debía ser
        wasLarge = false;

        if (imageSize >= freeMemory) {
            wasLarge = true;
        }

        while (imageSize >= freeMemory) {
            imageWidth /= 1.15;
            imageHeight /= 1.15;
            imageSize = (imageWidth * imageHeight * 4) / 1024;
        }

        long previousSize = (options.outWidth * options.outHeight * 4) / 1024;
        //double reduction = (double)previousSize / imageSize;
        int reduction = (int) Math.ceil((double) previousSize / imageSize);

        options = new BitmapFactory.Options();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            options.inMutable = true;

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        if (wasLarge)
            options.inSampleSize = reduction;

        return BitmapFactory.decodeStream(c.getContentResolver().openInputStream(uri), null, options);
    }

    private String getCurrentDate(String separator) {
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        return getStringOfNumber(calendar.get(Calendar.YEAR)) + separator
                + getStringOfNumber(calendar.get(Calendar.MONTH) + 1) + separator
                + getStringOfNumber(calendar.get(Calendar.DAY_OF_MONTH)) + separator +
                getStringOfNumber(calendar.get(Calendar.HOUR_OF_DAY)) + separator
                + getStringOfNumber(calendar.get(Calendar.MINUTE)) + separator
                + getStringOfNumber(calendar.get(Calendar.SECOND));
    }

    private String getCurrentLocalizedDate() {
        return Utils.getFormattedDate(new Date());
    }

    public Bitmap writeDateOnBitmap(Bitmap b, String text, int orientation) {

        Bitmap.Config bitmapConfig = b.getConfig();

        if(bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888;
        }

        //b = b.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(b);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.YELLOW);
        paint.setTextSize(Math.min(b.getWidth() / 20, b.getHeight() / 20));
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

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
            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
            {
                canvas.save();

                x = bounds.height(); //altotexto
                y = (int) (b.getHeight() - bounds.width() - marginHeight); //alto - anchotexto

                canvas.rotate(-270, x, y);

                canvas.drawText(text, x, y, paint);

                canvas.restore();
                break;
            }
            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
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
            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
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
            case android.media.ExifInterface.ORIENTATION_ROTATE_270: tempStr += "ORIENTATION_ROTATE_270"; break;
            case android.media.ExifInterface.ORIENTATION_TRANSVERSE: tempStr += "ORIENTATION_TRANSVERSE"; break;
            case android.media.ExifInterface.ORIENTATION_TRANSPOSE: tempStr += "ORIENTATION_TRANSPOSE"; break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_90: tempStr += "ORIENTATION_ROTATE_90"; break;
            case android.media.ExifInterface.ORIENTATION_UNDEFINED: tempStr += "ORIENTATION_UNDEFINED"; break;
            case android.media.ExifInterface.ORIENTATION_ROTATE_180: tempStr += "ORIENTATION_ROTATE_180"; break;
            case android.media.ExifInterface.ORIENTATION_NORMAL: tempStr += "ORIENTATION_NORMAL"; break;
            case android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL: tempStr += "ORIENTATION_FLIP_HORIZONTAL"; break;
            case android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL: tempStr += "ORIENTATION_FLIP_VERTICAL"; break;
        }

        tempStr += "\n";

        //Toast.makeText(this, String.valueOf(b.getHeight()), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Height: "+ String.valueOf(b.getHeight()) + " Width: " + String.valueOf(b.getWidth()), Toast.LENGTH_LONG).show();
        return b;
    }
}
