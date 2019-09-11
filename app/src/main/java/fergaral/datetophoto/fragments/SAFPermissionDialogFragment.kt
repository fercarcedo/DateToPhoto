package fergaral.datetophoto.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fergaral.datetophoto.R

class SAFPermissionDialogFragment : DialogFragment() {
    interface SAFPermissionDialogListener {
        fun onCheckSAFPermission(folders: Array<String>)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val folders = requireArguments()[FOLDERS_KEY] as Array<String>
        return MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.permission)
                .setMessage(R.string.give_permission_to_overwrite_photos)
                .setPositiveButton(R.string.accept) { dialogInterface, i ->
                    dismissAllowingStateLoss()
                    (parentFragment as SAFPermissionDialogListener).onCheckSAFPermission(folders)
                }
                .setNegativeButton(R.string.cancel) { dialogInterface, i ->
                    dismissAllowingStateLoss()
                }
                .create()
    }

    companion object {
        const val TAG = "SAFPermissionDialogFragment"
        const val FOLDERS_KEY = "folders"

        @JvmStatic
        fun newInstance(folders: Array<String>) = SAFPermissionDialogFragment().apply {
            arguments = Bundle().apply {
                putStringArray(FOLDERS_KEY, folders)
            }
        }
    }
}