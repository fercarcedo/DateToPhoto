package fergaral.datetophoto.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import fergaral.datetophoto.R;

/**
 * Created by fer on 24/05/15.
 */
public class TickedImageView extends ImageView {

    private boolean selected;
    private Bitmap mTickBmp;
    private Paint mDarkerPaint;
    private View.OnClickListener onImageClickListener;
    private int drawingWidth;
    private float horizontalSpacing;
    private int x, y;

    public TickedImageView(Context context) {
        super(context);
        init();
    }

    public TickedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelected(!isSelected());

                if (onImageClickListener != null)
                    onImageClickListener.onClick(TickedImageView.this);
            }
        });

        mDarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDarkerPaint.setStyle(Paint.Style.FILL);
        mDarkerPaint.setColor(0x80142030);

        mTickBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_done_white_48px);

        horizontalSpacing = Utils.dpToPixels(2, getResources());
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (selected) {
            canvas.drawRect(0, 0, canvas.getWidth(), drawingWidth, mDarkerPaint);
            canvas.drawBitmap(mTickBmp, x, y, null);
        }
    }

    public void setOnImageClickListener(View.OnClickListener listener) {
        onImageClickListener = listener;
    }

    /**
     * Sets the drawingWidth in pixels
     *
     * @param drawingWidth width in pixels
     */
    public void setDrawingWidth(int drawingWidth) {
        this.drawingWidth = drawingWidth;
        getLayoutParams().width = drawingWidth;
        getLayoutParams().height = drawingWidth;
        x = ((drawingWidth / 2) - (mTickBmp.getWidth() / 2));
        y = (drawingWidth / 2) - (mTickBmp.getHeight() / 2);
    }

    public float getDrawingWidth() {
        return drawingWidth;
    }
}
