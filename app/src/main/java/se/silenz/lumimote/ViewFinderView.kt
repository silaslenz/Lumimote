package se.silenz.lumimote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.View


/**
 * Based on https://github.com/mbmb5/Eylca/blob/master/app/src/main/java/mbmb5/extendedcontrolapp/StreamView.java
 */
class ViewFinderView(context: Context?, st: AttributeSet) : SurfaceView(context, st) {
    private var currentImage: Bitmap? = null
    internal var boxes: Array<Rect> = arrayOf()

    internal var viewFinderWidth = 100
    internal var viewFinderHeight = 0


    init {
        setWillNotDraw(false)
        addOnLayoutChangeListener { view: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
            viewFinderWidth = right - left
        }
    }


    fun setCurrentImage(bitmap: Bitmap?) {
        if (bitmap == null)
            return
        currentImage = bitmap
        // compute the height of the view once we know the bitmap's width and height
        if (viewFinderHeight == 0) {
            val factor = viewFinderWidth.toFloat() / bitmap.width.toFloat()
            viewFinderHeight = (currentImage!!.height * factor).toInt()
        }
        currentImage = Bitmap.createScaledBitmap(currentImage!!, viewFinderWidth, viewFinderHeight, false)
        invalidate()
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (currentImage != null) {
            canvas.drawBitmap(currentImage!!, 0f, 0f, Paint())
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.strokeWidth = 2f
        paint.color = android.graphics.Color.YELLOW
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        println(boxes.size)
        for (box in boxes) {
            canvas.drawRect(box, paint)
        }
    }


}


