package fergaral.datetophoto.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ImageView

import java.util.ArrayList

/**
 * Created by fer on 21/07/16.
 */
class RectImageView(context: Context, attrs: AttributeSet) : ImageView(context, attrs) {
    private val boundingBoxes: MutableList<Rect>
    private val paint: Paint

    init {
        boundingBoxes = ArrayList()
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.YELLOW
    }

    fun addBoundingBox(boundingBox: Rect?) {
        if (boundingBox != null)
            boundingBoxes.add(boundingBox)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas) //Draw the image

        for (boundingBox in boundingBoxes)
            canvas.drawRect(boundingBox, paint)
    }
}
