package fergaral.datetophoto.utils;

import android.net.Uri;

/**
 * Created by fer on 12/07/15.
 */
public class Thumbnail {

    private String imagePath, thumbPath;
    private Uri uri;

    public Thumbnail(String imagePath, String thumbPath, Uri uri) {
        this.imagePath = imagePath;
        this.thumbPath = thumbPath;
        this.uri = uri;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public Uri getUri() {
        return uri;
    }
}
