package fergaral.datetophoto.receivers;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;

/**
 * Created by fer on 10/06/16.
 */
public class PhotosObserver extends ContentObserver {
    private Context mContext;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public PhotosObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        this.onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        Media media = readFromMediaStore(mContext,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        if(media != null && media.getFile() != null)
            Toast.makeText(mContext, "Detected " + media.getFile().getName(), Toast.LENGTH_SHORT).show();
    }

    private Media readFromMediaStore(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri,
                                                null,
                                                null,
                                                null,
                                                "date_added DESC");

        Media media = null;

        if(cursor != null && cursor.moveToNext()) {
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            media = new Media(new File(cursor.getString(dataColumn)),
                                cursor.getString(mimeTypeColumn));
        }

        if(cursor != null)
            cursor.close();

        return media;
    }

    private class Media {
        private File file;
        private String type;

        public Media(File file, String type) {
            this.file = file;
            this.type = type;
        }

        public File getFile() {
            return file;
        }

        public String getType() {
            return type;
        }
    }


}
