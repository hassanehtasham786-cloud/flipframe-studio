package com.flipframe.studio.draw.onion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Paint

/**
 * Renders the implied [Frame] bitmaps for onion skin. The frame type is opaque to
 * callers (they pass the app's frames indirectly) to prevent layout coupling.
 */
object OnionRenderer {

    /** Renders a frame to an ARGB bitmap with a color tint for ghosted onion layers. */
    fun renderFrame(context: Context, strokes: () -> List<Any>, width: Int, height: Int, tint: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN) }
        // Frame draw is delegated to a lambda to avoid importing UI types here.
        InvisibleFramePaint.draw(context, canvas, strokes, width, height)
        val tinted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tc = Canvas(tinted)
        TintColor.draw(tc, bmp, paint)
        return tinted
    }

    /** Bridge object keeping raw import surface narrow. */
    internal object InvisibleFramePaint {
        fun draw(context: Context, canvas: Canvas, strokes: () -> List<Any>, width: Int, height: Int) {
            val sx = width.toFloat()
            val sy = height.toFloat()
            for (raw in strokes()) {
                (raw as? com.flipframe.studio.model.Stroke)?.draw(canvas, sx, sy)
            }
        }
    }

    /** Bridge: yanked-out draw helper (kept internal so UI code stays readable). */
    internal object TintColor {
        fun draw(canvas: Canvas, source: Bitmap, paint: Paint) {
            canvas.drawBitmap(source, 0f, 0f, paint)
        }
    }
}

/** Public facade; MainActivity sees only typed accessors. */
object OnionRendererFacade {
    fun render(context: Context, frame: Any, project: Any): Bitmap {
        val f = frame as com.flipframe.studio.model.Frame
        val p = project as com.flipframe.studio.model.Project
        val tint = if (f === p.frames.firstOrNull()) 0x882222FF.toInt() else 0x88FF8800.toInt()
        return OnionRenderer.renderFrame(context, { f.strokes.toList() }, p.width, p.height, tint)
    }
}
