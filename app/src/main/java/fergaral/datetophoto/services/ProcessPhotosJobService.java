package fergaral.datetophoto.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.MyActivity;

/**
 * Created by Parejúa on 21/11/2014.
 */
public class ProcessPhotosJobService extends JobService {

    private static final int NOTIFICATION_ID = 1;
    private MediaScannerConnection msConn;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                showNotification("Procesando tus fotos en segundo plano...");

                ArrayList<String> galleryImages = getCameraImages(ProcessPhotosJobService.this);

                if (galleryImages != null) {
                    int total = 0, actual = 0;
                    total = galleryImages.size();


                    for (String s : galleryImages) {
                        File imgFile = new File(s);
                        if (imgFile.exists()) {


                            if (!imgFile.getName().contains("dtp-")) {


                                File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

                                if (!imgFileWithDate.exists()) {


                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    //options.inMutable = true;
                                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                                    myBitmap = convertToMutable(myBitmap);

                                    String date = "";

                                    try {
                                        ExifInterface exifInterface = new ExifInterface(imgFile.getAbsolutePath());
                                        date = getExifTag(exifInterface, ExifInterface.TAG_DATETIME, imgFile);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    Bitmap bitmap2 = writeDateOnBitmap(myBitmap, date);

                                    myBitmap = null;

                                    //CapturePhotoUtils.insertImage(getContentResolver(), bitmap2, imgFile.getName() + "-dtp.jpg", "generated using Date To Photo");
                                    savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtp-" + imgFile.getName());

                                    bitmap2 = null;

                                }
                            }
                        }

                        actual++;

                        imgFile = null;
                    }
                }
                showNotification("El proceso ha finalizado");
                jobFinished(jobParameters, true);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
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

            String year = attribute.substring(0, 4);
            String month = attribute.substring(5, 7);
            String day = attribute.substring(8, 10);

            return day + "/" + month + "/" + year;
        }

        Date lastModDate = new Date(imgFile.lastModified());
        Calendar cal = Calendar.getInstance();
        cal.setTime(lastModDate);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return day + "/" + month + "/" + year;
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

    public Bitmap writeDateOnBitmap(Bitmap b, String text) {

        float scale = getResources().getDisplayMetrics().density;
        Bitmap.Config bitmapConfig = b.getConfig();

        if(bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888;
        }

        //b = b.copy(bitmapConfig, true);


        Canvas canvas = new Canvas(b);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.YELLOW);
        paint.setTextSize(b.getHeight() / 20);
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int x = (b.getWidth() - bounds.width()) / 2;
        int y = (b.getHeight() + bounds.height()) / 2;

        x = b.getWidth() - bounds.width();
        y = b.getHeight() - bounds.height();

        canvas.drawText(text, x, y, paint);

        //Toast.makeText(this, String.valueOf(b.getHeight()), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Height: "+ String.valueOf(b.getHeight()) + " Width: " + String.valueOf(b.getWidth()), Toast.LENGTH_LONG).show();
        return b;
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

        if(cursor != null) {
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
                        Log.i("data :", data);
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

    public void savePhoto(Bitmap bmp, String basePath, String name)
    {
        File imageFileFolder = new File(basePath);
        FileOutputStream out = null;
        File imageFileName = new File(imageFileFolder, name);
        try
        {
            out = new FileOutputStream(imageFileName);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            scanPhoto(imageFileName.toString());
            out = null;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void scanPhoto(final String imageFileName)
    {

        msConn = new MediaScannerConnection(ProcessPhotosJobService.this,new MediaScannerConnection.MediaScannerConnectionClient()
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
        msConn.connect();
    }
}