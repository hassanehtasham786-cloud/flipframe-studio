package com.flipframe.studio.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.json.JSONArray
import org.json.JSONObject

/**
 * Root document model: an ordered list of frames with shared canvas settings.
 * Persistence goes through [StringBuilder] text (FlipScript) plus stroke JSON.
 */
class Project(
    var name: String = "My Animation",
    var width: Int = 640,
    var height: Int = 360,
    var fps: Int = 8,
    var frames: MutableList<Frame> = mutableListOf(Frame()),
    /** Hex background color of the project canvas, e.g. "#FFFFFF". */
    var background: String = "#FFFFFF"
) {

    val frameCount: Int get() = frames.size

    fun frameAt(index: Int): Frame {
        require(index >= 0) { "negative frame index" }
        while (frames.size <= index) frames.add(Frame())
        return frames[index]
    }

    fun insertFrame(index: Int, frame: Frame) {
        val i = index.coerceIn(0, frames.size)
        frames.add(i, frame)
    }

    fun addFrame(frame: Frame) {
        frames.add(frame)
    }

    fun removeFrameAt(index: Int) {
        if (frames.size <= 1) return
        if (index in frames.indices) frames.removeAt(index)
    }

    fun moveFrame(from: Int, to: Int) {
        if (from !in frames.indices) return
        val f = frames.removeAt(from)
        val dest = to.coerceIn(0, frames.size)
        frames.add(dest, f)
    }

    fun duplicateFrame(index: Int): Int {
        val copy = frames.getOrNull(index)?.copyStrokes() ?: return -1
        val at = (index + 1).coerceAtMost(frames.size)
        insertFrame(at, copy)
        return at
    }

    fun durationSeconds(): Float = if (fps <= 0) 0f else frames.size.toFloat() / fps

    // ---- JSON ----

    fun metadataJson(): JSONObject = JSONObject().apply {
        put(JsonKeys.FORMAT_VERSION, 1)
        put(JsonKeys.NAME, name)
        put(JsonKeys.WIDTH, width)
        put(JsonKeys.HEIGHT, height)
        put(JsonKeys.FPS, fps)
        put(JsonKeys.BACKGROUND, background)
        put(JsonKeys.FRAMES, JSONArray().apply {
            for (f in frames) put(f.toJson())
        })
    }

    companion object {
        object JsonKeys {
            const val FORMAT_VERSION = "format_version"
            const val NAME = "name"
            const val WIDTH = "width"
            const val HEIGHT = "height"
            const val FPS = "fps"
            const val BACKGROUND = "background"
            const val FRAMES = "frames"
        }

        fun fromJson(o: JSONObject): Project {
            val name = o.optString(JsonKeys.NAME, "My Animation")
            val w = o.optInt(JsonKeys.WIDTH, 640).coerceIn(MIN_WIDTH, MAX_WIDTH)
            val h = o.optInt(JsonKeys.HEIGHT, 360).coerceIn(MIN_HEIGHT, MAX_HEIGHT)
            val fps = o.optInt(JsonKeys.FPS, 8).coerceIn(1, 60)
            val bg = o.optString(JsonKeys.BACKGROUND, "#FFFFFF")
            val p = Project(name, w, h, fps, mutableListOf(), background = bg)
            val arr = o.optJSONArray(JsonKeys.FRAMES)
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { p.addFrame(Frame.fromJson(it)) }
                }
            }
            if (p.frames.isEmpty()) p.frames.add(Frame())
            return p
        }

        const val MIN_WIDTH = 32
        const val MAX_WIDTH = 4096
        const val MIN_HEIGHT = 32
        const val MAX_HEIGHT = 4096
    }

    /** Renders the whole animation onto one bitmap (used by export preview). */
    fun renderFrameBitmap(index: Int, cache: Bitmap? = null): Bitmap {
        val bmp = cache?.takeIf { it.width == width && it.height == height }
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        HexColor.fromHex(background)?.let { canvas.drawColor(it) }
        frameAt(index).render(canvas, 1f, 1f)
        return bmp
    }

    fun markerPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.MAGENTA
    }
}
