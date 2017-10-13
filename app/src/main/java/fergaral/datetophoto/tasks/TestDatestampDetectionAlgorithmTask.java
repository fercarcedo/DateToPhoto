package fergaral.datetophoto.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import fergaral.datetophoto.utils.Point;

/**
 * Created by Fer on 13/10/2017.
 */


public class TestDatestampDetectionAlgorithmTask extends AsyncTask<Void, Integer, Float> {

    public interface AlgorithmCallback {
        void onProgressChanged(int progress);
        void onCompleted(float percentage);
    }

    private AlgorithmCallback listener;
    private List<String> imagesToProcess;
    private WeakReference<Context> contextRef;

    public TestDatestampDetectionAlgorithmTask(AlgorithmCallback listener,
                                               List<String> imagesToProcess,
                                               Context context) {
        this.listener = listener;
        this.imagesToProcess = imagesToProcess;
        this.contextRef = new WeakReference<>(context);
    }

    @Override
    protected Float doInBackground(Void... params) {
        Context context = contextRef.get();
        if (context == null) return null;
        else context = context.getApplicationContext();

        int numCorrect = 0;
        int totalImages = 0;

        for (int i = 0; i < imagesToProcess.size(); i++) {
            String path = imagesToProcess.get(i);
            Bitmap imageBitmap = BitmapFactory.decodeFile(new File(path).getAbsolutePath());

            if (imageBitmap == null) continue;

            totalImages++;
            if (datestampDetected(context, imageBitmap)) {
                if (exifFieldFound(path)) {
                    numCorrect++;
                }
            } else {
                if (!exifFieldFound(path)) {
                    numCorrect++;
                }
            }

            publishProgress(i + 1);
        }
        return (numCorrect / ((float)totalImages)) * 100;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        listener.onProgressChanged(values[0]);
    }

    @Override
    protected void onPostExecute(Float percentage) {
        listener.onCompleted(percentage);
    }

    private boolean datestampDetected(Context context, Bitmap imageBitmap) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        boolean datestamped = false;

        if (textRecognizer.isOperational()) {
            Frame frame = new Frame.Builder()
                    .setBitmap(imageBitmap)
                    .build();

            SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

            for(int i = 0; i < textBlocks.size(); i++) {
                Rect boundingBox = textBlocks.valueAt(i).getBoundingBox();
                Point bottomRightPoint = new Point(boundingBox.right, boundingBox.bottom);
                if(bottomRightPoint.isInBottomRightCorner(imageBitmap.getWidth(), imageBitmap.getHeight())) {
                    datestamped = true;
                    break;
                }
            }
        }

        return datestamped;
    }

    private boolean exifFieldFound(String imagePath) {
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            String makeExif = exifInterface.getAttribute(ExifInterface.TAG_MAKE);
            return makeExif != null && makeExif.startsWith("dtp-");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}