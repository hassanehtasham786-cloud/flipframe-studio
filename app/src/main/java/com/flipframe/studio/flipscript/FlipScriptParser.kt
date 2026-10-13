package com.flipframe.studio.flipscript

import com.flipframe.studio.model.Frame
import com.flipframe.studio.model.HexColor
import com.flipframe.studio.model.Project
import com.flipframe.studio.model.Stroke

/**
 * Line-based parser for FlipScript, the editable text format of a .flipframe project.
 *
 * Grammar (one command per line, `#` starts a comment):
 * ```
 * # comment
 * project "Name"
 * canvas <width> <height>
 * fps <frames per second>
 * background <#RRGGBB>
 * frame <1-based index>      // starts/continues a frame; jump allowed
 * rect <x> <y> <w> <h> <#RRGGBB>
 * circle <cx> <cy> <r> <#RRGGBB>
 * line <x1> <y1> <x2> <y2> <#RRGGBB> [<width>]
 * text <x> <y> <fontSize> <words> [#RRGGBB]
 * stroke <width> <eraser:true|false> <x,y> ... <x,y>  // freehand double (document coords)
 * ```
 * All coordinates and sizes are document pixel units and may be fractional.
 * Line numbers in errors are 1-based.
 */
class FlipScriptParser {

    /** Result of parsing; keeps commands attached to their source line for error reporting. */
    class Document(
        var name: String = "My Animation",
        var width: Int = 640,
        var height: Int = 360,
        var fps: Int = 8,
        var background: String = "#FFFFFF",
        val frames: MutableList<Frame> = mutableListOf()
    )

    /** A user-visible error: message plus 1-based line number. */
    class ScriptError(message: String, val line: Int) : RuntimeException(message) {
        override fun toString(): String = "Line $line: $message"
    }

    class ParseException(val errors: List<ScriptError>) :
        RuntimeException(errors.joinToString("; ") { it.toString() })

