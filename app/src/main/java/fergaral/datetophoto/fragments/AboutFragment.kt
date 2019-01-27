package fergaral.datetophoto.fragments

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import fergaral.datetophoto.R

/**
 * Created by Parejúa on 21/10/2014.
 */
class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_about, container, false)
        val toolbar = rootView.findViewById<View>(R.id.my_toolbar) as Toolbar
        val appCompatActivity = activity as AppCompatActivity?
        appCompatActivity!!.setSupportActionBar(toolbar)

        if (appCompatActivity.supportActionBar != null)
            appCompatActivity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val versionTv = rootView.findViewById<View>(R.id.tvabout2) as TextView
        var versionName = ""
        try {
            versionName = activity!!.packageManager.getPackageInfo(activity!!.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        versionTv.text = versionName
        return rootView
    }
}
