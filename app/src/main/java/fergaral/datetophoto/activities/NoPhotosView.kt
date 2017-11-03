package fergaral.datetophoto.activities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Created by fer on 6/03/16.
 */
class NoPhotosView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var paint: Paint? = null

    init {
        init()
    }

    private fun init() {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint!!.style = Paint.Style.FILL
        paint!!.color = Color.GREEN
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(0f, 0f, Math.min(measuredWidth / 2, measuredHeight / 2).toFloat(), paint!!)
    }
}
