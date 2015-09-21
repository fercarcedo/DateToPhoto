package fergaral.datetophoto.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
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

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.andexert.library.RippleView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.software.shell.fab.ActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.db.DatabaseHelper;
import fergaral.datetophoto.fragments.LoadPhotosFragment;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.receivers.ActionCancelReceiver;
import fergaral.datetophoto.services.RegisterPhotoURIIntoDBService;
import fergaral.datetophoto.utils.CircularProgressWheel;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.ProgressCircle;
import fergaral.datetophoto.utils.TickedImageView;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 18/07/15.
 */
public class PhotosActivity extends AppCompatActivity implements ProgressChangedListener,
                                                                    LoadPhotosFragment.TaskCallbacks {

    private static final String IS_REFRESHING_KEY = "isRefreshing";
    private static final String PROGRESSBAR_SHOWING_KEY = "progressBarShowing";
    private static final String LOAD_PHOTOS_FRAGMENT_TAG = "loadPhotosFragment";
    private static final String PHOTOS_LIST_KEY = "photosList";
    private static final String SELECTED_PHOTOS_KEY = "selectedPhotos";
    private static final String FAB_PRESSED_KEY = "fabPressed";
    public static final String EXTRA_IMAGE_URI = "fergaraldatetophotoextraimageuri";
    public static boolean SHOULD_REFRESH_GRID = false;
    public static boolean IS_PROCESSING = false;

    private GridView photosGrid;
    private ArrayList<String> selectedPaths;
    private boolean wasFABPressed;
    private ActionButton processPhotosBtn, fabSpeedDial1, fabSpeedDial2;
    private View coverView;
    private int mNumberOfColumns;
    private CardView cardSpeedDial1, cardSpeedDial2;
    private ProgressCircle progressCircle;
    private CircularProgressWheel loadingBar;
    private TextView noPhotosTextView;
    private boolean hideNoPhotosTextView;
    private Bundle mSavedInstanceState;
    private Intent mIntent;
    private ArrayList<String> mImagesToProcess;
    private LoadPhotosFragment loadPhotosFragment;
    private boolean isRefreshing;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        PhotoUtils.selectAllFoldersOnFirstUse(this);

        noPhotosTextView = (TextView) findViewById(R.id.nophotostv);
        mNumberOfColumns = Utils.landscape(PhotosActivity.this) ? 5 : 3;
        processPhotosBtn = (ActionButton) findViewById(R.id.btnProcessSelectedPhotos);
        loadingBar = (CircularProgressWheel) findViewById(R.id.loadingPhotosProgressBar);
        fabSpeedDial1 = (ActionButton) findViewById(R.id.fab_speeddial_action1);
        fabSpeedDial2 = (ActionButton) findViewById(R.id.fab_speeddial_action2);
        coverView = findViewById(R.id.coverView);
        progressCircle = (ProgressCircle) findViewById(R.id.progressCircle);

        progressCircle.setOnCancelListener(new ProgressCircle.OnCancelListener() {
            @Override
            public void onCancel(View view) {
                sendBroadcast(new Intent(PhotosActivity.this, ActionCancelReceiver.class));
            }
        });

        coverView.setOnClickListener(new View.OnClickListener() {
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
                clickFabSpeedDial1();
            }
        });

        fabSpeedDial2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFabSpeedDial2();
            }
        });

        cardSpeedDial1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFabSpeedDial1();
            }
        });

        cardSpeedDial2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickFabSpeedDial2();
            }
        });

        selectedPaths = new ArrayList<>();
        photosGrid = (GridView) findViewById(R.id.photos_grid);

        final Intent intent = getIntent();

        //Si es la primera vez que se abre la aplicación, buscamos si ya hay fotos sin fechar en el dispositivo
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean searchPhotos = prefs.getBoolean("searchPhotosFirstUse", true);

        mIntent = intent;
        mSavedInstanceState = savedInstanceState;

        if(savedInstanceState != null && savedInstanceState.containsKey(IS_REFRESHING_KEY))
            isRefreshing = savedInstanceState.getBoolean(IS_REFRESHING_KEY, true);
        else
            isRefreshing = true;

        if(searchPhotos) {
            //Mostramos un diálogo preguntando si usaron Date To Photo previamente, pero solo si es la primera vez que
            //se abre la app (porque sería un DialogFragment y se mantiene intacto en rotaciones, etc)
            if(savedInstanceState == null) {
                new FirstUseDialogFragment().show(getSupportFragmentManager(), FirstUseDialogFragment.class.getSimpleName());
            }
        }else{
            if(!isRefreshing && savedInstanceState != null && savedInstanceState.containsKey(PHOTOS_LIST_KEY)) {
                mImagesToProcess = savedInstanceState.getStringArrayList(PHOTOS_LIST_KEY);

                if(mImagesToProcess != null && mImagesToProcess.size() > 0) {

                    if(savedInstanceState.containsKey(SELECTED_PHOTOS_KEY))
                        selectedPaths = savedInstanceState.getStringArrayList(SELECTED_PHOTOS_KEY);

                    if(savedInstanceState.containsKey(FAB_PRESSED_KEY))
                        wasFABPressed = savedInstanceState.getBoolean(FAB_PRESSED_KEY);

                    if(wasFABPressed) {
                        fabSpeedDial1.show();
                        fabSpeedDial2.show();
                        cardSpeedDial1.setVisibility(View.VISIBLE);
                        cardSpeedDial2.setVisibility(View.VISIBLE);
                        coverView.setVisibility(View.VISIBLE);
                        processPhotosBtn.setImageResource(R.drawable.ic_clear_24dp);
                    }

                    photosGrid.setAdapter(new PhotosAdapter(mImagesToProcess));
                    processPhotosBtn.show();
                }else{
                    noPhotosTextView.setVisibility(View.VISIBLE);
                }
            }else {
                //Iniciamos la AsyncTask (en el Headless Fragment)
                loadPhotosFragment = (LoadPhotosFragment) getSupportFragmentManager()
                                                                .findFragmentByTag(LOAD_PHOTOS_FRAGMENT_TAG);

                if(loadPhotosFragment == null) {
                    //Todavía no hemos creado el Fragment. Lo creamos e iniciamos la AsyncTask
                    loadPhotosFragment = new LoadPhotosFragment();
                    getSupportFragmentManager()
                            .beginTransaction()
                            .add(loadPhotosFragment, LOAD_PHOTOS_FRAGMENT_TAG)
                            .commit();
                }else{
                    //El Fragment ya se creó (pudo ser debido a un cambio de rotación)
                    if(savedInstanceState != null && savedInstanceState.containsKey(PROGRESSBAR_SHOWING_KEY)) {
                        boolean wasProgressBarShowing = savedInstanceState.getBoolean(PROGRESSBAR_SHOWING_KEY);

                        if(wasProgressBarShowing)
                            loadingBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            checkSharedPhotos(savedInstanceState, intent);
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
                showAboutDialog();
                break;
            case R.id.action_tutorial:
                Utils.startActivityCompat(this, new Intent(this, TutorialActivity.class));
                break;
            case R.id.action_refresh:
                refreshGrid();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void reportTotal(final int total) {
        showProgress(total, "Fechando fotos...");

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
                    new ShareEndDialogFragment().show(getSupportFragmentManager(), ShareEndDialogFragment.class.getSimpleName());
                }
            }
        });

        hideProgress();

        PhotosAdapter adapter = (PhotosAdapter) photosGrid.getAdapter();

        if(adapter != null) {
            if (adapter.isEmpty() && !hideNoPhotosTextView) {
                noPhotosTextView.setVisibility(View.VISIBLE);
            } else if (!adapter.isEmpty()) {
                processPhotosBtn.show();
            }
        }

        if(fromActionShare)
            refreshGrid();
    }

    @Override
    public void onPreExecute() {
        loadingBar.setVisibility(View.VISIBLE);
        photosGrid.setVisibility(View.INVISIBLE);
        noPhotosTextView.setVisibility(View.INVISIBLE);
        processPhotosBtn.hide();
    }

    @Override
    public void onPostExecute(ArrayList<String> imagesToProcess) {

        if(isRefreshing)
            isRefreshing = false;

        mImagesToProcess = imagesToProcess;

        if(!PhotosActivity.IS_PROCESSING) {
            photosGrid.setVisibility(View.VISIBLE);
            photosGrid.setAdapter(new PhotosAdapter(imagesToProcess));
        }

        if (loadingBar.getVisibility() == View.VISIBLE)
            loadingBar.setVisibility(View.INVISIBLE);

        if(hideNoPhotosTextView)
            hideNoPhotosTextView = false;

        if(!PhotosActivity.IS_PROCESSING) {
            if (imagesToProcess.size() == 0) {
                noPhotosTextView.setVisibility(View.VISIBLE);
            } else if (imagesToProcess.size() != 0) {
                processPhotosBtn.show();
            }
        }
    }

    public class PhotosAdapter extends BaseAdapter {
        private List<String> images;

        public PhotosAdapter(List<String> images) {
            this.images = images;

            if(images != null && images.size() != 0) {
                if(noPhotosTextView != null)
                    noPhotosTextView.setVisibility(View.INVISIBLE);
            }
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
            thumbV.setSelected(selectedPaths.contains(images.get(position)));

            thumbV.setOnClickListener(new View.OnClickListener() {
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

            Log.d("TAG", images.get(position));
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

            /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
            }*/

            coverView.setVisibility(View.INVISIBLE);

            //Al estar pulsado previamente, el icono que había era el aspa, así que lo cambiamos por el tick
            processPhotosBtn.setImageResource(R.drawable.ic_done_24px);
        }else{
            //Si no estaba pulsado, mostramos los otros dos FAB
            fabSpeedDial1.show();
            fabSpeedDial2.show();
            cardSpeedDial1.setVisibility(View.VISIBLE);
            cardSpeedDial2.setVisibility(View.VISIBLE);

            /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
            }*/

            coverView.setVisibility(View.VISIBLE);

            //Al no estar pulsado previamente, el icono que había era el tick, así que lo cambiamos por el aspa
            processPhotosBtn.setImageResource(R.drawable.ic_clear_24dp);
        }

        wasFABPressed = !wasFABPressed;
    }

    private void handleSingleImage(Intent intent) {
        Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        ArrayList<String> selectedPaths = new ArrayList<>();

        if(imageUri != null) {
            selectedPaths.add(imageUri.toString());
            //Lanzamos el servicio de procesar fotos por URI con la foto
            Utils.startProcessPhotosURIService(this, this, selectedPaths);

            /*//Si podemos encontrar la ruta local de la foto, la añadimos a la BD
            Intent addPhotoIntent = new Intent(this, RegisterPhotoURIIntoDBService.class);

            ArrayList<Uri> imageUris = new ArrayList<>();
            imageUris.add(imageUri);

            addPhotoIntent.putExtra(EXTRA_IMAGE_URI, imageUris);
            startService(addPhotoIntent);*/
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

            /*//Si existe la ruta local de la URI, añadimos la ruta de la foto a la base de datos
            Intent addPhotosIntent = new Intent(this, RegisterPhotoURIIntoDBService.class);
            addPhotosIntent.putExtra(EXTRA_IMAGE_URI, imageUris);
            startService(addPhotosIntent);*/
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
        if (adapter.isEmpty() && !hideNoPhotosTextView) {
            noPhotosTextView.setVisibility(View.VISIBLE);
            processPhotosBtn.hide();
        }
    }

    private void showProgress(int total, String title) {
        PhotosActivity.IS_PROCESSING = true;

        photosGrid.setVisibility(View.INVISIBLE);
        loadingBar.setVisibility(View.INVISIBLE);

        Utils.lockOrientation(this);

        //Establecemos el total de fotos a fechar
        progressCircle.setTotal(total);
        progressCircle.setVisibility(View.VISIBLE);
        progressCircle.setTitle(title);
    }

    private void hideProgress() {
        PhotosActivity.IS_PROCESSING = false;

        Utils.unlockOrientation(this);
        photosGrid.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.INVISIBLE);

        if(PhotosActivity.SHOULD_REFRESH_GRID) {
            //Actualizamos la lista de fotos
            refreshGrid();
        }
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
            refreshGrid();
        }
    }

    private void selectAll() {
        PhotosAdapter adapter = (PhotosAdapter) photosGrid.getAdapter();

        for(int i=0; i<adapter.getCount(); i++)
            selectedPaths.add(adapter.getImage(i));
    }

    private void showAboutDialog() {
        new AboutDialogFragment().show(getSupportFragmentManager(), AboutDialogFragment.class.getSimpleName());
    }

    public static class AboutDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String versionName = "";
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            return new MaterialDialog.Builder(getActivity())
                    .title("Date To Photo " + versionName)
                    .content("Desarrollada por Fernando García Álvarez\nTesting por Sergio Artidiello González")
                    .positiveText("Aceptar")
                    .iconRes(R.drawable.ic_launcher)
                    .show();
        }
    }

    public static class FirstUseDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final MaterialDialog usedPreviouslyDialog = new MaterialDialog.Builder(getActivity())
                    .title("¿Has usado antes Date To Photo?")
                    .content("Selecciona SÍ si en este dispositivo hay alguna foto fechada previamente con esta aplicación. "
                            + "Si no hay, o no recuerdas haber usado Date To Photo, selecciona NO")
                    .positiveText("Sí")
                    .negativeText("No")
                    .cancelable(false)
                    .autoDismiss(true)
                    .build();

            final PhotosActivity activity = (PhotosActivity) getActivity();

            usedPreviouslyDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    usedPreviouslyDialog.dismiss();

                    //Buscamos si hay fotos sin fechar


                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("searchPhotosFirstUse", false);
                    editor.apply();

                    Intent searchPhotosIntent = new Intent(getActivity(), ProgressActivity.class);
                    searchPhotosIntent.putExtra(ProgressActivity.SEARCH_PHOTOS_KEY, true);

                    startActivity(searchPhotosIntent);
                }
            });

            usedPreviouslyDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    usedPreviouslyDialog.dismiss();

                    activity.refreshGrid();
                    activity.checkSharedPhotos(activity.mSavedInstanceState, activity.mIntent);
                }
            });

            usedPreviouslyDialog.show();

            return usedPreviouslyDialog;
        }
    }

    public static class ShareEndDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new MaterialDialog.Builder(getActivity())
                    .title("El proceso ha finalizado")
                    .content("Como las fotos han sido compartidas desde una aplicación, las fotos con fecha que " +
                            "no procedían del dispositivo (por ejemplo, de la nube) se han guardado en la" +
                            " carpeta DateToPhoto.")
                    .positiveText("De acuerdo")
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        if(wasFABPressed)
            clickBigFAB();
        else
            super.onBackPressed();
    }

    private void refreshGrid() {
        if(loadPhotosFragment != null) {
            loadPhotosFragment.refresh();
            isRefreshing = true;
        }else {
            createLoadPhotosFragment();
            if (loadPhotosFragment != null) {
                loadPhotosFragment.refresh();
                isRefreshing = true;
            }
        }

        PhotosActivity.SHOULD_REFRESH_GRID = false;
        hideNoPhotosTextView = true;
    }

    private void checkSharedPhotos(Bundle savedInstanceState, Intent intent) {
        //Verificamos si la aplicación se abrió desde el menú de compartir, y si se compartieron 1 o varias fotos
        if(savedInstanceState == null) {
            if (Intent.ACTION_SEND.equals(intent.getAction()))
                handleSingleImage(intent);
            else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
                handleMultipleImages(intent);
        }
    }

    private void clickFabSpeedDial1() {
        //Se pulsó el botón de fechar las fotos seleccionadas
        //Arrancamos el servicio de fechar fotos con las imágenes seleccionadas
        /*clearSelectedPhotos();
        processPhotosBtn.hide();

        Utils.startProcessPhotosService(PhotosActivity.this, PhotosActivity.this, selectedPaths);

        //Ocultamos los botones (es equivalente a pulsar el FAB grande)
        clickBigFAB();*/

        Intent progressActivityIntent = new Intent(this, ProgressActivity.class);
        progressActivityIntent.putStringArrayListExtra(ProgressActivity.SELECTED_PATHS_KEY, selectedPaths);
        startActivity(progressActivityIntent);
    }

    private void clickFabSpeedDial2() {
        selectAll();
        clickFabSpeedDial1();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(mImagesToProcess != null)
            outState.putStringArrayList(PHOTOS_LIST_KEY, mImagesToProcess);

        if(selectedPaths != null)
            outState.putStringArrayList(SELECTED_PHOTOS_KEY, selectedPaths);

        outState.putBoolean(FAB_PRESSED_KEY, wasFABPressed);

        outState.putBoolean(PROGRESSBAR_SHOWING_KEY, loadingBar.getVisibility() == View.VISIBLE);

        outState.putBoolean(IS_REFRESHING_KEY, isRefreshing);

        //Dejamos que se guarden los valores por defecto (texto introdcido en un EditText, etc)
        super.onSaveInstanceState(outState);
    }

    public void createLoadPhotosFragment() {
        //Iniciamos la AsyncTask (en el Headless Fragment)
        loadPhotosFragment = (LoadPhotosFragment) getSupportFragmentManager()
                .findFragmentByTag(LOAD_PHOTOS_FRAGMENT_TAG);

        if(loadPhotosFragment == null) {
            //Todavía no hemos creado el Fragment. Lo creamos e iniciamos la AsyncTask
            loadPhotosFragment = new LoadPhotosFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(loadPhotosFragment, LOAD_PHOTOS_FRAGMENT_TAG)
                    .commit();
        }
    }
}
