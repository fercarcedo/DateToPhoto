package fergaral.datetophoto.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Build;
import android.view.View;
import android.view.ViewAnimationUtils;

import fergaral.datetophoto.activities.PhotosActivity;

/**
 * Created by fer on 16/06/16.
 */
public final class AnimationUtils {
    private AnimationUtils() {}

    public static void showWithCircularReveal(View view, PhotosActivity activity) {
        circularReveal(view, activity, true);
    }

    public static void showWithCircularReveal(View view, PhotosActivity activity, int cx, int cy, float radius) {
        circularReveal(view, activity, true, cx, cy, radius);
    }

    public static void hideWithCircularReveal(View view, PhotosActivity activity) {
        circularReveal(view, activity, false);
    }

    public static void hideWithCircularReveal(View view, PhotosActivity activity, int cx, int cy, float radius) {
        circularReveal(view, activity, false, cx, cy, radius);
    }

    private static void circularReveal(final View view, final PhotosActivity activity, boolean show) {
        //get the center for the clipping circle
        int cx = view.getWidth() / 2;
        int cy = view.getHeight() / 2;

        //get the final radius for the clipping circle
        float radius = (float) Math.hypot(cx, cy);

        circularReveal(view, activity, show, cx, cy, radius);
    }

    private static void circularReveal(final View view, final PhotosActivity activity, boolean show,
                                       int cx, int cy, float radius) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (show) {
                //create the animator for this view (the start radius is 0)
                try {
                    Animator anim =
                            ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, radius);

                    view.setVisibility(View.VISIBLE);
                    anim.start();
                }catch(IllegalStateException e) {
                    //There was a problem while trying to do reveal effect over the view,
                    //so set it visible without it
                    view.setVisibility(View.VISIBLE);
                }
            } else {
                //create the animator for this view (the final radius is 0)
                try {
                    Animator anim =
                            ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0);

                    anim.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            view.setVisibility(View.INVISIBLE);

                            if (activity.shouldShowLoading)
                                activity.showLoading();
                        }
                    });

                    anim.start();
                }catch(IllegalStateException e) {
                    //There was a problem while trying to do reveal effect over the view,
                    //so set it invisible without it
                    view.setVisibility(View.INVISIBLE);

                    if(activity.shouldShowLoading)
                        activity.showLoading();
                }
            }
        }else{
            if(show)
                view.setVisibility(View.VISIBLE);
            else
                view.setVisibility(View.INVISIBLE);
        }
    }
}
