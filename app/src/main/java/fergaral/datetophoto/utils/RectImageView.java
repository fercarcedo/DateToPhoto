package fergaral.datetophoto.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fer on 21/07/16.
 */
public class RectImageView extends ImageView {
    private final List<Rect> boundingBoxes;
    private final Paint paint;

    public RectImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boundingBoxes = new ArrayList<>();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.YELLOW);
    }

    public void addBoundingBox(Rect boundingBox) {
        if(boundingBox != null)
            boundingBoxes.add(boundingBox);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); //Draw the image

        for(Rect boundingBox : boundingBoxes)
            canvas.drawRect(boundingBox, paint);
    }
}
