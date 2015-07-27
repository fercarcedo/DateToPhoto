package fergaral.datetophoto.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.github.lzyzsd.circleprogress.DonutProgress;

/**
 * Created by fer on 24/07/15.
 */
public class ProgressCircle extends DonutProgress {
    private int total;

    public ProgressCircle(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTotal(int total) {
        this.total = total;
    }

    @Override
    public void setProgress(int progress) {
        if(total != 0) {
            double progDouble = (double) progress;
            int prog = (int) ((progDouble / total) * 100);
            super.setProgress(prog);
            Log.d("TAG", String.valueOf(prog));
        }
    }
}
