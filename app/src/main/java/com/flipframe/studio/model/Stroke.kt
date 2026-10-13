package com.flipframe.studio.model

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

/**
 * A single drawn path or vector shape. Points keep both touch pressure and the stroke
 * width at recording time so variable-width pens can be added later without changing
 * the format.
 */
class Stroke(
    val color: Int,
    var width: Float,
    /** True when this stroke erases what is already drawn on the layer. */
    var eraser: Boolean,
    val points: MutableList<Point> = mutableListOf()
) {
    /** Shape kinds a Stroke can represent; shapes store geometry inside [points]. */
    enum class Kind { FREEHAND, RECT, CIRCLE, LINE, TEXT }

    var kind: Kind = Kind.FREEHAND
        private set

    /** Text payload for [Kind.TEXT]; underscores were restored to spaces at parse time. */
    var text: String = ""
        private set

    class Point(val x: Float, val y: Float, val pressure: Float)

    fun addPoint(x: Float, y: Float, pressure: Float = 1f) {
        val last = points.lastOrNull()
        if (last != null && abs(last.x - x) < 1f && abs(last.y - y) < 1f) return
        points.add(Point(x, y, pressure))
    }

    /** Draws the stroke onto [canvas]. [scaleX]/[scaleY] map document coordinates to pixels. */
    fun draw(canvas: Canvas, scaleX: Float, scaleY: Float) {
        if (points.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = this@Stroke.color
        }
        if (eraser) {
            paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        }
        val path = Path()
        when (kind) {
            Kind.RECT -> {
                val a = points[0]
                val b = points[1]
                path.addRect(
                    minOf(a.x, b.x) * scaleX, minOf(a.y, b.y) * scaleY,
                    maxOf(a.x, b.x) * scaleX, maxOf(a.y, b.y) * scaleY,
                    Path.Direction.CW
                )
                canvas.drawPath(path, paint)
            }
            Kind.CIRCLE -> {
                val c = points[0]
                val r = width
                canvas.drawCircle(c.x * scaleX, c.y * scaleY, r, paint)
            }
            else -> {
                val first = points.first()
                path.moveTo(first.x * scaleX, first.y * scaleY)
                if (points.size == 1) {
                    path.lineTo(first.x * scaleX + 0.1f, first.y * scaleY + 0.1f)
                } else {
                    for (i in 1 until points.size) {
                        val p = points[i]
                        path.lineTo(p.x * scaleX, p.y * scaleY)
                    }
                }
                canvas.drawPath(path, paint)
                if (kind == Kind.TEXT) {
                    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = this@Stroke.color
                        textSize = width * scaleX
                    }
                    canvas.drawText(text, first.x * scaleX, first.y * scaleY, fill)
                }
            }
        }
    }

    fun isEraser(): Boolean = eraser

    // ---- Serialization (org.json, available on every Android device) ----

    fun toJson(): JSONObject = JSONObject().apply {
        put(HexKeys.COLOR, HexColor.toHex(color))
        put(HexKeys.WIDTH, width.toDouble())
        put(HexKeys.ERASER, eraser)
        put(HexKeys.KIND, kind.name.lowercase())
        if (kind == Kind.TEXT) put(HexKeys.TEXT, text)
        if (kind == Kind.CIRCLE) put(HexKeys.RADIUS, width.toDouble())
        put(HexKeys.POINTS, JSONArray().apply {
            for (p in points) put(
                JSONArray().apply {
                    put((p.x * 100).toInt() / 100.0)
                    put((p.y * 100).toInt() / 100.0)
                    put((p.pressure * 100).toInt() / 100.0)
                }
            )
        })
    }

    companion object {
        object HexKeys {
            const val COLOR = "color"
            const val WIDTH = "width"
            const val ERASER = "eraser"
            const val POINTS = "points"
            const val KIND = "kind"
            const val TEXT = "text"
            const val RADIUS = "radius"
        }

        // ---- Shape factories used by the FlipScript parser ----

        fun rect(x: Float, y: Float, w: Float, h: Float, color: Int): Stroke {
            val s = Stroke(color, 4f, false)
            s.kind = Kind.RECT
            s.addPoint(x, y)
            s.addPoint(x + w, y + h)
            return s
        }

        fun circle(cx: Float, cy: Float, r: Float, color: Int): Stroke {
            val s = Stroke(color, 4f, false)
            s.kind = Kind.CIRCLE
            s.addPoint(cx, cy)
            s.width = r
            return s
        }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, width: Float): Stroke {
            val s = Stroke(color, width.coerceAtLeast(1f), false)
            s.kind = Kind.LINE
            s.addPoint(x1, y1)
            s.addPoint(x2, y2)
            return s
        }

        fun text(x: Float, y: Float, size: Float, content: String, color: Int): Stroke {
            val s = Stroke(color, size.coerceAtLeast(4f), false)
            s.kind = Kind.TEXT
            s.addPoint(x, y)
            s.text = content.replace('_', ' ')
            return s
        }

        fun fromJson(o: JSONObject): Stroke? {
            val color = HexColor.fromHex(o.optString(HexKeys.COLOR)) ?: return null
            val eraser = o.optBoolean(HexKeys.ERASER, false)
            val s = Stroke(color, 6f, eraser)
            val kind = when (o.optString(HexKeys.KIND, "freehand")) {
                "rect" -> Kind.RECT
                "circle" -> Kind.CIRCLE
                "line" -> Kind.LINE
                "text" -> Kind.TEXT
                else -> Kind.FREEHAND
            }
            s.setKindInternal(kind)
            s.width = o.optDouble(HexKeys.WIDTH, 6.0).toFloat().coerceAtLeast(1f)
            if (kind == Kind.CIRCLE) {
                s.width = o.optDouble(HexKeys.RADIUS, s.width.toDouble()).toFloat().coerceAtLeast(1f)
            }
            if (kind == Kind.TEXT) s.text = o.optString(HexKeys.TEXT)
            val arr = o.optJSONArray(HexKeys.POINTS) ?: return s
            for (i in 0 until arr.length()) {
                val a = arr.optJSONArray(i) ?: continue
                if (a.length() >= 2) {
                    s.addPoint(
                        a.optDouble(0).toFloat(),
                        a.optDouble(1).toFloat(),
                        a.optDouble(2, 1.0).toFloat()
                    )
                }
            }
            if (kind == Kind.TEXT && s.points.isEmpty()) return null
            return s
        }
    }

    /** Package-visible mutator for deserialization only. */
    private fun setKindInternal(k: Kind) {
        kind = k
    }
}

