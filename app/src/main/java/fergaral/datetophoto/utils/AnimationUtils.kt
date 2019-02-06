package fergaral.datetophoto.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.view.View
import android.view.ViewAnimationUtils

import fergaral.datetophoto.activities.PhotosActivity

/**
 * Created by fer on 16/06/16.
 */
object AnimationUtils {

    fun showWithCircularReveal(view: View) {
        circularReveal(view, true)
    }

    fun showWithCircularReveal(view: View, cx: Int, cy: Int, radius: Float) {
        circularReveal(view, true, cx, cy, radius)
    }

    fun hideWithCircularReveal(view: View, listener: () -> Unit) {
        circularReveal(view, false, listener)
    }

    fun hideWithCircularReveal(view: View, cx: Int, cy: Int, radius: Float, listener: () -> Unit) {
        circularReveal(view, false, cx, cy, radius, listener)
    }

    private fun circularReveal(view: View, show: Boolean, listener: (() -> Unit)? = null) {
        //get the center for the clipping circle
        val cx = view.width / 2
        val cy = view.height / 2

        //get the final radius for the clipping circle
        val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

        circularReveal(view, show, cx, cy, radius, listener)
    }

    private fun circularReveal(view: View, show: Boolean,
                               cx: Int, cy: Int, radius: Float, listener: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (show) {
                //create the animator for this view (the start radius is 0)
                try {
                    val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, radius)
                    view.visibility = View.VISIBLE
                    anim.start()
                } catch (e: IllegalStateException) {
                    //There was a problem while trying to do reveal effect over the view,
                    //so set it visible without it
                    view.visibility = View.VISIBLE
                }

            } else {
                //create the animator for this view (the final radius is 0)
                try {
                    val anim = ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0f)

                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            view.visibility = View.INVISIBLE
                            listener?.invoke()
                        }
                    })

                    anim.start()
                } catch (e: IllegalStateException) {
                    //There was a problem while trying to do reveal effect over the view,
                    //so set it invisible without it
                    view.visibility = View.INVISIBLE
                    listener?.invoke()
                }

            }
        } else {
            if (show)
                view.visibility = View.VISIBLE
            else {
                view.visibility = View.INVISIBLE
                listener?.invoke()
            }
        }
    }
}
