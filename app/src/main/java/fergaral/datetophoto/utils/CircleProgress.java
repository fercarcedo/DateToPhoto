package fergaral.datetophoto.utils;

import android.content.Context;
import android.util.AttributeSet;

import com.github.lzyzsd.circleprogress.DonutProgress;

/**
 * Created by fer on 21/09/15.
 */
public class CircleProgress extends DonutProgress {
    public CircleProgress(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, heightMeasureSpec);
    }
}
