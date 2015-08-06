package fergaral.datetophoto.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.software.shell.fab.ActionButton;

import java.io.File;
import java.util.ArrayList;

import fergaral.datetophoto.R;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.services.RegisterPhotoURIIntoDBService;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.ProgressCircle;
import fergaral.datetophoto.utils.TickedImageView;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 18/07/15.
 */
public class PhotosActivity extends AppCompatActivity implements ProgressChangedListener {

    public static final String EXTRA_IMAGE_URI = "fergaraldatetophotoextraimageuri";
    public static boolean SHOULD_REFRESH_GRID = false;
    public static boolean IS_PROCESSING = false;

    private GridView photosGrid;
    private ArrayList<String> selectedPaths;
    private boolean wasFABPressed;
    private ActionButton processPhotosBtn, fabSpeedDial1, fabSpeedDial2;
    private RelativeLayout coverRl;
    private int mNumberOfColumns;
    private CardView cardSpeedDial1, cardSpeedDial2;
    private ProgressCircle progressCircle;
    private TextView datestampingPhotosTv;
    private ProgressBar loadingBar;
    private TextView noPhotosTextView;
    private AsyncTask loadPhotosTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        noPhotosTextView = (TextView) findViewById(R.id.nophotostv);
        mNumberOfColumns = Utils.landscape(PhotosActivity.this) ? 5 : 3;
        processPhotosBtn = (ActionButton) findViewById(R.id.btnProcessSelectedPhotos);
        loadingBar = (ProgressBar) findViewById(R.id.loadingPhotosProgressBar);
        fabSpeedDial1 = (ActionButton) findViewById(R.id.fab_speeddial_action1);
        fabSpeedDial2 = (ActionButton) findViewById(R.id.fab_speeddial_action2);
        coverRl = (RelativeLayout) findViewById(R.id.photos_cover_rl);
        datestampingPhotosTv = (TextView) findViewById(R.id.datestamping_photos_tv);
        progressCircle = (ProgressCircle) findViewById(R.id.progressCircle);

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
                clearSelectedPhotos();
                processPhotosBtn.hide();

                Utils.startProcessPhotosService(PhotosActivity.this, PhotosActivity.this, selectedPaths);

                //Ocultamos los botones (es equivalente a pulsar el FAB grande)
                clickBigFAB();
            }
        });

        fabSpeedDial2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                selectAll();

                clearSelectedPhotos();
                processPhotosBtn.hide();

                //Fechamos todas las fotos
                Utils.startProcessPhotosService(PhotosActivity.this, PhotosActivity.this, selectedPaths);

                //Ocultamos los botones ("pulsamos" el FAB)
                clickBigFAB();
            }
        });

        selectedPaths = new ArrayList<>();
        photosGrid = (GridView) findViewById(R.id.photos_grid);

        loadPhotosTask = new ImagesToProcessTask().execute();

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
                    datestampingPhotosTv.setText("Buscando fotos ya fechadas...");
                    showProgress(total);
                }

                @Override
                public void onProgressChanged(final int progress) {

                }

                @Override
                public void reportEnd(boolean fromActionShare) {
                    hideProgress();
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
        showProgress(total);

        selectedPaths = new ArrayList<>();
    }

    @Override
    public void onProgressChanged(final int progress) {
        progressCircle.setActual(this, progress);
    }

    @Override
    public void reportEnd(final boolean fromActionShare) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (fromActionShare) {
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

        hideProgress();

        PhotosAdapter adapter = (PhotosAdapter) photosGrid.getAdapter();
        if(adapter.isEmpty()) {
            noPhotosTextView.setVisibility(View.VISIBLE);
        }else{
            processPhotosBtn.show();
        }
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

        public void removeImage(String image) {
            images.remove(image);
        }
        public String getImage(int position) {
            return images.get(position);
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

    public class ImagesToProcessTask extends AsyncTask<Void, Void, ArrayList<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingBar.setVisibility(View.VISIBLE);
            photosGrid.setVisibility(View.INVISIBLE);
            noPhotosTextView.setVisibility(View.INVISIBLE);
            processPhotosBtn.hide();
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            SQLiteDatabase photosDb = new DatabaseHelper(PhotosActivity.this).getReadableDatabase();

            ArrayList<String> cameraImages = new PhotoUtils(PhotosActivity.this).getCameraImages();

            //Obtenemos las fotos sin fechar de entre las que hay que procesar, ya que es más rápido identificar las que hay
            //que procesar, que no las que están sin fechars
            ArrayList<String> imagesToProcess = Utils.getPhotosWithoutDate(PhotosActivity.this,
                    Utils.getImagesToProcess(PhotosActivity.this, cameraImages), photosDb);

            photosDb.close();

            return imagesToProcess;
        }

        @Override
        protected void onPostExecute(ArrayList<String> imagesToProcess) {
            super.onPostExecute(imagesToProcess);

            photosGrid.setVisibility(View.VISIBLE);
            photosGrid.setAdapter(new PhotosAdapter(imagesToProcess));
            photosGrid.setNumColumns(mNumberOfColumns);

            if (loadingBar.getVisibility() == View.VISIBLE)
                loadingBar.setVisibility(View.INVISIBLE);

            if (imagesToProcess.size() == 0) {
                noPhotosTextView.setVisibility(View.VISIBLE);
            } else {
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

    private void clearSelectedPhotos() {
        //Eliminamos de la lista las fotos que se van a fechar
        PhotosAdapter adapter = (PhotosAdapter) photosGrid.getAdapter();

        for(String path : selectedPaths) {
            adapter.removeImage(path);
        }

        adapter.notifyDataSetChanged();
    }

    private void checkImagesToProcessEmpty(PhotosAdapter adapter) {
        if (adapter.isEmpty()) {
            noPhotosTextView.setVisibility(View.VISIBLE);
            processPhotosBtn.hide();
        }
    }

    private void showProgress(int total) {
        PhotosActivity.IS_PROCESSING = true;

        photosGrid.setVisibility(View.INVISIBLE);
        loadingBar.setVisibility(View.INVISIBLE);

        Utils.lockOrientation(this);

        //Establecemos el total de fotos a fechar
        progressCircle.setTotal(total);
        progressCircle.setVisibility(View.VISIBLE);
        datestampingPhotosTv.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        PhotosActivity.IS_PROCESSING = false;

        Utils.unlockOrientation(this);
        photosGrid.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.INVISIBLE);
        datestampingPhotosTv.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PhotosActivity.SHOULD_REFRESH_GRID = true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        if(!PhotosActivity.IS_PROCESSING && PhotosActivity.SHOULD_REFRESH_GRID) {
            //Actualizamos la lista de fotos
            if(loadPhotosTask != null && !loadPhotosTask.isCancelled()) {
                //Cancelamos la AsyncTask (el onPostExecute no se ejecutará)
                loadPhotosTask.cancel(true);
            }

            loadPhotosTask = new ImagesToProcessTask().execute();
        }

        PhotosActivity.SHOULD_REFRESH_GRID = false;
    }

    private void selectAll() {
        PhotosAdapter adapter = (PhotosAdapter) photosGrid.getAdapter();

        for(int i=0; i<adapter.getCount(); i++)
            selectedPaths.add(adapter.getImage(i));
    }
}
