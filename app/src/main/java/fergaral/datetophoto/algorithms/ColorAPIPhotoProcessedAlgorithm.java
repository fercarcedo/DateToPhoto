package fergaral.datetophoto.algorithms;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import fergaral.datetophoto.utils.Utils;

/**
 * Created by Fer on 18/10/2017.
 */

public class ColorAPIPhotoProcessedAlgorithm implements PhotoProcessedAlgorithm {
    @Override
    public Result isProcessed(String photoPath) {
        Bitmap myBitmap = BitmapFactory.decodeFile(new File(photoPath).getAbsolutePath());
        if (myBitmap == null) return Result.ERROR;

        int upperLeft = myBitmap.getPixel(0, 0);
        int upperRight = myBitmap.getPixel(myBitmap.getWidth() - 1, 0);
        int lowerLeft = myBitmap.getPixel(0, myBitmap.getHeight() - 1);
        int lowerRight = myBitmap.getPixel(myBitmap.getWidth() - 1, myBitmap.getHeight() - 1);

        return (Utils.getColor(upperLeft) == Color.BLUE && Utils.getColor(upperRight) == Color.RED
                && Utils.getColor(lowerLeft) == Color.GREEN && Utils.getColor(lowerRight) == Color.YELLOW)
                    ? Result.PROCESSED
                    : Result.NOT_PROCESSED;
    }
}
