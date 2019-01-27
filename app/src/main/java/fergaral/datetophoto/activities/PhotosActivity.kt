package fergaral.datetophoto.activities

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.preference.PreferenceManager
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.scalified.fab.ActionButton
import fergaral.datetophoto.GlideApp
import fergaral.datetophoto.R
import fergaral.datetophoto.fragments.LoadPhotosFragment
import fergaral.datetophoto.fragments.ProgressHeadlessFragment
import fergaral.datetophoto.receivers.ActionCancelReceiver
import fergaral.datetophoto.services.ProcessPhotosService
import fergaral.datetophoto.utils.*
import java.io.File
import java.util.*

/**
 * Created by fer on 18/07/15.
 */

class PhotosActivity : PermissionActivity(), LoadPhotosFragment.TaskCallbacks, ProgressListener {

    private var photosGrid: GridView? = null
    private var selectedPaths: ArrayList<String>? = null
    private var wasFABPressed: Boolean = false
    private var processPhotosBtn: ActionButton? = null
    private var fabSpeedDial1: ActionButton? = null
    private var fabSpeedDial2: ActionButton? = null
    private var coverView: View? = null
    private lateinit var progressLayout: View
    private lateinit var tvProgressStatus: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvProgressActual: TextView
    private lateinit var tvProgressTotal: TextView
    private lateinit var btnProgressCancel: Button
    private lateinit var tvProgressMessage: TextView
    private var cardSpeedDial1: CardView? = null
    private var cardSpeedDial2: CardView? = null
    private var hideNoPhotosTextView: Boolean = false
    private var mSavedInstanceState: Bundle? = null
    private var mIntent: Intent? = null
    private var mImagesToProcess: ArrayList<String>? = null
    private var loadPhotosFragment: LoadPhotosFragment? = null
    private var isRefreshing: Boolean = false
    private var loadingProgBar: View? = null
    private var foldersSpinner: AppCompatSpinner? = null
    private var lastSelectedSpinnerPosition: Int = 0
    var shouldShowLoading: Boolean = false

    private val isNoPhotosScreenShown: Boolean
        get() {
            val noPhotosView = findViewById<View>(R.id.tv_nophotos)
            return noPhotosView != null && noPhotosView.visibility == View.VISIBLE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos)

        val toolbar = findViewById<Toolbar>(R.id.tool_bar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            if (ProcessPhotosService.isRunning) {
                showProgressDialog(false, true)
            }
        }

        val noPhotosTv = findViewById<AppCompatTextView>(R.id.tv_nophotos)

