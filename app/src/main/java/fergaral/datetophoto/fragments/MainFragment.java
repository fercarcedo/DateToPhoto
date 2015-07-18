package fergaral.datetophoto.fragments;

import android.app.ActivityOptions;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import fergaral.datetophoto.R;
import fergaral.datetophoto.activities.MyActivity;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.utils.MyResultReceiver;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by Parejúa on 08/07/2014.
 */
public class MainFragment extends Fragment implements ProgressChangedListener {

    private static final int NOTIFICATION_ID = 1;
    public static final String INTENT_ACTION = "fergaral.datetophoto.CANCEL_DIALOG_ACTION";

    public static final int READ_REQUEST_CODE = 1;
    static final int REQUEST_IMAGE_CAPTURE = 2;
    private Uri[] uris;
    private Button btn1, btn2, btn3, btn4, btn5;
    private ImageView iv1;
    //    private PhotosObserver instUploadObserver = new PhotosObserver();
    private String saved;
    private MaterialDialog dialog;
    private MediaScannerConnection msConn;
    private boolean dialogCancelled, actionCancel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        dialog = new MaterialDialog.Builder(getActivity())
                .content("Procesando fotos...")
                .title("Progreso")
                .build();

        //dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //dialog.setCancelable(false);
        /*dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Lo dejamos en blanco para evitar ambigüedad si pasamos null, ya lo sobreescribimos luego
                //con su cuerpo
            }
        });*/

        //dialog.setActionButton(DialogAction.NEGATIVE, "Cancelar");

        /*dialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.setTitle("Cancelando");
                dialog.setContent("Cancelando... Un segundo, por favor...");

                dialogCancelled = true;
            }
        });*7

        /*dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterf) {
                Button dialogButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                dialogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.setTitle("Cancelando");
                        dialog.setMessage("Cancelando... Un segundo, por favor...");

                        dialogCancelled = true;
                    }
                });
            }
        });*/

       /*btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //showNotification();
                performFileSearch();
            }
        });*/

        //iv1 = (ImageView) findViewById(R.id.iv1);
  /*      this.getApplicationContext()
                .getContentResolver()
                .registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false,
                        instUploadObserver);*/


        /*btn2 = (Button) findViewById(R.id.button);
        btn2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startCamera();
            }
        });*/

        btn3 = (Button) rootView.findViewById(R.id.btn3);
        btn3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                /*ArrayList<String> cameraImages = getCameraImages(MyActivity.this);
                ProcessImagesTask processImagesTask = new ProcessImagesTask();
                processImagesTask.execute(cameraImages);*/

