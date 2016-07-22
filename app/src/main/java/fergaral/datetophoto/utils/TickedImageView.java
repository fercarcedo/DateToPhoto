package fergaral.datetophoto.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import fergaral.datetophoto.R;

/**
 * Created by fer on 24/05/15.
 */
public class TickedImageView extends ImageView {

    private boolean checked;
    private Bitmap mTickBmp;
    private Paint mDarkerPaint;
    private View.OnClickListener onImageClickListener;
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
        super.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setChecked(!isChecked());

                if (onImageClickListener != null)
                    onImageClickListener.onClick(TickedImageView.this);
            }
        });

        mDarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDarkerPaint.setStyle(Paint.Style.FILL);
        mDarkerPaint.setColor(0x80142030);

        mTickBmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_done_white_48px);
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        invalidate();
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (checked) {
            canvas.drawRect(0, 0, canvas.getWidth(), getMeasuredWidth(), mDarkerPaint);
            canvas.drawBitmap(mTickBmp, x, y, null);
        }
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        onImageClickListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);

        x = (getMeasuredWidth() / 2) - (mTickBmp.getWidth() / 2);
        y = (getMeasuredWidth() / 2) - (mTickBmp.getHeight() / 2);
    }
}