        if (noPhotosTv != null) {
            val drawable = DrawableCompat.wrap(
                    ContextCompat.getDrawable(this, R.drawable.ic_check_circle_black)!!
            )

            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.colorAccent))

            noPhotosTv.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
        }

        if (savedInstanceState != null)
            lastSelectedSpinnerPosition = savedInstanceState.getInt(LAST_SELECTED_SPINNER_POSITION_KEY, 0)

        foldersSpinner = findViewById(R.id.foldersSpinner)
        //nophotosView = (NoPhotosView) findViewById(R.id.noPhotosView);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            return  //The permission screen will be displayed

        PhotoUtils.selectAllFoldersOnFirstUse(this)

        val foldersArray = Utils.getFoldersToProcess(this)
        val folders = ArrayList<String>()

        for (folder in foldersArray)
            if (!folder.trim().isEmpty())
                folders.add(folder)

        folders.sortWith(Comparator { lhs, rhs -> lhs.toLowerCase().compareTo(rhs.toLowerCase()) })

        folders.add(0, "Todas las fotos")
        foldersSpinner!!.adapter = ArrayAdapter(this, R.layout.spinner_row, folders)
        foldersSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (lastSelectedSpinnerPosition != position) {
                    refreshGrid()
                    lastSelectedSpinnerPosition = position
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        loadingProgBar = findViewById(R.id.loading_photos_prog_bar)
        processPhotosBtn = findViewById(R.id.btnProcessSelectedPhotos)
        fabSpeedDial1 = findViewById(R.id.fab_speeddial_action1)
        fabSpeedDial2 = findViewById(R.id.fab_speeddial_action2)
        progressLayout = findViewById(R.id.progress_layout)
        tvProgressStatus = findViewById(R.id.tv_progress_status)
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        tvProgressPercentage = findViewById(R.id.tv_progress_percentage)
        tvProgressActual = findViewById(R.id.tv_progress_actual)
        tvProgressTotal = findViewById(R.id.tv_progress_total)
        tvProgressMessage = findViewById(R.id.tv_progress_message)
        btnProgressCancel = findViewById(R.id.btn_progress_cancel)
        btnProgressCancel.setOnClickListener {
            cancelProgress()
        }
        coverView = findViewById(R.id.coverView)

        coverView!!.setOnClickListener {
            //Pulsar el fondo gris es equivalente a pulsar el botón grande
            clickBigFAB()
        }

        cardSpeedDial1 = findViewById(R.id.card_fab_speeddial_action1)
        cardSpeedDial2 = findViewById(R.id.card_fab_speeddial_action2)

        cardSpeedDial1!!.visibility = View.GONE
        cardSpeedDial2!!.visibility = View.GONE

        //Inicialmente los FABs no están visibles
        processPhotosBtn!!.hide()
        fabSpeedDial1!!.hide()
        fabSpeedDial2!!.hide()

        //Establecemos el onClick del FAB
        processPhotosBtn!!.setOnClickListener { clickBigFAB() }

        fabSpeedDial1!!.setOnClickListener { clickFabSpeedDial1() }

        fabSpeedDial2!!.setOnClickListener { clickFabSpeedDial2() }

        cardSpeedDial1!!.setOnClickListener { clickFabSpeedDial1() }

        cardSpeedDial2!!.setOnClickListener { clickFabSpeedDial2() }

        selectedPaths = ArrayList()
        photosGrid = findViewById(R.id.photos_grid)

        val intent = intent

        //Si es la primera vez que se abre la aplicación, buscamos si ya hay fotos sin fechar en el dispositivo
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val searchPhotos = prefs.getBoolean("searchPhotosFirstUse", true)

        mIntent = intent
        mSavedInstanceState = savedInstanceState

        if (savedInstanceState != null)
            isRefreshing = savedInstanceState.getBoolean(IS_REFRESHING_KEY, true)

        if (searchPhotos) {
            //Mostramos un diálogo preguntando si usaron Date To Photo previamente, pero solo si es la primera vez que
            //se abre la app (porque sería un DialogFragment y se mantiene intacto en rotaciones, etc)
            if (savedInstanceState == null) {
                FirstUseDialogFragment().show(supportFragmentManager, FirstUseDialogFragment::class.java.getSimpleName())
            }
        } else {
            if (!isRefreshing && savedInstanceState != null && savedInstanceState.containsKey(PHOTOS_LIST_KEY)) {
                mImagesToProcess = savedInstanceState.getStringArrayList(PHOTOS_LIST_KEY)

                if (mImagesToProcess != null && mImagesToProcess!!.size > 0) {

                    if (savedInstanceState.containsKey(SELECTED_PHOTOS_KEY))
                        selectedPaths = savedInstanceState.getStringArrayList(SELECTED_PHOTOS_KEY)

                    if (savedInstanceState.containsKey(FAB_PRESSED_KEY))
                        wasFABPressed = savedInstanceState.getBoolean(FAB_PRESSED_KEY)

                    if (wasFABPressed) {
                        fabSpeedDial1!!.show()
                        fabSpeedDial2!!.show()
                        cardSpeedDial1!!.visibility = View.VISIBLE
                        cardSpeedDial2!!.visibility = View.VISIBLE
                        coverView!!.visibility = View.VISIBLE
                        processPhotosBtn!!.setImageResource(R.drawable.ic_clear_24dp)
                    }

                    photosGrid!!.adapter = PhotosAdapter(mImagesToProcess)
                    //processPhotosBtn.show();
                } else {
                    showNoPhotosScreen()
                }
            } else {
                //Iniciamos la AsyncTask (en el Headless Fragment)
                loadPhotosFragment = supportFragmentManager
                        .findFragmentByTag(LOAD_PHOTOS_FRAGMENT_TAG) as LoadPhotosFragment?

                if (loadPhotosFragment == null) {
                    //Todavía no hemos creado el Fragment. Lo creamos e iniciamos la AsyncTask
                    loadPhotosFragment = LoadPhotosFragment()
                    supportFragmentManager
                            .beginTransaction()
                            .add(loadPhotosFragment!!, LOAD_PHOTOS_FRAGMENT_TAG)
                            .commit()
                } else {
                    //El Fragment ya se creó (pudo ser debido a un cambio de rotación)
                    if (isRefreshing)
                        showLoading()
                }
            }

            checkSharedPhotos(savedInstanceState, intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_photos, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                Utils.startActivityCompat(this, Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.action_about -> {
                showAboutDialog()
                return true
            }
            R.id.action_refresh -> {
                refreshGrid()
                return true
            }
            R.id.action_detect_date -> {
                Utils.startActivityCompat(this, Intent(this, DetectDateActivity::class.java))
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPreExecute() {
        showLoading()
        photosGrid!!.visibility = View.INVISIBLE
        //hideNoPhotosScreen();
        processPhotosBtn!!.hide()
    }

    override fun onPostExecute(result: ArrayList<String>) {
        mImagesToProcess = result

        if (!PhotosActivity.IS_PROCESSING) {
            progressLayout.visibility = View.INVISIBLE
            photosGrid!!.visibility = View.VISIBLE
            photosGrid!!.adapter = PhotosAdapter(mImagesToProcess)
        }

        //progressActivity.showContent();

        if (hideNoPhotosTextView)
            hideNoPhotosTextView = false

        hideLoading()

        if (!PhotosActivity.IS_PROCESSING) {
            if (result.size == 0) {
                showNoPhotosScreen()
            } else if (result.size != 0) {
                // processPhotosBtn.show();
            }
        }
    }

    inner class PhotosAdapter(private val images: MutableList<String>?) : BaseAdapter() {

        init {
            if (images != null && images.size != 0 && isNoPhotosScreenShown) {
                hideNoPhotosScreen() {
                    if (shouldShowLoading)
                        showLoading()
                }
            }
        }

        override fun getCount(): Int {
            return images!!.size
        }

        override fun getItem(position: Int): Any {
            return images!![position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var row: View? = convertView

            if (row == null) {
                //No tenemos vista reciclada, la creamos
                row = layoutInflater.inflate(R.layout.grid_row, parent, false)
            }

            val thumbV = row!!.findViewById<View>(R.id.thumb) as TickedImageView

            //Por si la TickedImageView fue reciclada, la desmarcamos
            thumbV.isChecked = selectedPaths!!.contains(images!![position])

            thumbV.setOnClickListener { v ->
                val view = v as TickedImageView

                if (view.isChecked)
                    selectedPaths!!.add(images[position])
                else
                    selectedPaths!!.remove(images[position])
            }

            //Cargamos la imagen
            GlideApp.with(this@PhotosActivity)
                    .load(File(images[position]))
                    .centerCrop()
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                            removeImage((model as File).path)
                            (photosGrid!!.adapter as PhotosAdapter).notifyDataSetChanged()
                            return false
                        }

                        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            //At least one photo has been successfully loaded
                            processPhotosBtn!!.show()
                            return false
                        }
                    })
                    .into(thumbV)

            return row
        }

        fun removeImage(image: String) {
            images!!.remove(image)

            if (images.size == 0)
                showNoPhotosScreen()
        }

        fun getImage(position: Int): String {
            return images!![position]
        }
    }

    private fun clickBigFAB() {
        if (wasFABPressed) {
            //Si el FAB estaba pulsado, ocultamos los otros dos botones
            fabSpeedDial1!!.hide()
            fabSpeedDial2!!.hide()
            cardSpeedDial1!!.visibility = View.GONE
            cardSpeedDial2!!.visibility = View.GONE

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

            hideCoverView()

            //Al estar pulsado previamente, el icono que había era el aspa, así que lo cambiamos por el tick
            processPhotosBtn!!.setImageResource(R.drawable.ic_done_24px)
        } else {
            //Si no estaba pulsado, mostramos los otros dos FAB
            fabSpeedDial1!!.show()
            fabSpeedDial2!!.show()
            cardSpeedDial1!!.visibility = View.VISIBLE
            cardSpeedDial2!!.visibility = View.VISIBLE

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

            showCoverView()

            //Al no estar pulsado previamente, el icono que había era el tick, así que lo cambiamos por el aspa
            processPhotosBtn!!.setImageResource(R.drawable.ic_clear_24dp)
        }

        wasFABPressed = !wasFABPressed
    }

    private fun handleSingleImage(intent: Intent) {
        val imageUri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as Uri
        val selectedPaths = ArrayList<String>()

        selectedPaths.add(imageUri.toString())
        //Lanzamos el servicio de procesar fotos por URI con la foto
        showProgressDialogShare(selectedPaths)
    }

    private fun handleMultipleImages(intent: Intent) {
        val imageUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        val selectedPaths = ArrayList<String>()

        if (imageUris != null) {
            for (uri in imageUris) {
                if (uri != null) {
                    selectedPaths.add(uri.toString())
                }
            }

            //Procesamos las fotos
            showProgressDialogShare(selectedPaths)

            /*//Si existe la ruta local de la URI, añadimos la ruta de la foto a la base de datos
            Intent addPhotosIntent = new Intent(this, RegisterPhotoURIIntoDBService.class);
            addPhotosIntent.putExtra(EXTRA_IMAGE_URI, imageUris);
            startService(addPhotosIntent);*/
        }
    }

    private fun clearSelectedPhotos() {
        //Eliminamos de la lista las fotos que se van a fechar
        val adapter = photosGrid!!.adapter as PhotosAdapter

        for (path in selectedPaths!!) {
            adapter.removeImage(path)
        }

        adapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        PhotosActivity.SHOULD_REFRESH_GRID = false
    }

    override fun onRestart() {
        super.onRestart()

        if (!PhotosActivity.IS_PROCESSING && PhotosActivity.SHOULD_REFRESH_GRID) {
            refreshGrid()

            val selectedFolder = foldersSpinner!!.selectedItem as String
            val folders = ArrayList(Arrays.asList(*Utils.getFoldersToProcess(this)))
            folders.add(0, "Todas las fotos")
            foldersSpinner!!.adapter = ArrayAdapter(this, R.layout.spinner_row, folders)

            val selectedIndex = folders.indexOf(selectedFolder)

            foldersSpinner!!.setSelection(if (selectedIndex != -1) selectedIndex else 0)
        }
    }

    private fun selectAll() {
        val adapter = photosGrid!!.adapter as PhotosAdapter

        for (i in 0 until adapter.count)
            selectedPaths!!.add(adapter.getImage(i))
    }

    private fun showAboutDialog() {
        AboutDialogFragment().show(supportFragmentManager, AboutDialogFragment::class.java!!.getSimpleName())
    }

    class AboutDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            var versionName = ""
            try {
                versionName = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            return MaterialDialog(activity!!)
                    .title(text = "Date To Photo $versionName")
                    .message(text = "")
                    .positiveButton(text = "Aceptar")
                    .icon(R.mipmap.ic_launcher)
        }
    }

    class NoPhotosSelectedDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialDialog(activity!!)
                    .title(text = "No has seleccionado ninguna foto")
                    .message(text = "Selecciona alguna foto para continuar")
                    .positiveButton(text = "Aceptar")
        }
    }

    class FirstUseDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val photosActivity = activity as PhotosActivity
            return MaterialDialog(photosActivity)
                    .title(text = "¿Has usado antes Date To Photo?")
                    .message(text = "Selecciona SÍ si en este dispositivo hay alguna foto fechada previamente con esta aplicación. " + "Si no hay, o no recuerdas haber usado Date To Photo, selecciona NO")
                    .positiveButton(text = "Sí") {
                        it.dismiss()
                        //Indicamos que ya no hay que preguntar más veces si el usuario ha usado antes la aplicación
                        photosActivity.setSearchPhotosFirstUse(false)
                        /*
                            Intent searchPhotosIntent = new Intent(getActivity(), ProgressActivity.class);
                            searchPhotosIntent.putExtra(ProgressActivity.SEARCH_PHOTOS_KEY, true);*/
                        photosActivity.showProgressDialog(true)
                    }
                    .negativeButton(text = "No") {
                        it.dismiss()

                        //Indicamos que ya no hay que preguntar más veces si el usuario ha usado antes la aplicación
                        photosActivity.setSearchPhotosFirstUse(false)

                        photosActivity.createLoadPhotosFragment()
                        photosActivity.checkSharedPhotos(photosActivity.mSavedInstanceState, photosActivity.mIntent)
                    }
                    .cancelable(false)
        }
    }

    class ShareEndDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialDialog(activity!!)
                    .title(text = "El proceso ha finalizado")
                    .message(text = "Como las fotos han sido compartidas desde una aplicación, las fotos con fecha que " +
                            "no procedían del dispositivo (por ejemplo, de la nube) se han guardado en la" +
                            " carpeta DateToPhoto.")
                    .positiveButton(text = "De acuerdo")
        }
    }

    override fun onBackPressed() {
        if (wasFABPressed)
            clickBigFAB()
        else
            finish() //Since it's the first screen of the app, we simply leave it
    }

    private fun refreshGrid() {
        hideNoPhotosScreen() {
            if (shouldShowLoading)
                showLoading()
            val selectedFolderPosition = foldersSpinner!!.selectedItemPosition
            val selectedFolder = foldersSpinner!!.selectedItem as String
            if (loadPhotosFragment != null) {
                if (selectedFolderPosition == 0)
                    loadPhotosFragment!!.refresh()
                else
                    loadPhotosFragment!!.load(selectedFolder)
            } else {
                createLoadPhotosFragment()
                if (selectedFolderPosition == 0)
                    loadPhotosFragment!!.refresh()
                else
                    loadPhotosFragment!!.load(selectedFolder)
            }

            PhotosActivity.SHOULD_REFRESH_GRID = false
            hideNoPhotosTextView = true
        }
    }

    private fun checkSharedPhotos(savedInstanceState: Bundle?, intent: Intent?) {
        //Verificamos si la aplicación se abrió desde el menú de compartir, y si se compartieron 1 o varias fotos
        if (savedInstanceState == null) {
            if (Intent.ACTION_SEND == intent!!.action)
                handleSingleImage(intent)
            else if (Intent.ACTION_SEND_MULTIPLE == intent.action)
                handleMultipleImages(intent)
        }
    }

    private fun checkProgressFragment() {
        val fragment = supportFragmentManager.findFragmentByTag("progress_dialog")

        if (fragment != null) {
            val df = fragment as DialogFragment
            df.dismiss()
            supportFragmentManager.beginTransaction().remove(fragment)
        }
    }

    private fun clickFabSpeedDial1() {
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
        if (selectedPaths == null || selectedPaths!!.size == 0)
            NoPhotosSelectedDialogFragment().show(supportFragmentManager,
                    NoPhotosSelectedDialogFragment::class.java!!.getSimpleName())
        else
            showProgressDialog(false)

        clickBigFAB()
    }

    private fun clickFabSpeedDial2() {
        selectAll()
        clickFabSpeedDial1()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mImagesToProcess != null)
            outState.putStringArrayList(PHOTOS_LIST_KEY, mImagesToProcess
            )

        if (selectedPaths != null)
            outState.putStringArrayList(SELECTED_PHOTOS_KEY, selectedPaths)

        outState.putBoolean(FAB_PRESSED_KEY, wasFABPressed)

        if (loadingProgBar != null)
            outState.putBoolean(IS_REFRESHING_KEY, loadingProgBar!!.visibility == View.VISIBLE)

        outState.putInt(LAST_SELECTED_SPINNER_POSITION_KEY, lastSelectedSpinnerPosition)
        //Dejamos que se guarden los valores por defecto (texto introdcido en un EditText, etc)
        super.onSaveInstanceState(outState)
    }

    fun createLoadPhotosFragment() {
        //Iniciamos la AsyncTask (en el Headless Fragment)
        loadPhotosFragment = supportFragmentManager
                .findFragmentByTag(LOAD_PHOTOS_FRAGMENT_TAG) as LoadPhotosFragment?

        if (loadPhotosFragment == null) {
            //Todavía no hemos creado el Fragment. Lo creamos e iniciamos la AsyncTask
            loadPhotosFragment = LoadPhotosFragment()
            supportFragmentManager
                    .beginTransaction()
                    .add(loadPhotosFragment!!, LOAD_PHOTOS_FRAGMENT_TAG)
                    .commit()
        }
    }

    private fun showNoPhotosScreen() {
        //nophotosView.setVisibility(View.VISIBLE);
        progressLayout.visibility = View.INVISIBLE
        photosGrid!!.visibility = View.GONE

        //Ocultamos el FAB si se estaba mostrando
        if (processPhotosBtn!!.isShown)
            processPhotosBtn!!.hide()

        val noPhotosTv = findViewById<View>(R.id.tv_nophotos) as TextView?

        if (noPhotosTv != null)
            AnimationUtils.showWithCircularReveal(noPhotosTv, this@PhotosActivity)
    }

    private fun hideNoPhotosScreen(listener: () -> Unit) {
        //nophotosView.setVisibility(View.INVISIBLE);
        photosGrid!!.visibility = View.VISIBLE

        val noPhotosTv = findViewById<View>(R.id.tv_nophotos) as TextView?

        if (noPhotosTv != null)
            AnimationUtils.hideWithCircularReveal(noPhotosTv, this@PhotosActivity, listener)
    }

    private fun hideLoading() {
        shouldShowLoading = false

        if (loadingProgBar != null)
            loadingProgBar!!.visibility = View.INVISIBLE
    }

    fun showLoading() {
        progressLayout.visibility = View.INVISIBLE
        val noPhotosTv = findViewById<View>(R.id.tv_nophotos) as TextView
        shouldShowLoading = true

        if (loadingProgBar != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (noPhotosTv == null || noPhotosTv.visibility != View.VISIBLE) {
                    loadingProgBar!!.visibility = View.VISIBLE
                }//If no photos layout is shown, hide effect will take place, no need to hide drawable right now
            } else {
                loadingProgBar!!.visibility = View.VISIBLE
            }
        }
    }

    private fun showProgressDialog(searchPhotos: Boolean, connectToRunningService: Boolean = false) {
        showProgressDialog(searchPhotos, selectedPaths, connectToRunningService, false)
    }

    private fun showProgressDialogShare(selectedPaths: ArrayList<String>) {
        showProgressDialog(false, selectedPaths, false, true)
    }

    private fun showProgressDialog(searchPhotos: Boolean,
                                   selectedPaths: ArrayList<String>?,
                                   connectToRunningService: Boolean = false,
                                   fromShare: Boolean = true
    ) {
        showProgressLayout()
        tvProgressStatus.text = if (searchPhotos) "Buscando fotos..." else "Fechando fotos..."
        tvProgressMessage.text = if (searchPhotos) "Estamos buscando fotos ya fechadas en tu dispositivo"
                                 else "Tus fotos están siendo fechadas. Relájate y haz otras cosas mientras tanto"
        val total = if (searchPhotos) {
            PhotoUtils(this).cameraImages.size
        } else {
            selectedPaths!!.size
        }
        reportTotal(total)
        var headless = supportFragmentManager.findFragmentByTag("headless") as ProgressHeadlessFragment?
        if (headless == null) {
            headless = ProgressHeadlessFragment()

            supportFragmentManager.beginTransaction()
                .add(headless, "headless")
                .commit()

            val fragmentArgs = Bundle()

            fragmentArgs.putBoolean(SEARCH_PHOTOS_KEY, searchPhotos)
            fragmentArgs.putStringArrayList(SELECTED_PATHS_KEY, selectedPaths)
            fragmentArgs.putBoolean(PhotosActivity.ACTION_SHARE_KEY, fromShare)
            fragmentArgs.putBoolean(PhotosActivity.CONNECT_TO_RUNNING_SERVICE_KEY, connectToRunningService)

            headless.arguments = fragmentArgs
        }
    }

    private fun showProgressLayout() {
        photosGrid?.visibility = View.INVISIBLE
        progressLayout.visibility = View.VISIBLE
    }

    private fun setSearchPhotosFirstUse(searchPhotosFirstUse: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putBoolean(SEARCH_PHOTOS_FIRST_USE_KEY, searchPhotosFirstUse)
            .apply()
    }

    private fun showCoverView() {
        //center for reveal animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cx = (processPhotosBtn!!.x + processPhotosBtn!!.width / 2).toInt()

            val cy = (processPhotosBtn!!.y + processPhotosBtn!!.height / 2).toInt()

            val finalRadius = Math.hypot(coverView!!.width.toDouble(), coverView!!.height.toDouble()).toFloat()

            AnimationUtils.showWithCircularReveal(coverView!!, this, cx, cy, finalRadius)
        } else {
            coverView!!.visibility = View.VISIBLE
        }
    }

    private fun hideCoverView() {
        //center for reveal animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val cx = (processPhotosBtn!!.x + processPhotosBtn!!.width / 2).toInt()

            val cy = (processPhotosBtn!!.y + processPhotosBtn!!.height / 2).toInt()

            val initialRadius = Math.hypot(coverView!!.width.toDouble(), coverView!!.height.toDouble()).toFloat()

            AnimationUtils.hideWithCircularReveal(coverView!!, this, cx, cy, initialRadius) {
                if (shouldShowLoading)
                    showLoading()
            }
        } else {
            coverView!!.visibility = View.INVISIBLE
        }
    }

    override fun reportTotal(total: Int) {
        tvProgressTotal.text = total.toString()
        tvProgressActual.text = "0"
        tvProgressPercentage.text = "0%"
    }

    override fun onProgressChanged(progress: Int, actual: Int) {
        tvProgressActual.text = actual.toString()
        tvProgressPercentage.text = "$progress%"
    }

    override fun reportEnd(fromActionShare: Boolean, searchPhotos: Boolean) {
        selectedPaths?.clear()

        if (!searchPhotos) {
            refreshGrid()
        } else {
            val fragment = supportFragmentManager.findFragmentByTag("headless")

            if (fragment != null) {
                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .commit()
            }

            createLoadPhotosFragment()
            checkSharedPhotos(mSavedInstanceState, mIntent)
        }
    }

    private fun cancelProgress() {
        tvProgressStatus.setText(R.string.cancelling)
        sendBroadcast(Intent(this, ActionCancelReceiver::class.java))
    }

    companion object {
        val EXTRA_IMAGE_URI = "fergaraldatetophotoextraimageuri"
        val CONNECT_TO_RUNNING_SERVICE_KEY = "connecttorunningservice"
        var SHOULD_REFRESH_GRID = false
        var IS_PROCESSING = false
        val ACTION_SHARE_KEY = "actionshare"
        val INTENT_ACTION = "fergaral.datetophoto.CANCEL_DIALOG_ACTION"
        val INTENT_QUERY_ACTION = "fergaral.datetophoto.QUERY_SERVICE_ACTION"
        val SEARCH_PHOTOS_KEY = "search_photos"
        val SELECTED_PATHS_KEY = "selected_paths"
        val SEARCH_PHOTOS_FIRST_USE_KEY = "searchPhotosFirstUse"
        val LAST_SELECTED_SPINNER_POSITION_KEY = "lastSelectedSpinnerPos"
        val IS_REFRESHING_KEY = "isRefreshing"
        val LOAD_PHOTOS_FRAGMENT_TAG = "loadPhotosFragment"
        val PHOTOS_LIST_KEY = "photosList"
        val SELECTED_PHOTOS_KEY = "selectedPhotos"
        val FAB_PRESSED_KEY = "fabPressed"
        val PROGRESS_KEY = "fergaral.datetophoto.PROGRESS"
        val PROGRESS_ACTION_SEND = "fergaral.datetophoto.PROGRESS_ACTION_SEND"
        val PROGRESS_ACTION_QUERY = "fergaral.datetophoto.PROGRESS_ACTION_QUERY"
    }
}