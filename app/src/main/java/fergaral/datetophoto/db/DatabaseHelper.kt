package fergaral.datetophoto.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by fer on 14/06/15.
 */
class DatabaseHelper @JvmOverloads constructor(context: Context, name: String = DB_NAME, factory: SQLiteDatabase.CursorFactory? = null, version: Int = CURRENT_VERSION) : SQLiteOpenHelper(context, name, factory, version) {

    private val CREATE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + PATH_COLUMN + " TEXT, " +
            FOLDER_COLUMN + " TEXT) "

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //En cualquier caso, eliminamos la tabla de antes y la volvemos a crear
        //Porque ahora mismo no está en producción

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        db.execSQL(CREATE_SQL)
    }

    companion object {
        val TABLE_NAME = "Photos"
        val PATH_COLUMN = "path"
        val FOLDER_COLUMN = "folder"
        val DB_NAME = "fergaraldatetophotodb"
        val CURRENT_VERSION = 4
    }
}