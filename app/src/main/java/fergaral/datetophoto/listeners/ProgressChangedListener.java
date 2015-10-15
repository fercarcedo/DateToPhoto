package fergaral.datetophoto.listeners;

import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Parejúa on 23/03/2015.
 */
public interface ProgressChangedListener extends Serializable {
    void reportTotal(int total);
    void onProgressChanged(int progress);
    void reportEnd(boolean fromActionShare);
}
