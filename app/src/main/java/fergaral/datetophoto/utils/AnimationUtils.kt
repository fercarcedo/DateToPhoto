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

    fun showWithCircularReveal(view: View, activity: PhotosActivity) {
        circularReveal(view, activity, true)
    }

    fun showWithCircularReveal(view: View, activity: PhotosActivity, cx: Int, cy: Int, radius: Float) {
        circularReveal(view, activity, true, cx, cy, radius)
    }

    fun hideWithCircularReveal(view: View, activity: PhotosActivity) {
        circularReveal(view, activity, false)
    }

    fun hideWithCircularReveal(view: View, activity: PhotosActivity, cx: Int, cy: Int, radius: Float) {
        circularReveal(view, activity, false, cx, cy, radius)
    }

    private fun circularReveal(view: View, activity: PhotosActivity, show: Boolean) {
        //get the center for the clipping circle
        val cx = view.width / 2
        val cy = view.height / 2

        //get the final radius for the clipping circle
        val radius = Math.hypot(cx.toDouble(), cy.toDouble()).toFloat()

        circularReveal(view, activity, show, cx, cy, radius)
    }

    private fun circularReveal(view: View, activity: PhotosActivity, show: Boolean,
                               cx: Int, cy: Int, radius: Float) {
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

                            if (activity.shouldShowLoading)
                                activity.showLoading()
                        }
                    })

                    anim.start()
                } catch (e: IllegalStateException) {
                    //There was a problem while trying to do reveal effect over the view,
                    //so set it invisible without it
                    view.visibility = View.INVISIBLE

                    if (activity.shouldShowLoading)
                        activity.showLoading()
                }

            }
        } else {
            if (show)
                view.visibility = View.VISIBLE
            else
                view.visibility = View.INVISIBLE
        }
    }
}
