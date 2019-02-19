package se.silenz.lumimote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceView
import android.view.View

/**
 * Based on https://github.com/mbmb5/Eylca/blob/master/app/src/main/java/mbmb5/extendedcontrolapp/StreamView.java
 */
class ViewFinderView(context: Context?, st: AttributeSet) : SurfaceView(context, st) {
    private var currentImage: Bitmap? = null

    private var ViewFinderWidth= 100
    private var viewFinderHeight = 0


    init {
        setWillNotDraw(false)
        addOnLayoutChangeListener { view: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
            ViewFinderWidth = right - left
        }
    }


    fun setCurrentImage(bitmap: Bitmap?) {
        if (bitmap == null)
            return
        currentImage = bitmap
        // compute the height of the view once we know the bitmap's width and height
        if (viewFinderHeight == 0) {
            val factor = ViewFinderWidth.toFloat() / bitmap.width.toFloat()
            viewFinderHeight = (currentImage!!.height * factor).toInt()
        }
        currentImage = Bitmap.createScaledBitmap(currentImage!!, ViewFinderWidth, viewFinderHeight, false)
        invalidate()
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (currentImage != null) {
            canvas.drawBitmap(currentImage!!, 0f, 0f, Paint())
        }
    }


}
