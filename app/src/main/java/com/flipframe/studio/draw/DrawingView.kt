package com.flipframe.studio.draw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.flipframe.studio.model.HexColor
import com.flipframe.studio.model.Stroke

/**
 * Full-frame drawing canvas. Renders the entire document (all strokes of the current
 * frame) each pass; kept simple intentionally since a single frame is bounded by the
 * canvas resolution and strokes all rerender in one pass. The active stroke is drawn
 * live from touch events and committed into the frame on ACTION_UP.
 */
class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onStrokeCommitted(stroke: Stroke)
        fun onDocumentTouched()
    }

    var listener: Listener? = null

    private var frameProvider: (() -> List<Stroke>)? = null
    private var widthPx = 640
    private var heightPx = 360
    private var scale = 1f

    private var currentStroke: Stroke? = null
    private var brushColor: Int = Color.BLACK
    private var brushWidth: Float = 6f
    private var eraserMode: Boolean = false

    private val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(60, 255, 255, 255)
    }

    private var docWidth = 640
    private var docHeight = 360
    private var docBackground = "#FFFFFF"

    /** Onion-skin overlays as ARGB bitmaps; drawn faintly behind/after the frame. */
    private var onionPrev: Bitmap? = null
    private var onionNext: Bitmap? = null
    private var onionOpacity = 0.3f

    fun setDocument(w: Int, h: Int, backgroundHex: String) {
        docWidth = w
        docHeight = h
        docBackground = backgroundHex
        postInvalidate()
    }

    fun setOnion(prev: Bitmap?, next: Bitmap?, opacity: Float) {
        onionPrev = prev
        onionNext = next
        onionOpacity = opacity.coerceIn(0f, 1f)
        postInvalidate()
    }

    fun setBrush(color: Int, width: Float, eraser: Boolean) {
        brushColor = color
        brushWidth = width
        eraserMode = eraser
        if (color in 0..0xFFFFFF) {
            settingHint = color
        }
    }

    /** Internal used to remember last brush color when re-enabling pen mode. */
    private var settingHint: Int = Color.BLACK

    fun setStrokesProvider(provider: () -> List<Stroke>) {
        frameProvider = provider
        postInvalidate()
    }

    fun commitCurrentStroke() {
        currentStroke?.let { s ->
            listener?.onStrokeCommitted(s)
            currentStroke = null
            postInvalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val sx = viewToDocX(event.x)
        val sy = viewToDocY(event.y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                currentStroke = Stroke(brushColor, brushWidth, eraserMode)
                currentStroke?.addPoint(sx, sy)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val stroke = currentStroke ?: return true
                for (i in 0 until event.historySize) {
                    stroke.addPoint(viewToDocX(event.getHistoricalX(i)), viewToDocY(event.getHistoricalY(i)))
                }
                stroke.addPoint(sx, sy)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val stroke = currentStroke
                currentStroke = null
                if (stroke != null) {
                    stroke.addPoint(sx, sy)
                    listener?.onStrokeCommitted(stroke)
                }
                invalidate()
                return true
            }
            else -> return super.onTouchEvent(event)
        }
    }

    private fun viewToDocX(vx: Float): Float =
        if (width == 0) vx else vx * docWidth / width

    private fun viewToDocY(vy: Float): Float =
        if (height == 0) vy else vy * docHeight / height

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scale = if (docWidth > 0) w.toFloat() / docWidth else 1f
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.argb(255, 16, 18, 22))
        // Draw working area.
        val left = 0f
        val top = 0f
        val right = width.toFloat()
        val bottom = height.toFloat()
        HexColor.fromHex(docBackground)?.let { bgPaint.color = it }
        canvas.drawRect(left, top, right, bottom, bgPaint)
        canvas.drawRect(left, top, right, bottom, borderPaint)

        // Onion skin (behind current frame content).
        onionPrev?.let { drawOnion(canvas, it) }
        onionNext?.let { drawOnion(canvas, it) }

        val sx = width.toFloat() / docWidth
        val sy = height.toFloat() / docHeight
        frameProvider?.invoke()?.let { strokes ->
            for (s in strokes) s.draw(canvas, sx, sy)
        }
        currentStroke?.draw(canvas, sx, sy)
    }

    private fun drawOnion(canvas: Canvas, bmp: Bitmap) {
        val paint = Paint().apply { alpha = (onionOpacity * 255).toInt() }
        val src = android.graphics.Rect(0, 0, bmp.width, bmp.height)
        val dst = android.graphics.RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawBitmap(bmp, src, dst, paint)
    }
}
