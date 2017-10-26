package fergaral.datetophoto.algorithms;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;

import fergaral.datetophoto.DateToPhoto;
import fergaral.datetophoto.utils.Point;

/**
 * Created by Fer on 18/10/2017.
 */

public class VisionAPIPhotoProcessedAlgorithm implements PhotoProcessedAlgorithm {
    @Override
    public Result isProcessed(String photoPath) {
        Context context = DateToPhoto.getInstance();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap imageBitmap = BitmapFactory.decodeFile(new File(photoPath).getAbsolutePath(), options);
        if (imageBitmap == null) return Result.ERROR;
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

        return datestamped ? Result.PROCESSED : Result.NOT_PROCESSED;
    }
}
