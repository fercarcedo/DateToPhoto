package fergaral.datetophoto.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.software.shell.fab.ActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fergaral.datetophoto.R;
import fergaral.datetophoto.fragments.LoadPhotosFragment;
import fergaral.datetophoto.fragments.ProgressHeadlessFragment;
import fergaral.datetophoto.listeners.ProgressChangedListener;
import fergaral.datetophoto.receivers.ActionCancelReceiver;
import fergaral.datetophoto.utils.CircularProgressWheel;
import fergaral.datetophoto.utils.PhotoUtils;
import fergaral.datetophoto.utils.ProgressCircle;
import fergaral.datetophoto.utils.ProgressListener;
import fergaral.datetophoto.utils.TickedImageView;
import fergaral.datetophoto.utils.Utils;

/**
 * Created by fer on 18/07/15.
 */
public class PhotosActivity extends AppCompatActivity implements ProgressChangedListener,
                                                                    LoadPhotosFragment.TaskCallbacks {

    private static final String SEARCH_PHOTOS_FIRST_USE_KEY = "searchPhotosFirstUse";
    public static final String ACTION_SHARE_KEY = "actionshare";
    private static final String LAST_SELECTED_SPINNER_POSITION_KEY = "lastSelectedSpinnerPos";
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
    private CardView cardSpeedDial1, cardSpeedDial2;
    private boolean hideNoPhotosTextView;
    private com.vlonjatg.progressactivity.ProgressActivity progressActivity;
    private Bundle mSavedInstanceState;
    private Intent mIntent;
    private List<String> mImagesToProcess;
    private LoadPhotosFragment loadPhotosFragment;
    private boolean isRefreshing;
    private CircularProgressWheel loadingProgBar;
    private AppCompatSpinner foldersSpinner;
    private int lastSelectedSpinnerPosition;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        if(savedInstanceState != null)
            lastSelectedSpinnerPosition = savedInstanceState.getInt(LAST_SELECTED_SPINNER_POSITION_KEY, 0);

        foldersSpinner = (AppCompatSpinner) findViewById(R.id.foldersSpinner);

        final List<String> folders = new ArrayList<String>(Arrays.asList(Utils.getFoldersToProcess(this)));
        folders.add(0, "Todas las fotos");
        foldersSpinner.setAdapter(new ArrayAdapter<String>(this, R.layout.spinner_row, folders));
        foldersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(lastSelectedSpinnerPosition != position) {
                    refreshGrid();
                    lastSelectedSpinnerPosition = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        PhotoUtils.selectAllFoldersOnFirstUse(this);

        progressActivity = (com.vlonjatg.progressactivity.ProgressActivity) findViewById(R.id.prog_activ);
        loadingProgBar = (CircularProgressWheel) findViewById(R.id.loading_photos_prog_bar);
        processPhotosBtn = (ActionButton) findViewById(R.id.btnProcessSelectedPhotos);
        fabSpeedDial1 = (ActionButton) findViewById(R.id.fab_speeddial_action1);
        fabSpeedDial2 = (ActionButton) findViewById(R.id.fab_speeddial_action2);
        coverView = findViewById(R.id.coverView);

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
                    showNoPhotosScreen();
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

                        if(wasProgressBarShowing) {
                            showLoading();
                        }

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
            case R.id.action_settings:
                Utils.startActivityCompat(this, new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_about:
                showAboutDialog();
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
        //progressCircle.setActual(this, progress);
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
                showNoPhotosScreen();
            } else if (!adapter.isEmpty()) {
                processPhotosBtn.show();
            }
        }

        if(fromActionShare)
            refreshGrid();
    }

    @Override
    public void onPreExecute() {
        showLoading();
        photosGrid.setVisibility(View.INVISIBLE);
        //hideNoPhotosScreen();
        processPhotosBtn.hide();
    }

    @Override
    public void onPostExecute(List<String> imagesToProcess) {

        if(isRefreshing)
            isRefreshing = false;

        mImagesToProcess = imagesToProcess;

        if(!PhotosActivity.IS_PROCESSING) {
            photosGrid.setVisibility(View.VISIBLE);
            photosGrid.setAdapter(new PhotosAdapter(imagesToProcess));
        }

        progressActivity.showContent();

        if(hideNoPhotosTextView)
            hideNoPhotosTextView = false;

        loadingProgBar.setVisibility(View.INVISIBLE);

        if(!PhotosActivity.IS_PROCESSING) {
            if (imagesToProcess.size() == 0) {
                showNoPhotosScreen();
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
                    hideNoPhotosScreen();
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
            showProgressDialogShare(selectedPaths);
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
            showProgressDialogShare(selectedPaths);

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
            showNoPhotosScreen();
            processPhotosBtn.hide();
        }
    }

    private void showProgress(int total, String title) {
        PhotosActivity.IS_PROCESSING = true;

        photosGrid.setVisibility(View.INVISIBLE);
        progressActivity.showContent();

        Utils.lockOrientation(this);

        //Establecemos el total de fotos a fechar
        //progressCircle.setTotal(total);
        //progressCircle.setVisibility(View.VISIBLE);
        //progressCircle.setTitle(title);
    }

    private void hideProgress() {
        PhotosActivity.IS_PROCESSING = false;

        Utils.unlockOrientation(this);
        photosGrid.setVisibility(View.VISIBLE);
        //progressCircle.setVisibility(View.INVISIBLE);

        if(PhotosActivity.SHOULD_REFRESH_GRID) {
            //Actualizamos la lista de fotos
            refreshGrid();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        PhotosActivity.SHOULD_REFRESH_GRID = false;
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
                    .content("")
                    .positiveText("Aceptar")
                    .iconRes(R.drawable.ic_launcher)
                    .build();
        }
    }

    public static class NoPhotosSelectedDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new MaterialDialog.Builder(getActivity())
                    .title("No has seleccionado ninguna foto")
                    .content("Selecciona alguna foto para continuar")
                    .positiveText("Aceptar")
                    .build();
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

            final PhotosActivity photosActivity = (PhotosActivity) getActivity();

            usedPreviouslyDialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    usedPreviouslyDialog.dismiss();

                    //Indicamos que ya no hay que preguntar más veces si el usuario ha usado antes la aplicación
                    photosActivity.setSearchPhotosFirstUse(false);

                    /*
                    Intent searchPhotosIntent = new Intent(getActivity(), ProgressActivity.class);
                    searchPhotosIntent.putExtra(ProgressActivity.SEARCH_PHOTOS_KEY, true);*/

                    photosActivity.showProgressDialog(true);
                }
            });

            usedPreviouslyDialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    usedPreviouslyDialog.dismiss();

                    //Indicamos que ya no hay que preguntar más veces si el usuario ha usado antes la aplicación
                    photosActivity.setSearchPhotosFirstUse(false);

                    photosActivity.createLoadPhotosFragment();
                    photosActivity.checkSharedPhotos(photosActivity.mSavedInstanceState, photosActivity.mIntent);
                }
            });

            //usedPreviouslyDialog.show();

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
        int selectedFolderPosition = foldersSpinner.getSelectedItemPosition();
        String selectedFolder = (String) foldersSpinner.getSelectedItem();
        if(loadPhotosFragment != null) {
            if(selectedFolderPosition == 0)
                loadPhotosFragment.refresh();
            else
                loadPhotosFragment.load(selectedFolder);
            isRefreshing = true;
        }else {
            createLoadPhotosFragment();
            if(selectedFolderPosition == 0)
                loadPhotosFragment.refresh();
            else
                loadPhotosFragment.load(selectedFolder);
            isRefreshing = true;
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

        /*Intent progressActivityIntent = new Intent(this, ProgressActivity.class);
        progressActivityIntent.putStringArrayListExtra(ProgressActivity.SELECTED_PATHS_KEY, selectedPaths);
        startActivity(progressActivityIntent);*/
        if(selectedPaths == null || selectedPaths.size() == 0)
            new NoPhotosSelectedDialogFragment().show(getSupportFragmentManager(),
                    NoPhotosSelectedDialogFragment.class.getSimpleName());
        else
            showProgressDialog(false);

        clickBigFAB();
    }

    private void clickFabSpeedDial2() {
        selectAll();
        clickFabSpeedDial1();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(mImagesToProcess != null)
            outState.putStringArrayList(PHOTOS_LIST_KEY, new ArrayList<String>(mImagesToProcess)
            );

        if(selectedPaths != null)
            outState.putStringArrayList(SELECTED_PHOTOS_KEY, selectedPaths);

        outState.putBoolean(FAB_PRESSED_KEY, wasFABPressed);

        outState.putBoolean(PROGRESSBAR_SHOWING_KEY, loadingProgBar.getVisibility() == View.VISIBLE);

        outState.putBoolean(IS_REFRESHING_KEY, isRefreshing);

        outState.putInt(LAST_SELECTED_SPINNER_POSITION_KEY, lastSelectedSpinnerPosition);
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

    private void showNoPhotosScreen() {
        progressActivity.showEmpty(getResources().getDrawable(R.drawable.ic_done_white_48px), "No hay fotos sin fechar", "");
    }

    private void hideNoPhotosScreen() {
        progressActivity.showContent();
    }

    private void showLoading() {
        if(progressActivity.isEmpty()) {
            progressActivity.showContent();
            Log.d("TAG", "isEmpty");
        }

        loadingProgBar.setVisibility(View.VISIBLE);
        Log.d("TAG", "Loading");
    }

    private void showProgressDialog(boolean searchPhotos) {
        ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
        Bundle args = new Bundle();

        if(searchPhotos) {
            //Buscamos las fotos sin fechar (es el primer uso)
            args.putBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, true);
        }else{
            args.putBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, false);
            args.putStringArrayList(ProgressActivity.SELECTED_PATHS_KEY, selectedPaths);
        }

        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "progress_dialog");
    }

    private void showProgressDialogShare(ArrayList<String> selectedPaths) {
        ProgressDialogFragment dialogFragment = new ProgressDialogFragment();
        Bundle args = new Bundle();

        args.putBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, false);
        args.putStringArrayList(ProgressActivity.SELECTED_PATHS_KEY, selectedPaths);
        args.putBoolean(PhotosActivity.ACTION_SHARE_KEY, true);

        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "progress_dialog");
    }

    public static class ProgressDialogFragment extends DialogFragment implements ProgressListener {
        private ProgressHeadlessFragment mHeadless;
        private boolean searchPhotos, shareAction;
        private ArrayList<String> selectedPaths;
        private int total = 100;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setCancelable(false);

            Bundle arguments = getArguments();

            if(arguments.containsKey(ProgressActivity.SEARCH_PHOTOS_KEY))
                searchPhotos = arguments.getBoolean(ProgressActivity.SEARCH_PHOTOS_KEY);

            if(searchPhotos)
                total = new PhotoUtils(getActivity()).getCameraImages().size();

            selectedPaths = new ArrayList<>();

            if(arguments.containsKey(ProgressActivity.SELECTED_PATHS_KEY)) {
                selectedPaths = arguments.getStringArrayList(ProgressActivity.SELECTED_PATHS_KEY);
                if(selectedPaths != null)
                    total = selectedPaths.size();
            }

            if(arguments.containsKey(PhotosActivity.ACTION_SHARE_KEY))
                shareAction = arguments.getBoolean(PhotosActivity.ACTION_SHARE_KEY, false);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState)
        {
            super.onActivityCreated(savedInstanceState);

            FragmentManager fm = getFragmentManager();
            mHeadless = (ProgressHeadlessFragment) fm.findFragmentByTag("headless");

            if(mHeadless == null){
                mHeadless = new ProgressHeadlessFragment();

                fm.beginTransaction()
                        .add(mHeadless, "headless")
                        .commit();

                Bundle fragmentArgs = new Bundle();

                fragmentArgs.putBoolean(ProgressActivity.SEARCH_PHOTOS_KEY, searchPhotos);
                fragmentArgs.putStringArrayList(ProgressActivity.SELECTED_PATHS_KEY, selectedPaths);
                fragmentArgs.putBoolean(PhotosActivity.ACTION_SHARE_KEY, shareAction);

                mHeadless.setArguments(fragmentArgs);
                mHeadless.setTargetFragment(this, 0);
            }
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                    .title(searchPhotos ? "Buscando fotos..." : "Fechando fotos...")
                    .content(searchPhotos ? "Estamos buscando fotos ya fechadas en tu dispositivo"
                            : "Tus fotos están siendo fechadas. Relájate y haz otras cosas mientras tanto")
                    .progress(false, total, true)
                    .build();

            if(!searchPhotos) {
                dialog.setActionButton(DialogAction.NEGATIVE, "Cancelar");
                dialog.getActionButton(DialogAction.NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelProgress();
                    }
                });
            }

            return dialog;
        }

        @Override
        public void reportTotal(int total) {
            MaterialDialog dialog = (MaterialDialog) getDialog();
            dialog.setMaxProgress(total);
        }

        @Override
        public void onProgressChanged(int progress, int actual) {
            MaterialDialog dialog = (MaterialDialog) getDialog();
            dialog.setProgress(actual);
        }

        @Override
        public void reportEnd(boolean fromActionShare) {
            dismiss();

            PhotosActivity photosActivity = (PhotosActivity) getActivity();
            if(!searchPhotos) {
                photosActivity.refreshGrid();
            }else{
                photosActivity.createLoadPhotosFragment();
                photosActivity.checkSharedPhotos(photosActivity.mSavedInstanceState, photosActivity.mIntent);
            }
        }

        private void cancelProgress() {
            MaterialDialog dialog = (MaterialDialog) getDialog();
            dialog.setTitle("Cancelando...");
            dialog.setContent("");
            getActivity().sendBroadcast(new Intent(getActivity(), ActionCancelReceiver.class));
        }
    }

    private void setSearchPhotosFirstUse(boolean searchPhotosFirstUse) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SEARCH_PHOTOS_FIRST_USE_KEY, searchPhotosFirstUse);
        editor.apply();
    }
}
