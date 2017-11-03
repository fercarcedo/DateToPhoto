package fergaral.datetophoto.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Created by fer on 16/08/15.
 */
class SameWidthHeightFrameLayout(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}