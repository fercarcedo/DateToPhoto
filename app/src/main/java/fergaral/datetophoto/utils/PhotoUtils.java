package fergaral.datetophoto.utils;

import android.content.Context;
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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.Toast;

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
import java.util.List;
import java.util.Map;

import fergaral.datetophoto.R;

/**
 * Created by Parejúa on 02/11/2014.
 */
public final class PhotoUtils {

    private Context context;
    private MediaScannerConnection msConn;

    /**
     * Constructor with 1 parameter
     * @param context application context
     */
    public PhotoUtils(Context context) {
        this.context = context;
    }

    /**
     * Method that returns an ArrayList with the photos of the gallery
     * @return ArrayList<String> photos from gallery
     */
    public ArrayList<String> getCameraImages() {
        // Set up an array of the Thumbnail Image ID column we want
        String[] projection = {MediaStore.Images.Media.DATA};

        final Cursor cursorExternal = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);

        final Cursor cursorInternal = context.getContentResolver().query(MediaStore.Images.Media.INTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);

        ArrayList<String> result;

        if(cursorExternal != null && cursorInternal == null)
            result = new ArrayList<String>(cursorExternal.getCount());
        else if(cursorExternal != null)
            result = new ArrayList<String>(cursorExternal.getCount() + cursorInternal.getCount());
        else if(cursorInternal != null)
            result = new ArrayList<String>(cursorInternal.getCount());
        else
            result = new ArrayList<String>();

        processPhotosCursor(cursorExternal, result);
        processPhotosCursor(cursorInternal, result);

        return result;
    }

    public List<Thumbnail> getCameraThumbnails() {
        String[] projection = {MediaStore.Images.Thumbnails.DATA, MediaStore.Images.Thumbnails.IMAGE_ID,
                                MediaStore.Images.Thumbnails._ID};

        Cursor cursorExternal = context.getContentResolver().query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);

        Cursor cursorInternal = context.getContentResolver().query(MediaStore.Images.Thumbnails.INTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null);

        ArrayList<Thumbnail> cameraThumbnails;

        if(cursorExternal != null && cursorInternal == null)
            cameraThumbnails = new ArrayList<>(cursorExternal.getCount());
        else if(cursorExternal != null)
            cameraThumbnails = new ArrayList<>(cursorExternal.getCount() + cursorInternal.getCount());
        else if(cursorInternal != null)
            cameraThumbnails = new ArrayList<>(cursorInternal.getCount());
        else
            cameraThumbnails = new ArrayList<>();

        processPhotosThumbnailPhotoCursor(cursorExternal, cameraThumbnails);
        processPhotosThumbnailPhotoCursor(cursorInternal, cameraThumbnails);

        return cameraThumbnails;
    }

    private void processPhotosThumbnailPhotoCursor(Cursor cursor, List<Thumbnail> thumbnails) {
        if(cursor == null || thumbnails == null)
            return;

        if(cursor.moveToFirst()) {
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA);
            int imageIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID);
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails._ID);

            do {
                String thumbPath = cursor.getString(dataColumn);
                int imageId = cursor.getInt(imageIdColumn);
                Uri imageURI = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        Integer.toString(cursor.getInt(idColumn)));

                thumbnails.add(new Thumbnail(String.valueOf(imageId), thumbPath, imageURI));
            }while(cursor.moveToNext());
        }

        cursor.close();
    }

    private void processPhotosThumbnailCursor(Cursor cursor, List<String> photosUriList) {
        processImagesCursor(cursor, photosUriList, MediaStore.Images.Thumbnails.DATA);
    }

    private void processPhotosCursor(Cursor cursor, List<String> photosUriList)
    {
        processImagesCursor(cursor, photosUriList, MediaStore.Images.Media.DATA);
    }

    private void processImagesCursor(Cursor cursor, List<String> photosUriList, String imageDataColumn) {
        if(photosUriList == null || cursor == null)
            return;

        if (cursor.moveToFirst()) {
            final int dataColumn = cursor.getColumnIndexOrThrow(imageDataColumn);
            do {
                final String data = cursor.getString(dataColumn);
                //Log.i("data :", data);
                photosUriList.add(data);
            } while (cursor.moveToNext());
        }

        cursor.close();
    }

    /**
     * Method that puts date to the photos of the gallery
     * @param cameraImages ArrayList with the photos of the gallery
     */
    public void processPhotos(ArrayList<String> cameraImages)
    {

        ArrayList<String> galleryImages = cameraImages;

        if(galleryImages != null) {
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
        else
        {
            /*AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MyActivity.this);
            alertDialogBuilder.setTitle("No hay fotos");
            alertDialogBuilder.setMessage("No se han encontrado fotos en la memoria de tu dispositivo");
            alertDialogBuilder.setCancelable(true);
            alertDialogBuilder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            alertDialogBuilder.show();

            return null;*/
        }
    }

    /**
     * Method that returns the date associated with a photo, which can be EXIF, or if it's not present, the file date
     * @param exif ExifInterface object
     * @param tag EXIF tag
     * @param imgFile File (used to get file date if EXIF date is not present)
     * @return String date of the photo
     */
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

    /**
     * Method that converts an immutable Bitmap to a mutable one
     * @param imgIn immutable Bitmap
     * @return Bitmap the bitmap passed in, but mutable
     */
    private Bitmap convertToMutable(Bitmap imgIn) {
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

    /**
     * Method that puts date on a Bitmap
     * @param b Bitmap without date
     * @param text Date as a string
     * @return Bitmap with date
     */
    public Bitmap writeDateOnBitmap(Bitmap b, String text) {

        float scale = context.getResources().getDisplayMetrics().density;
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

    /**
     * Method that saves the Bitmap passed in to the device's gallery
     * @param bmp
     * @param basePath
     * @param name
     */
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

        msConn = new MediaScannerConnection(context,new MediaScannerConnection.MediaScannerConnectionClient()
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

    public static ArrayList<String> getFolders(Context ctx)
    {
        ArrayList<String> cameraImages = new PhotoUtils(ctx).getCameraImages();
        ArrayList<String> folderNames = new ArrayList<String>();

        for(String path : cameraImages)
        {
            String parentFolderName = getParentFolderName(path);

            if(!folderNames.contains(parentFolderName))
            {
                folderNames.add(parentFolderName);
            }
        }

        return folderNames;
    }


    public static String getParentFolderName(String path)
    {
        String[] pathSegments = path.split("/");

        return pathSegments[pathSegments.length - 2];
    }


    public static void deletePhoto(Context context, String s)
    {
        File file = new File(s);
        String filePath = file.getAbsolutePath();

        file.delete();
        MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);
    }

    public static void selectAllFolders(Context context) {
        ArrayList<String> folderNames = PhotoUtils.getFolders(context);
        StringBuilder stringBuilder = new StringBuilder();

        for(String folderName : folderNames) {
            stringBuilder.append(folderName).append(FoldersListPreference.SEPARATOR);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_folderstoprocess_key), stringBuilder.toString());
        editor.apply();
    }

    public static void selectAllFoldersOnFirstUse(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(prefs.getBoolean("firstuse", true))
            selectAllFolders(context);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("firstuse", false);
        editor.apply();
    }
}
