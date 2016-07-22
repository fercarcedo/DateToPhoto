package fergaral.datetophoto.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import fergaral.datetophoto.R;
import fergaral.datetophoto.utils.Point;
import fergaral.datetophoto.utils.RectImageView;

public class DetectDateActivity extends AppCompatActivity {

    private static final int SELECT_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_date);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button selectPhotoBtn = (Button) findViewById(R.id.select_photo_btn);

        if(selectPhotoBtn != null)
            selectPhotoBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);

                    startActivityForResult(Intent.createChooser(intent, "Select photo"), SELECT_IMAGE_REQUEST);
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == SELECT_IMAGE_REQUEST && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            Uri uri = data.getData();
            PrintWriter printWriter = null;

            try {
                long startTimeDecodeBitmap = System.currentTimeMillis();

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                long endTimeDecodeBitmap = System.currentTimeMillis();

                long startTimeRecognition = System.currentTimeMillis();

                TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

                if(textRecognizer.isOperational()) {
                    //Toast.makeText(this, "Text recognizer operational", Toast.LENGTH_SHORT).show();

                    Frame frame = new Frame.Builder()
                            .setBitmap(bitmap)
                            .build();

                    SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

                    long endTimeRecognition = System.currentTimeMillis();

                    File downloadFile = new File(Environment.getExternalStorageDirectory().getPath()
                            + File.separator + "Download");

                    if(!downloadFile.exists())
                        downloadFile.mkdirs();

                    printWriter = new PrintWriter(new FileWriter(
                            downloadFile.getAbsolutePath() + File.separator + "dtpdetections.txt"
                    ));

                    RectImageView selectedPhotoIv = (RectImageView) findViewById(R.id.selected_photo_iv);

                    if(selectedPhotoIv != null)
                        selectedPhotoIv.setImageBitmap(bitmap);

                    for(int i = 0; i < textBlocks.size(); i++) {
                        Rect boundingBox = textBlocks.valueAt(i).getBoundingBox();

                        if(selectedPhotoIv != null)
                            selectedPhotoIv.addBoundingBox(boundingBox);

                        printWriter.println("Top left: (" + boundingBox.left + "," + boundingBox.top + ")");
                        printWriter.println("Top right: " + boundingBox.right + "," + boundingBox.top + ")");
                        printWriter.println("Bottom left: " + boundingBox.left + "," + boundingBox.bottom + ")");
                        printWriter.println("Bottom right: " + boundingBox.right + "," + boundingBox.bottom + ")");
                        printWriter.print("Image dimensions: ");
                        printWriter.print(bitmap.getWidth());
                        printWriter.print(" x ");
                        printWriter.println(bitmap.getHeight());

                        Point bottomRightPoint = new Point(boundingBox.right, boundingBox.bottom);

                        if(bottomRightPoint.isInBottomRightCorner(bitmap.getWidth(), bitmap.getHeight()))
                            printWriter.println("Datestamped");
                        else
                            printWriter.println("Not datestamped");

                        printWriter.print("Decode bitmap time: ");
                        printWriter.println((endTimeDecodeBitmap - startTimeDecodeBitmap));
                        printWriter.print("Recognition time: ");
                        printWriter.println((endTimeRecognition - startTimeRecognition));
                        printWriter.println();
                    }
                }else{
                    Toast.makeText(this, "Text recognizer not operational", Toast.LENGTH_SHORT).show();
                }
            }catch(IOException e) {
                e.printStackTrace();
            }finally{
                if(printWriter != null)
                    printWriter.close();
            }
        }
    }
}