                Utils.startProcessPhotosService(getActivity(), MainFragment.this, null);
            }
        });

        //GridView gridView1 = (GridView) findViewById(R.id.gridview1);
        //gridView1.setAdapter(new ImageAdapter(this));

        //tv1 = (TextView) findViewById(R.id.tv1);
        //tv2 = (TextView) findViewById(R.id.tv2);

        //getCameraImagesWithoutDate();

        //btn5 = (Button) findViewById(R.id.btn5);

        /*btn5.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                showNotification();
            }
        });

        checkChargingStatus(tv2);*/


        /*JobScheduler jobScheduler = JobScheduler.getInstance(this);

        JobInfo jobInfo = new JobInfo.Builder(0, new ComponentName(this, ProcessPhotosJobService.class))
                .setRequiresCharging(true)
                .build();
        jobScheduler.schedule(jobInfo);*/
        //final TextView tvdiskspace1 = (TextView) findViewById(R.id.tvdiskspace1);
        //final TextView tvdiskspace2 = (TextView) findViewById(R.id.tvdiskspace2);
        /*Button buttonShowDiskSpaceUsage = (Button) findViewById(R.id.btnshowdiskspaceusage);
        buttonShowDiskSpaceUsage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int[] diskSpaceUsed = getDiskSpaceUsage();
                tvdiskspace1.setText("Photos with date: " + String.valueOf(diskSpaceUsed[0]) + "MB");
                tvdiskspace2.setText("Photos without date: " + String.valueOf(diskSpaceUsed[1]) + "MB");
            }
        });*/

        /*Button commandbtn = (Button) findViewById(R.id.commandbtn);
        commandbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeSpecifiedCommand();
            }
        });*/

        /*Button btn4 = (Button) findViewById(R.id.btndeletephotoswithdate);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DeleteImagesTask().execute(getCameraImages(MyActivity.this));
            }
        });*/

        //a();
        //b();

        //Vamos a enviarle un mensaje al servicio para que registre nuestro listener

        if(!dialog.isShowing()) {
            MyResultReceiver receiver = new MyResultReceiver(new Handler());
            receiver.setReceiver(MainFragment.this);
            Intent broadcastIntent = new Intent(MyActivity.INTENT_QUERY_ACTION);
            broadcastIntent.putExtra("receiver", receiver);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
        }

        Intent intent = getActivity().getIntent();

        //Verificamos si el Intent recibido procede del botón compartir
        if(Intent.ACTION_SEND.equals(intent.getAction())) {
            handleSingleImage(intent);
        }else if(Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            handleMultipleImages(intent);
        }

        actionCancel = intent.hasExtra("fromnotifaction");

        return rootView;
    }

    public void showNotification() {

        int notificationId = 1;

        Intent intent = new Intent(getActivity(), MyActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getActivity());
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Título de la notificación")
                .setContentText("Esto es una prueba de notificación")
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    public void showNotification(String text) {
        Intent resultIntent = new Intent(getActivity(), MyActivity.class);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                getActivity(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(getActivity())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Date To Photo")
                .setContentText(text)
                .setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notifBuilder.build());
    }

    /*public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }*/

   /* @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if(data == null) {
                return;
            }

            if(data.getData() != null) {
                try {
                    Bitmap b = getBitmapFromUri(data.getData());
                    Bitmap b2 = writeDateOnBitmap(b,  "Texto");
                    iv1.setImageBitmap(b2);

                    CapturePhotoUtils.insertImage(getContentResolver(), b2, "20140812-dtp-1.jpg", "generated using Date To Photo");

                }catch(IOException e) {}
            }else {

                //Handle multiple documents
                ClipData clipData = data.getClipData();

                if(clipData != null) {
                    uris = new Uri[clipData.getItemCount()];
                    for(int i = 0; i < clipData.getItemCount(); i++) {
                        uris[i] = clipData.getItemAt(i).getUri();
                    }

                    try {
                        Bitmap b = getBitmapFromUri(uris[0]);
                        iv1.setImageBitmap(b);
                    }catch(IOException e) {}
                }
            }


        }


        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            iv1.setImageBitmap(imageBitmap);
        }
    }*/

    /*public void getMetaData(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null, null);

        if(cursor != null && cursor.moveToFirst()) {
            String displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            String size = null;

            if(!cursor.isNull(sizeIndex)) {
                size = cursor.getString(sizeIndex);
            }else {
                size = "Unknown";
            }
        }

        cursor.close();
    }*/

    /*private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");

        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);

        parcelFileDescriptor.close();

        return image;
    }*/

    /*public Bitmap writeDateOnBitmap(Bitmap b, String text, int orientation) {

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

        switch(orientation)
        {
            case ExifInterface.ORIENTATION_ROTATE_270:
            {
                canvas.save();
                canvas.rotate(-270, x, y);

                canvas.drawText(text, x, y, paint);

                canvas.restore();
                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_90:
            {
                canvas.save();


                x = b.getWidth() - bounds.height(); //ancho - altotexto
                y = bounds.width(); //anchotexto

                canvas.rotate(-90, x, y);


                canvas.drawText(text, x, y, paint);

                canvas.restore();
                break;
            }
            case ExifInterface.ORIENTATION_ROTATE_180:
            {
                canvas.save();

                x = 0;
                y = 0;

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
        //Toast.makeText(this, String.valueOf(b.getHeight()), Toast.LENGTH_LONG).show();
        //Toast.makeText(this, "Height: "+ String.valueOf(b.getHeight()) + " Width: " + String.valueOf(b.getWidth()), Toast.LENGTH_LONG).show();
        return b;
    }*/

    /*private class PhotosObserver extends ContentObserver {

        public PhotosObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Media media = readFromMediaStore(getApplicationContext(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            saved = "I detected " + media.file.getName();
            Log.d("INSTANT", "detected picture");
        }
    }*/

    /*private Media readFromMediaStore(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null,
                null, "date_added DESC");
        Media media = null;
        if (cursor.moveToNext()) {
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            String filePath = cursor.getString(dataColumn);
            int mimeTypeColumn = cursor
                    .getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            String mimeType = cursor.getString(mimeTypeColumn);
            media = new Media(new File(filePath), mimeType);
        }
        cursor.close();
        return media;
    }*/

    /*private class Media {
        private File file;
        @SuppressWarnings("unused")
        private String type;

        public Media(File file, String type) {
            this.file = file;
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public File getFile() {
            return file;
        }
    }*/

    /*@Override
    public void onResume() {
        super.onResume();
        if (saved != null) {
            btn1.setText(saved);
        }
    }*/

    /*@Override
    public void onDestroy() {
        super.onDestroy();
        this.getApplicationContext().getContentResolver()
                .unregisterContentObserver(instUploadObserver);
        Log.d("INSTANT", "unregistered content observer");
    }*/

    /*public void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 1);
        }
    }*/

    @Override
    public void reportTotal(int total) {
        dialog = new MaterialDialog.Builder(getActivity())
                .title("Progreso")
                .content("Procesando fotos...")
                .progress(false, total, true)
                .build();
        dialog.setActionButton(DialogAction.NEGATIVE, "Cancelar");
        dialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.setTitle("Cancelando");
                dialog.setContent("Cancelando... Un segundo, por favor...");

                dialogCancelled = true;
            }
        });

        if(actionCancel) {
            dialog.setTitle("Cancelando");
            dialog.setContent("Cancelando... Un segundo, por favor...");

            dialogCancelled = true;
            actionCancel = false;
        }

        dialog.show();
    }

    @Override
    public void onProgressChanged(int progress) {
        dialog.incrementProgress(Math.round(progress) - dialog.getCurrentProgress());

        if (dialogCancelled)
        {
            cancelService();
            dialogCancelled = false;
        }
    }

    @Override
    public void reportEnd(boolean fromActionShare) {
        dialog.dismiss();
        dialog.setContent("Procesando fotos...");
        dialog.setTitle("Progreso");
    }

    private void cancelService()
    {
        Intent broadcastIntent = new Intent(INTENT_ACTION);
        broadcastIntent.putExtra("dialogcancelled", true);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(broadcastIntent);
    }
    /*public class ProcessImagesTask extends AsyncTask<ArrayList, Float, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.setProgress(0);
            dialog.setMax(100);
            dialog.show();
        }

        @Override
        protected Void doInBackground(ArrayList... arrayLists) {

            ArrayList<String> galleryImages = arrayLists[0];

            if(galleryImages != null) {
                int total = 0, actual = 0;
                total = galleryImages.size();

                dialog.setMax(total);

                for (String s : galleryImages) {
                    File imgFile = new File(s);
                    if (imgFile.exists()) {


                        if (!imgFile.getName().contains("dtp-")) {

                            if (imgFile.getName().endsWith(".jpg") || imgFile.getName().endsWith(".jpeg") || imgFile.getName().endsWith(".png")) {
                                File imgFileWithDate = new File(imgFile.getParentFile().getAbsolutePath() + "/dtp-" + imgFile.getName());

                                if (!imgFileWithDate.exists()) {

                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    //options.inMutable = true;
                                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                                    myBitmap = convertToMutable(myBitmap);

                                    myBitmap = rotateBitmap(myBitmap, imgFile);
                                    //String date = "";
                                    //Bitmap correctlyRotatedBitmap = null;

                                    /*try {
                                        ExifInterface exifInterface = new ExifInterface(imgFile.getAbsolutePath());
                                        date = getExifTag(exifInterface, ExifInterface.TAG_DATETIME, imgFile);
                                        int rotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                                        Matrix matrix = new Matrix();
                                        switch (rotation) {
                                            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                                                matrix.setScale(-1, 1);
                                                break;

                                            case ExifInterface.ORIENTATION_ROTATE_180:
                                                matrix.setRotate(180);
                                                break;

                                            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                                                matrix.setRotate(180);
                                                matrix.postScale(-1, 1);
                                                break;

                                        case ExifInterface.ORIENTATION_TRANSPOSE:
                                            matrix.setRotate(90);
                                            matrix.postScale(-1, 1);
                                            break;

                                        case ExifInterface.ORIENTATION_ROTATE_90:
                                            matrix.setRotate(90);
                                            break;

                                        case ExifInterface.ORIENTATION_TRANSVERSE:
                                            matrix.setRotate(-90);
                                            matrix.postScale(-1, 1);
                                            break;

                                        case ExifInterface.ORIENTATION_ROTATE_270:
                                            matrix.setRotate(-90);
                                            break;

                                        case ExifInterface.ORIENTATION_NORMAL:
                                        default:
                                            break;
                                    }

                                    int height = myBitmap.getHeight();
                                    int width = myBitmap.getWidth();
                                    correctlyRotatedBitmap = Bitmap.createBitmap(myBitmap, 0, 0, width, height, matrix, true);
                                    //myBitmap.recycle();
                                    myBitmap = null;
                                    matrix = null;
                                    exifInterface = null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }*/

                                /*myBitmap = null;
                                Bitmap bitmap2 = writeDateOnBitmap(correctlyRotatedBitmap, date);
                                //myBitmap = null;
                                correctlyRotatedBitmap = null;*/
                                /*Bitmap bitmap2 = null;
                                if(correctlyRotatedBitmap != null) {
                                    myBitmap.recycle();
                                    myBitmap = null;
                                    bitmap2 = writeDateOnBitmap(correctlyRotatedBitmap, date);
                                    correctlyRotatedBitmap.recycle();
                                    correctlyRotatedBitmap = null;
                                }
                                else
                                {
                                    bitmap2 = writeDateOnBitmap(myBitmap, date);
                                    myBitmap.recycle();
                                    myBitmap = null;
                                }*/

    //myBitmap = null;
    //Bitmap bitmap2 = writeDateOnBitmap(correctlyRotatedBitmap, date);
    //correctlyRotatedBitmap = null;


    //CapturePhotoUtils.insertImage(getContentResolver(), bitmap2, imgFile.getName() + "-dtp.jpg", "generated using Date To Photo");
    //savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtp-" + imgFile.getName(), imgFile);


    //bitmap2 = null;

                                /*String date = "";
                                int rotation = ExifInterface.ORIENTATION_NORMAL;

                                try {
                                    ExifInterface exifInterface = new ExifInterface(imgFile.getAbsolutePath());
                                    date = getExifTag(exifInterface, ExifInterface.TAG_DATETIME, imgFile);
                                    rotation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                //myBitmap = getCorrectlyRotatedBitmap(myBitmap, imgFile);
                                Bitmap bitmap2 = writeDateOnBitmap(myBitmap, date, rotation);
                                myBitmap = null;

                                savePhoto(bitmap2, imgFile.getParentFile().getAbsolutePath(), "dtp-" + imgFile.getName(), imgFile, true);
                                bitmap2 = null;
                            }
                        }
                        }
                    }

                    actual++;
                    publishProgress((float)actual);

                    imgFile = null;
                }

                return null;
            }
            else
            {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MyActivity.this);
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

                return null;
            }
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            super.onProgressUpdate(values);
            //int p = Math.round(100 * values[0]);
            dialog.setProgress(Math.round(values[0]));
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
        }
    }*/

    private void startActivityCompat(Intent intent)
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            getActivity().startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
        }
        else
        {
            startActivity(intent);
        }
    }

    private void handleSingleImage(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        ArrayList<String> selectedPaths = new ArrayList<>();

        selectedPaths.add(uri.toString());

        Utils.startProcessPhotosURIService(getActivity(), MainFragment.this, selectedPaths);
    }

    private void handleMultipleImages(Intent intent) {
        ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

        ArrayList<String> selectedPaths = new ArrayList<>();

        for(Uri uri : uris) {
            selectedPaths.add(uri.toString());
        }

        Utils.startProcessPhotosURIService(getActivity(), MainFragment.this, selectedPaths);
    }

    public void savePhoto2(Bitmap bmp, String path) {
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(path);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }catch(Exception e) {
            e.printStackTrace();
        }finally{
            try {
                if(out != null)
                    out.close();
            }catch(IOException e) {
                e.printStackTrace();
            }
        }
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
                getActivity().getApplicationContext(),
                new String[]{imageFileName},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
    }
}
