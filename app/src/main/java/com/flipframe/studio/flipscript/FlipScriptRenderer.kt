package com.flipframe.studio.flipscript

import com.flipframe.studio.model.Frame
import com.flipframe.studio.model.HexColor
import com.flipframe.studio.model.Project
import com.flipframe.studio.model.Stroke
import kotlin.math.max
import kotlin.math.min

/**
 * Renders a [Project] back into FlipScript text. Freehand strokes become `line`
 * segment chains so the round trip stays readable and editable by coding agents.
 */
object FlipScriptRenderer {

    fun render(project: Project): String {
        val sb = StringBuilder()
        sb.append("project \"").append(project.name.replace("\"", "'")).append("\"\n")
        sb.append("canvas ").append(project.width).append(' ').append(project.height).append('\n')
        sb.append("fps ").append(project.fps).append('\n')
        sb.append("background ").append(HexColor.toHex(parseOr(project.background, 0xFFFFFFFF.toInt()))).append('\n')
        for (i in project.frames.indices) {
            sb.append("frame ").append(i + 1).append('\n')
            appendFrame(sb, project.frames[i], project.width)
        }
        return sb.toString()
    }

    private fun appendFrame(sb: StringBuilder, frame: Frame, docWidth: Int) {
        for (s in frame.strokes) {
            when (s.kind) {
                Stroke.Kind.RECT -> {
                    val a = s.points.getOrNull(0) ?: continue
                    val b = s.points.getOrNull(1) ?: continue
                    sb.append("rect ").append(fmt(min(a.x, b.x))).append(' ')
                        .append(fmt(min(a.y, b.y))).append(' ')
                        .append(fmt(max(a.x, b.x) - min(a.x, b.x))).append(' ')
                        .append(fmt(max(a.y, b.y) - min(a.y, b.y))).append(' ')
                        .append(HexColor.toHex(s.color)).append('\n')
                }
                Stroke.Kind.CIRCLE -> {
                    val c = s.points.getOrNull(0) ?: continue
                    sb.append("circle ").append(fmt(c.x)).append(' ')
                        .append(fmt(c.y)).append(' ')
                        .append(fmt(s.width)).append(' ')
                        .append(HexColor.toHex(s.color)).append('\n')
                }
                Stroke.Kind.LINE -> {
                    val a = s.points.getOrNull(0) ?: continue
                    val b = s.points.getOrNull(1) ?: continue
                    sb.append("line ").append(fmt(a.x)).append(' ')
                        .append(fmt(a.y)).append(' ')
                        .append(fmt(b.x)).append(' ')
                        .append(fmt(b.y)).append(' ')
                        .append(HexColor.toHex(s.color))
                    if (s.width != 4f) sb.append(' ').append(fmt(s.width))
                    sb.append('\n')
                }
                Stroke.Kind.TEXT -> {
                    val a = s.points.getOrNull(0) ?: continue
                    // Preserve the parse-time text: spaces back to underscores.
                    sb.append("text ").append(fmt(a.x)).append(' ')
                        .append(fmt(a.y)).append(' ')
                        .append(fmt(s.width)).append(' ')
                        .append(s.text.replace(' ', '_'))
                        .append(' ').append(HexColor.toHex(s.color)).append('\n')
                }
                Stroke.Kind.FREEHAND -> {
                    // Serialize freehand strokes as a polyline `stroke` command.
                    if (s.points.isEmpty()) continue
                    sb.append("stroke ").append(fmt(s.width))
                        .append(' ').append(if (s.eraser) "true" else "false")
                    for (p in s.points) {
                        sb.append(' ').append(fmt(p.x)).append(',').append(fmt(p.y))
                    }
                    sb.append('\n')
                }
            }
        }
    }

    /** Two decimal places max, no trailing ".0"; keeps scripts compact and stable. */
    private fun fmt(v: Float): String {
        val r = (v * 100).let { kotlin.math.round(it) } / 100f
        return if (r == r.toLong().toFloat()) r.toLong().toString() else r.toString()
    }

    private fun parseOr(hex: String, fallback: Int): Int = HexColor.fromHex(hex) ?: fallback
}
