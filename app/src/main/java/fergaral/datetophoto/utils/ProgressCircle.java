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
    private TextView progTv, titleTv;
    private TextView cancelBtn;
    private int total;
    private int actual;
    private OnCancelListener onCancelListener;

    public ProgressCircle(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.circle_prog_layout, this);
        donutProgress = (DonutProgress) view.findViewById(R.id.progress_circle);
        progTv = (TextView) view.findViewById(R.id.progress_circle_progtv);
        titleTv = (TextView) view.findViewById(R.id.progress_circle_titletv);
        cancelBtn = (TextView) view.findViewById(R.id.cancel_btn);

        cancelBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel(v);
            }
        });
    }

    public void setTotal(int total) {
        this.total = total;
        progTv.setText(0 + "/" + total);
    }

    public void setTitle(String title) {
        if(title != null)
            titleTv.setText(title);
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

    public void setOnCancelListener(OnCancelListener listener) {
        onCancelListener = listener;
    }

    public void cancel(View view) {
        setTitle("Cancelando...");

        if(onCancelListener != null)
            onCancelListener.onCancel(view);
    }

    public static abstract class OnCancelListener {
        public abstract void onCancel(View view);
    }
}
