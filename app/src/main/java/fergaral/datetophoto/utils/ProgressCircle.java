package fergaral.datetophoto.utils;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.util.Timer;
import java.util.TimerTask;

import fergaral.datetophoto.R;

/**
 * Created by fer on 24/07/15.
 */
public class ProgressCircle extends LinearLayout {

    private DonutProgress donutProgress;
    private TextView progTv;
    private int total;
    private int actual;

    public ProgressCircle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.circle_prog_layout, this);
        donutProgress = (DonutProgress) view.findViewById(R.id.progress_circle);
        progTv = (TextView) view.findViewById(R.id.progress_circle_progtv);
    }

    public void setTotal(int total) {
        this.total = total;
        progTv.setText(0 + "/" + total);
    }

    public void setActual(final Activity activity, final int actual) {
        if(total != 0) {
            double totalDouble = (double) total;
            double progress = (actual / totalDouble) * 100;
            donutProgress.setProgress((int) progress);
            progTv.setText(actual + "/" + total);
            ProgressCircle.this.actual = actual;
        }
    }
}
