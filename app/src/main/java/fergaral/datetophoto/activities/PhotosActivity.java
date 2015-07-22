package fergaral.datetophoto.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.software.shell.fab.ActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Queue;
import java.util.prefs.PreferenceChangeEvent;

import fergaral.datetophoto.R;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.services.RegisterPhotoURIIntoDBService;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.TickedImageView;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 18/07/15.
 */
public class PhotosActivity extends AppCompatActivity implements ProgressChangedListener {

    public static final String EXTRA_IMAGE_URI = "fergaraldatetophotoextraimageuri";

    private GridView photosGrid;
    private ArrayList<String> selectedPaths;
    private boolean wasFABPressed;
    private ActionButton processPhotosBtn, fabSpeedDial1, fabSpeedDial2;
    private RelativeLayout coverRl;
    private int mNumberOfColumns;
    private CardView cardSpeedDial1, cardSpeedDial2;
    private DonutProgress circleProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        mNumberOfColumns = Utils.landscape(PhotosActivity.this) ? 5 : 3;
        processPhotosBtn = (ActionButton) findViewById(R.id.btnProcessSelectedPhotos);
        fabSpeedDial1 = (ActionButton) findViewById(R.id.fab_speeddial_action1);
        fabSpeedDial2 = (ActionButton) findViewById(R.id.fab_speeddial_action2);
        circleProgress = (DonutProgress) findViewById(R.id.progress_circle);
        coverRl = (RelativeLayout) findViewById(R.id.photos_cover_rl);

