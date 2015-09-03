package fergaral.datetophoto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 14/06/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private String CREATE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + PATH_COLUMN + " TEXT) ";
    public static final String TABLE_NAME = "Photos";
    public static final String PATH_COLUMN = "path";
    public static final String DB_NAME = "fergaraldatetophotodb";
    public static final int CURRENT_VERSION = 3;

    public DatabaseHelper(Context context) {
        this(context, DB_NAME, null, CURRENT_VERSION);
    }

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //En cualquier caso, eliminamos la tabla de antes y la volvemos a crear
        //Porque ahora mismo no está en producción

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL(CREATE_SQL);
    }
}