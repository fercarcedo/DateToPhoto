package fergaral.datetophoto.utils;

/**
 * Created by fer on 21/09/15.
 */
public interface ProgressListener {
    void reportTotal(int total);
    void onProgressChanged(int progress, int actual);
    void reportEnd(boolean fromActionShare);
}