    /**
     * Parses [script] and returns a [Document]; throws [ParseException] listing all
     * errors when a line cannot be understood. Blank lines and `#` comments are skipped.
     */
    fun parse(script: String): Document {
        val doc = Document()
        val errors = mutableListOf<ScriptError>()
        var currentFrame: Frame? = null
        var frameIndex = -1 // last referenced 1-based frame number

        fun ensureFrame(): Frame? {
            return currentFrame
        }

        val lines = script.split('\n')
        for (i in lines.indices) {
            val raw = lines[i]
            val lineNo = i + 1
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val tokens = tokenize(line)
            if (tokens.isEmpty()) continue
            val cmd = tokens[0].lowercase()

            try {
                when (cmd) {
                    "project" -> {
                        val value = line.substringAfter(' ', "").trim()
                        doc.name = unquote(value).takeIf { it.isNotEmpty() } ?: "My Animation"
                    }
                    "canvas" -> {
                        val w = tokens.getOrNull(1)?.toIntOrNull()
                        val h = tokens.getOrNull(2)?.toIntOrNull()
                        if (w == null || h == null) throw ScriptError("canvas needs width and height", lineNo)
                        if (w < Project.MIN_WIDTH || w > Project.MAX_WIDTH) throw ScriptError(
                            "canvas width $w out of range ${Project.MIN_WIDTH}..${Project.MAX_WIDTH}", lineNo
                        )
                        if (h < Project.MIN_HEIGHT || h > Project.MAX_HEIGHT) throw ScriptError(
                            "canvas height $h out of range ${Project.MIN_HEIGHT}..${Project.MAX_HEIGHT}", lineNo
                        )
                        doc.width = w
                        doc.height = h
                    }
                    "fps" -> {
                        val v = tokens.getOrNull(1)?.toIntOrNull()
                        if (v == null || v < 1 || v > 60) throw ScriptError("fps must be 1..60", lineNo)
                        doc.fps = v
                    }
                    "background" -> {
                        val c = tokens.getOrNull(1)
                        val parsed = c?.let { HexColor.fromHex(it) }
                        if (parsed == null) throw ScriptError("background needs #RRGGBB color", lineNo)
                        // Keep original text so future richer colors survive round trips.
                        doc.background = c.uppercase()
                    }
                    "frame" -> {
                        val n = tokens.getOrNull(1)?.toIntOrNull()
                        if (n == null || n < 1) throw ScriptError("frame number must be >= 1", lineNo)
                        // Frames are created in order; a jumped index pads missing slots.
                        while (doc.frames.size < n) doc.frames.add(Frame())
                        frameIndex = n
                        currentFrame = doc.frames[n - 1]
                    }
                    "rect" -> {
                        val f = currentFrame ?: throw ScriptError(
                            "rect must appear inside a frame (use `frame N` first)", lineNo
                        )
                        val x = tokens.getOrNull(1)?.toFloatOrNull()
                        val y = tokens.getOrNull(2)?.toFloatOrNull()
                        val w = tokens.getOrNull(3)?.toFloatOrNull()
                        val h = tokens.getOrNull(4)?.toFloatOrNull()
                        val color = tokens.getOrNull(5)?.let { HexColor.fromHex(it) }
                        if (x == null || y == null || w == null || h == null || color == null) {
                            throw ScriptError("rect needs x y w h color", lineNo)
                        }
                        f.addStroke(Stroke.rect(x, y, w, h, color))
                    }
                    "circle" -> {
                        val f = currentFrame ?: throw ScriptError(
                            "circle must appear inside a frame (use `frame N` first)", lineNo
                        )
                        val cx = tokens.getOrNull(1)?.toFloatOrNull()
                        val cy = tokens.getOrNull(2)?.toFloatOrNull()
                        val r = tokens.getOrNull(3)?.toFloatOrNull()
                        val color = tokens.getOrNull(4)?.let { HexColor.fromHex(it) }
                        if (cx == null || cy == null || r == null || color == null) {
                            throw ScriptError("circle needs cx cy radius color", lineNo)
                        }
                        f.addStroke(Stroke.circle(cx, cy, r, color))
                    }
                    "line" -> {
                        val f = currentFrame ?: throw ScriptError(
                            "line must appear inside a frame (use `frame N` first)", lineNo
                        )
                        val x1 = tokens.getOrNull(1)?.toFloatOrNull()
                        val y1 = tokens.getOrNull(2)?.toFloatOrNull()
                        val x2 = tokens.getOrNull(3)?.toFloatOrNull()
                        val y2 = tokens.getOrNull(4)?.toFloatOrNull()
                        val color = tokens.getOrNull(5)?.let { HexColor.fromHex(it) }
                        val width = tokens.getOrNull(6)?.toFloatOrNull() ?: 4f
                        if (x1 == null || y1 == null || x2 == null || y2 == null || color == null) {
                            throw ScriptError("line needs x1 y1 x2 y2 color [width]", lineNo)
                        }
                        f.addStroke(Stroke.line(x1, y1, x2, y2, color, width))
                    }
                    "text" -> {
                        val f = currentFrame ?: throw ScriptError(
                            "text must appear inside a frame (use `frame N` first)", lineNo
                        )
                        val x = tokens.getOrNull(1)?.toFloatOrNull()
                        val y = tokens.getOrNull(2)?.toFloatOrNull()
                        val size = tokens.getOrNull(3)?.toFloatOrNull()
                        if (x == null || y == null || size == null) {
                            throw ScriptError("text needs x y fontSize", lineNo)
                        }
                        // Text content is everything after the size token up to the color token.
                        val rest = tokens.drop(4)
                        val colorTok = rest.lastOrNull()?.takeIf { it.startsWith("#") }
                        val content = if (colorTok != null && rest.size >= 2) {
                            rest.subList(0, rest.size - 1).joinToString(" ")
                        } else {
                            rest.joinToString(" ")
                        }
                        if (content.isBlank()) throw ScriptError("text needs some words", lineNo)
                        val color = (colorTok?.let { HexColor.fromHex(it) }) ?: 0xFF111111.toInt()
                        // Text does not rotate; underscores become spaces when drawn.
                        f.addStroke(Stroke.text(x, y, size, content, color))
                    }
                    "stroke" -> {
                        val f = currentFrame ?: throw ScriptError(
                            "stroke must appear inside a frame (use `frame N` first)", lineNo
                        )
                        val width = tokens.getOrNull(1)?.toFloatOrNull()
                        val eraserTok = tokens.getOrNull(2)?.lowercase()
                        if (width == null || eraserTok == null) {
                            throw ScriptError("stroke needs width eraser then points", lineNo)
                        }
                        val eraser = eraserTok == "true" || eraserTok == "1"
                        val start = 3
                        if (tokens.size <= start) throw ScriptError("stroke needs at least one point", lineNo)
                        val s = Stroke(0xFF222222.toInt(), width, eraser)
                        for (t in tokens.subList(start, tokens.size)) {
                            val pair = t.split(',')
                            val x = pair.getOrNull(0)?.toFloatOrNull()
                            val y = pair.getOrNull(1)?.toFloatOrNull()
                            if (x == null || y == null) throw ScriptError("stroke point must be x,y", lineNo)
                            s.addPoint(x, y)
                        }
                        f.addStroke(s)
                    }
                    else -> throw ScriptError("unknown command \"$cmd\"", lineNo)
                }
            } catch (e: ScriptError) {
                errors.add(e)
            }
        }

        if (errors.isNotEmpty()) throw ParseException(errors)
        if (doc.frames.isEmpty()) doc.frames.add(Frame())
        return doc
    }

    private fun tokenize(line: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == ' ' || c == '\t' -> i++
                c == '"' -> {
                    val end = line.indexOf('"', i + 1)
                    if (end == -1) {
                        out.add(line.substring(i + 1))
                        i = line.length
                    } else {
                        out.add(line.substring(i + 1, end))
                        i = end + 1
                    }
                }
                else -> {
                    var j = i
                    while (j < line.length && line[j] != ' ' && line[j] != '\t') j++
                    out.add(line.substring(i, j))
                    i = j
                }
            }
        }
        return out
    }

    private fun unquote(value: String): String = value.removeSurrounding("\"")

    companion object {
        /** Suggested keywords for editor assist (kept synced with the parser). */
        val KEYWORDS = listOf(
            "project", "canvas", "fps", "background", "frame",
            "rect", "circle", "line", "text", "stroke"
        )
    }
}