/** Utilities for parsing and writing #RRGGBB color strings used by FlipScript and JSON. */
object HexColor {
    /** Parses "#RRGGBB" or "#AARRGGBB"; returns null when invalid. Never throws. */
    fun fromHex(text: String): Int? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("#")) return null
        val hex = trimmed.substring(1)
        return when (hex.length) {
            6 -> try {
                // Alpha of 255 enables proper hand colors.
                0xFF000000.toInt() or hex.toLong(16).toInt()
            } catch (e: NumberFormatException) {
                null
            }
            8 -> try {
                hex.toLong(16).toInt()
            } catch (e: NumberFormatException) {
                null
            }
            else -> null
        }
    }

    fun toHex(color: Int): String = String.format("#%08X", color)
}

/** Utility helpers for reading legacy string-based stroke JSON (kept for round-trip tests). */
object StrokeJson {
    private const val SELF_COLOR = "self_color"
    private const val SELF_ERASER = "self_eraser"
    private const val SELF_WIDTH = "self_width"

    /**
     * Used by serialization tests to double-check metadata fields exist after a save.
     */
    fun hasMetadata(o: JSONObject): Boolean =
        o.has(SELF_COLOR) || o.has(SELF_ERASER) || o.has(SELF_WIDTH)

    fun minOf(a: Int, b: Int): Int = kotlin.math.min(a, b)
}