        coverRl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Pulsar el fondo gris es equivalente a pulsar el botón grande
                clickBigFAB();
            }
        });

        cardSpeedDial1 = (CardView) findViewById(R.id.card_fab_speeddial_action1);
        cardSpeedDial2 = (CardView) findViewById(R.id.card_fab_speeddial_action2);

        cardSpeedDial1.setVisibility(View.GONE);
        cardSpeedDial2.setVisibility(View.GONE);

        //Inicialmente los FABs no están visibles
        processPhotosBtn.hide();
        fabSpeedDial1.hide();
        fabSpeedDial2.hide();

        //Establecemos el onClick del FAB
        processPhotosBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickBigFAB();
            }
        });

        fabSpeedDial1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Se pulsó el botón de fechar las fotos seleccionadas
                //Arrancamos el servicio de fechar fotos con las imágenes seleccionadas
                Utils.startProcessPhotosService(PhotosActivity.this, PhotosActivity.this, selectedPaths);

                //Ocultamos los botones (es equivalente a pulsar el FAB grande)
                clickBigFAB();
            }
        });

        fabSpeedDial2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Fechamos todas las fotos
                Utils.startProcessPhotosService(PhotosActivity.this, PhotosActivity.this, null);

                //Ocultamos los botones ("pulsamos" el FAB)
                clickBigFAB();
            }
        });

        selectedPaths = new ArrayList<>();
        photosGrid = (GridView) findViewById(R.id.photos_grid);

        new ImagesToProcessTask().execute(new PhotoUtils(this).getCameraImages());

        Intent intent = getIntent();

        //Verificamos si la aplicación se abrió desde el menú de compartir, y si se compartieron 1 o varias fotos
        if(savedInstanceState == null) {
            if (Intent.ACTION_SEND.equals(intent.getAction()))
                handleSingleImage(intent);
            else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
                handleMultipleImages(intent);
        }

        //Si es la primera vez que se abre la aplicación, buscamos si ya hay fotos sin fechar en el dispositivo
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean searchPhotos = prefs.getBoolean("searchPhotosFirstUse", true);

        if(searchPhotos) {
            //Buscamos si hay fotos sin fechar
            Utils.searchForAlreadyProcessedPhotos(this, new ProgressChangedListener() {
                private int total;

                @Override
                public void reportTotal(final int total) {

                }

                @Override
                public void onProgressChanged(final int progress) {

                }

                @Override
                public void reportEnd(boolean fromActionShare) {

                }
            });

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("searchPhotosFirstUse", false);
            editor.apply();
        }
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

    @Override
    public void reportTotal(final int total) {

    }

    @Override
    public void onProgressChanged(final int progress) {

    }

    @Override
    public void reportEnd(final boolean fromActionShare) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(fromActionShare) {
                    //Las fotos fueron compartidas desde una aplicación, mostramos el diálogo que advierte al usuario de que las
                    //fotos ya fechadas están en la carpeta DateToPhoto
                    new MaterialDialog.Builder(PhotosActivity.this)
                            .title("El proceso ha finalizado")
                            .content("Como las fotos han sido compartidas desde una aplicación, las fotos con fecha se han guardado en la" +
                                    " carpeta DateToPhoto. Si quieres que la foto con fecha se guarde en la misma carpeta que la original " +
                                    " (o que la sobreescriba, no feches las fotos desde el botón de compartir)")
                            .positiveText("De acuerdo")
                            .show();
                }
            }
        });
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
        public View getView(final int position, View convertView, ViewGroup parent) {
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

            thumbV.setOnImageClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TickedImageView view = (TickedImageView) v;

                    if (view.isSelected())
                        selectedPaths.add(images.get(position));
                    else
                        selectedPaths.remove(images.get(position));
                }
            });

            //Cargamos la imagen
            Glide.with(PhotosActivity.this)
                    .load(new File(images.get(position)))
                    .centerCrop()
                    .into(thumbV);

            return row;
        }
    }

    private void clickBigFAB() {
        if(wasFABPressed) {
            //Si el FAB estaba pulsado, ocultamos los otros dos botones
            fabSpeedDial1.hide();
            fabSpeedDial2.hide();
            cardSpeedDial1.setVisibility(View.GONE);
            cardSpeedDial2.setVisibility(View.GONE);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //El centro de la animación de revelar es el botón
                float x = processPhotosBtn.getX();
                float y = processPhotosBtn.getY();

                int cx = (int) (x + (x + processPhotosBtn.getWidth())) / 2;
                int cy = (int) (y + (y - processPhotosBtn.getHeight())) / 2;

                int finalRadius = Math.max(coverRl.getWidth(), coverRl.getHeight()) / 2;

                Animator anim = ViewAnimationUtils.createCircularReveal(coverRl, cx, cy, finalRadius, 0);

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        coverRl.setVisibility(View.INVISIBLE);
                    }
                });
                anim.start();
            }

            //Al estar pulsado previamente, el icono que había era el aspa, así que lo cambiamos por el tick
            processPhotosBtn.setImageResource(R.drawable.ic_done_24px);
        }else{
            //Si no estaba pulsado, mostramos los otros dos FAB
            fabSpeedDial1.show();
            fabSpeedDial2.show();
            cardSpeedDial1.setVisibility(View.VISIBLE);
            cardSpeedDial2.setVisibility(View.VISIBLE);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //El centro de la animación de revelar es el botón
                float x = processPhotosBtn.getX();
                float y = processPhotosBtn.getY();

                int cx = (int) (x + (x + processPhotosBtn.getWidth())) / 2;
                int cy = (int) (y + (y - processPhotosBtn.getHeight())) / 2;

                int finalRadius = Math.max(coverRl.getWidth(), coverRl.getHeight()) / 2;

                Animator anim = ViewAnimationUtils.createCircularReveal(coverRl, cx, cy, 0, finalRadius);
                anim.setInterpolator(new AccelerateDecelerateInterpolator());
                coverRl.setVisibility(View.VISIBLE);
                anim.start();
            }

            //Al no estar pulsado previamente, el icono que había era el tick, así que lo cambiamos por el aspa
            processPhotosBtn.setImageResource(R.drawable.ic_clear_24dp);
        }

        wasFABPressed = !wasFABPressed;
    }

    public class ImagesToProcessTask extends AsyncTask<ArrayList<String>, Void, ArrayList<String>> {
        @Override
        protected ArrayList<String> doInBackground(ArrayList<String>... images) {
            SQLiteDatabase photosDb = new DatabaseHelper(PhotosActivity.this).getReadableDatabase();

            //Obtenemos las fotos sin fechar de entre las que hay que procesar, ya que es más rápido identificar las que hay
            //que procesar, que no las que están sin fechars
            ArrayList<String> imagesToProcess = Utils.getPhotosWithoutDate(PhotosActivity.this,
                    Utils.getImagesToProcess(PhotosActivity.this, images[0]), photosDb);

            photosDb.close();

            return imagesToProcess;
        }

        @Override
        protected void onPostExecute(ArrayList<String> imagesToProcess) {
            super.onPostExecute(imagesToProcess);

            photosGrid.setAdapter(new PhotosAdapter(imagesToProcess));
            photosGrid.setNumColumns(mNumberOfColumns);

            ProgressBar loadingBar = (ProgressBar) findViewById(R.id.loadingPhotosProgressBar);

            if(loadingBar.getVisibility() == View.VISIBLE)
                loadingBar.setVisibility(View.INVISIBLE);

            TextView noPhotosTextView = (TextView) findViewById(R.id.nophotostv);

            if(imagesToProcess.size() == 0) {
                noPhotosTextView.setVisibility(View.VISIBLE);
            }else{
                processPhotosBtn.show();
            }
        }
    }

    private void handleSingleImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        ArrayList<String> selectedPaths = new ArrayList<>();

        if(imageUri != null) {
            selectedPaths.add(imageUri.toString());
            //Lanzamos el servicio de procesar fotos por URI con la foto
            Utils.startProcessPhotosURIService(this, this, selectedPaths);

            //Si podemos encontrar la ruta local de la foto, la añadimos a la BD
            Intent addPhotoIntent = new Intent(this, RegisterPhotoURIIntoDBService.class);

            ArrayList<Uri> imageUris = new ArrayList<>();
            imageUris.add(imageUri);

            addPhotoIntent.putExtra(EXTRA_IMAGE_URI, imageUris);
            startService(addPhotoIntent);
        }
    }

    private void handleMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        ArrayList<String> selectedPaths = new ArrayList<>();

        if(imageUris != null) {
            for(Uri uri : imageUris) {
                if(uri != null) {
                    selectedPaths.add(uri.toString());
                }
            }

            //Procesamos las fotos
            Utils.startProcessPhotosURIService(this, this, selectedPaths);

            //Si existe la ruta local de la URI, añadimos la ruta de la foto a la base de datos
            Intent addPhotosIntent = new Intent(this, RegisterPhotoURIIntoDBService.class);
            addPhotosIntent.putExtra(EXTRA_IMAGE_URI, imageUris);
            startService(addPhotosIntent);
        }
    }
}
