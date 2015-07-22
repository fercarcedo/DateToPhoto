package fergaral.datetophoto.utils;

import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.ResultReceiver;

import fergaral.datetophoto.listeners.ProgressChangedListener;

/**
 * Created by Parejúa on 23/03/2015.
 */
public class MyResultReceiver extends ResultReceiver {

    private ProgressChangedListener receiver;
    private PowerManager.WakeLock wakeLock;

    /**
     * Create a new ResultReceive to receive results.  Your
     * {@link #onReceiveResult} method will be called from the thread running
     * <var>handler</var> if given, or from an arbitrary thread if null.
     *
     * @param handler
     */
    public MyResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(ProgressChangedListener receiver)
    {
        this.receiver = receiver;
    }

    public void setWakeLock(PowerManager.WakeLock wakeLock)
    {
        if(wakeLock != null)
            this.wakeLock = wakeLock;

    }

    public PowerManager.WakeLock getWakeLock()
    {
        return wakeLock;
    }
    
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData)
    {
        if(receiver != null)
        {
            if(resultData.containsKey("total")) {
                if(wakeLock != null)
                    wakeLock.acquire();
                receiver.reportTotal(resultData.getInt("total"));
            }
            if(resultData.containsKey("progress")) {
                receiver.onProgressChanged(resultData.getInt("progress"));
            }
            if(resultData.containsKey("end")) {
                if(wakeLock != null && wakeLock.isHeld())
                    wakeLock.release();
                receiver.reportEnd(false);
            }
            if(resultData.containsKey("endShared")) {
                if(wakeLock != null && wakeLock.isHeld())
                    wakeLock.release();
                receiver.reportEnd(true);
            }
        }
    }
}
