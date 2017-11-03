package fergaral.datetophoto.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import fergaral.datetophoto.R

/**
 * Created by fer on 24/05/15.
 */
class TickedImageView : ImageView {

    var isChecked: Boolean = false
        set(checked) {
            field = checked
            invalidate()
        }
    private var mTickBmp: Bitmap? = null
    private var mDarkerPaint: Paint? = null
    private var onImageClickListener: View.OnClickListener? = null
    private var x: Int = 0
    private var y: Int = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        super.setOnClickListener {
            isChecked = !isChecked

            if (onImageClickListener != null)
                onImageClickListener!!.onClick(this@TickedImageView)
        }

        mDarkerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mDarkerPaint!!.style = Paint.Style.FILL
        mDarkerPaint!!.color = -0x7febdfd0

        mTickBmp = BitmapFactory.decodeResource(resources, R.drawable.ic_done_white_48px)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isChecked) {
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), measuredWidth.toFloat(), mDarkerPaint!!)
            canvas.drawBitmap(mTickBmp!!, x.toFloat(), y.toFloat(), null)
        }
    }

    override fun setOnClickListener(listener: View.OnClickListener?) {
        onImageClickListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)

        x = measuredWidth / 2 - mTickBmp!!.width / 2
        y = measuredWidth / 2 - mTickBmp!!.height / 2
    }
}
