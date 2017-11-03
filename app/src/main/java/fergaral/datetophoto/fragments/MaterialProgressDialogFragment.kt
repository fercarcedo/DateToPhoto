package fergaral.datetophoto.fragments

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.afollestad.materialdialogs.MaterialDialog
import fergaral.datetophoto.fragments.MaterialProgressDialogFragment.Companion.PROGRESS_ACTION_QUERY
import fergaral.datetophoto.fragments.MaterialProgressDialogFragment.Companion.PROGRESS_ACTION_SEND
import fergaral.datetophoto.fragments.MaterialProgressDialogFragment.Companion.PROGRESS_KEY

/**
 * Created by fer on 11/06/15.
 */
class MaterialProgressDialogFragment : DialogFragment() {

    private var currentProgress: Int = 0

    val total: Int
        get() = arguments!!.getInt(TOTAL_KEY)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        currentProgress = arguments!!.getInt(PROGRESS_KEY, 0)

        Log.d("PROGRESS", currentProgress.toString())

        Log.d("PROGRESS", "onCreateDialog")

//dialog.incrementProgress(currentProgress);

        return MaterialDialog.Builder(activity!!)
                .title("Progreso")
                .content("Procesando fotos...")
                .progress(false, total, true)
                .showListener {
                    //Recibimos el progreso del Service
                    LocalBroadcastManager.getInstance(activity!!).registerReceiver(object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            val progress = intent.getIntExtra(PROGRESS_KEY, currentProgress)

                            if (getCurrentProgress() != -1)
                                incrementProgress(progress - getCurrentProgress())
                            else
                                incrementProgress(progress)

                            currentProgress = progress
                        }
                    }, IntentFilter(PROGRESS_ACTION_SEND))

                    //Pedimos al Service que nos diga el progreso actual
                    val intent = Intent(PROGRESS_ACTION_QUERY)
                    LocalBroadcastManager.getInstance(activity!!).sendBroadcast(intent)
                }
                .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PROGRESS", "onCreate")
    }

    override fun onPause() {
        super.onPause()
        arguments!!.putInt(PROGRESS_KEY, currentProgress)
    }

    fun incrementProgress(by: Int) {
        if (dialog != null) {
            (dialog as MaterialDialog).incrementProgress(by)
            currentProgress = currentProgress + by
        }
    }

    fun getCurrentProgress(): Int {
        return if (dialog != null) (dialog as MaterialDialog).currentProgress else -1

    }

    companion object {

        private val TOTAL_KEY = "total"

        val PROGRESS_KEY = "progress"
        val PROGRESS_ACTION_QUERY = "fergaral.datetophoto.progress.get"
        val PROGRESS_ACTION_SEND = "fergaral.datetophoto.progress.notify"

        fun newInstance(total: Int): MaterialProgressDialogFragment {
            val arguments = Bundle()
            arguments.putInt(TOTAL_KEY, total)
            val dialogFragment = MaterialProgressDialogFragment()
            dialogFragment.arguments = arguments

            return dialogFragment
        }
    }
}
