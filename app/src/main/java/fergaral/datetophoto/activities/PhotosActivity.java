package fergaral.datetophoto.activities;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

import fergaral.datetophoto.R;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.TickedImageView;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 18/07/15.
 */
public class PhotosActivity extends AppCompatActivity {

    private GridView photosGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        photosGrid = (GridView) findViewById(R.id.photos_grid);

        SQLiteDatabase photosDb = new DatabaseHelper(this).getReadableDatabase();

        //Obtenemos las fotos sin fechar de entre las que hay que procesar, ya que es más rápido identificar las que hay
        //que procesar, que no las que están sin fechars
        ArrayList<String> imagesToProcess = Utils.getPhotosWithoutDate(this,
                Utils.getImagesToProcess(this, new PhotoUtils(this).getCameraImages()), photosDb);

        photosDb.close();

        //Si el dispositivo está apaisado, 5 columnas. En otro caso, 3
        int numberOfColumns = Utils.landscape(this) ? 5 : 3;
        photosGrid.setAdapter(new PhotosAdapter(imagesToProcess));
        photosGrid.setNumColumns(numberOfColumns);

        photosGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(PhotosActivity.this, String.valueOf(position), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_diskspaceusage:
                Utils.startActivityCompat(this, new Intent(this, DiskSpaceUsageActivity.class));
                break;
            case R.id.action_settings:
                Utils.startActivityCompat(this, new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_about:
                Utils.startActivityCompat(this, new Intent(this, AboutActivity.class));
                break;
            case R.id.action_tutorial:
                Utils.startActivityCompat(this, new Intent(this, TutorialActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public class PhotosAdapter extends BaseAdapter {
        private ArrayList<String> images;

        public PhotosAdapter(ArrayList<String> images) {
            this.images = images;
        }

        @Override
        public int getCount() {
            return images.size();
        }

        @Override
        public Object getItem(int position) {
            return images.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;

            if(row == null) {
                //No tenemos vista reciclada, la creamos
                row = getLayoutInflater().inflate(R.layout.grid_row, parent, false);
            }

            TickedImageView thumbV = (TickedImageView) row.findViewById(R.id.thumb);
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int numberOfColumns = Utils.landscape(PhotosActivity.this) ? 5 : 3;
            int width = screenWidth / numberOfColumns;

            thumbV.setDrawingWidth(width);

            //Por si la TickedImageView fue reciclada, la desmarcamos
            thumbV.setSelected(false);

            //Cargamos la imagen
            Glide.with(PhotosActivity.this)
                    .load(new File(images.get(position)))
                    .centerCrop()
                    .into(thumbV);

            return row;
        }
    }
}
