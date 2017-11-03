package fergaral.datetophoto.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.View

/**
 * Created by Fer on 06/10/2017.
 */

object UIUtils {
    fun calculateNoOfColumns(view: View): Int {
        val dpWidth = pxToDp(view.context, view.width).toFloat()
        val scalingFactor = 180
        return Math.ceil((dpWidth / scalingFactor).toDouble()).toInt()
    }

    fun pxToDp(context: Context, px: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT))
    }
}
