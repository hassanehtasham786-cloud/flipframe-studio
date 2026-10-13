package com.flipframe.studio.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/**
 * Information about a single drawn frame. Strokes are the editable source of truth;
 * the bitmap cache renders them lazily.
 */
class Frame(val strokes: MutableList<Stroke> = mutableListOf()) {
    /** Lazily rendered cache of this frame on the current layer. */
    var cachedThumbnail: Bitmap? = null
        private set

    fun addStroke(s: Stroke) {
        strokes.add(s)
    }

    fun addStrokeFront(s: Stroke) {
        strokes.add(0, s)
    }

    /** Copies all strokes into a new Frame (used by duplicate). */
    fun copyStrokes(): Frame {
        val f = Frame()
        for (s in strokes) f.addStroke(s)
        return f
    }

    fun removeStrokeAt(index: Int) {
        if (index in strokes.indices) strokes.removeAt(index)
    }

    fun clearStrokes() {
        strokes.clear()
    }

    fun isEmpty(): Boolean = strokes.isEmpty()

    /** Assigns the cached bitmap once rendering finished (used by canvas tick). */
    fun badgeThumbnail(bitmap: Bitmap) {
        cachedThumbnail = bitmap
    }

    /** Draws the full frame onto [canvas] in document coordinates (scaled by sx/sy). */
    fun render(canvas: Canvas, sx: Float, sy: Float) {
        for (s in strokes) s.draw(canvas, sx, sy)
    }

    // ---- JSON ----

    fun toJson(): JSONObject = JSONObject().apply {
        put(JsonKeys.STROKES, JSONArray().apply {
            for (s in strokes) put(s.toJson())
        })
    }

    companion object {
        object JsonKeys {
            const val STROKES = "strokes"
        }

        fun fromJson(o: JSONObject): Frame {
            val f = Frame()
            val arr = o.optJSONArray(JsonKeys.STROKES)
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val so = arr.optJSONObject(i) ?: continue
                    Stroke.fromJson(so)?.let { f.addStroke(it) }
                }
            }
            return f
        }

        /** Opens the given file as a PNG into a Bitmap; used by frame thumbnail import. */
        fun emptyBitmap(w: Int, h: Int): Bitmap =
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.TRANSPARENT) }
    }
}
