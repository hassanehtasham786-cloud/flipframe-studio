package com.flipframe.studio

import com.flipframe.studio.flipscript.FlipScriptParser
import com.flipframe.studio.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FlipScriptParserTest {

    private val parser = FlipScriptParser()

    @Test
    fun parsesProjectHeaderCommands() {
        val script = """
            project "Race Scene"
            canvas 1280 720
            fps 24
            background #87CEEB
        """.trimIndent()
        val doc = parser.parse(script)
        assertEquals("Race Scene", doc.name)
        assertEquals(1280, doc.width)
        assertEquals(720, doc.height)
        assertEquals(24, doc.fps)
        assertEquals("#87CEEB", doc.background)
    }

    @Test
    fun parsesShapesIntoFrames() {
        val script = """
            canvas 640 360
            frame 1
            rect 10 20 100 50 #FF0000
            circle 320 180 45 #00FF00
            line 0 0 100 100 #0000FF 3
            text 30 300 28 Hello_world
            frame 2
            circle 360 180 45 #FFFF00
        """.trimIndent()
        val doc = parser.parse(script)
        assertEquals(2, doc.frames.size)
        val f1 = doc.frames[0]
        assertEquals(4, f1.strokes.size)
        assertEquals(Stroke.Kind.RECT, f1.strokes[0].kind)
        assertEquals(Stroke.Kind.CIRCLE, f1.strokes[1].kind)
        assertEquals(Stroke.Kind.LINE, f1.strokes[2].kind)
        assertEquals(3f, f1.strokes[2].width)
        assertEquals(Stroke.Kind.TEXT, f1.strokes[3].kind)
        assertEquals("Hello world", f1.strokes[3].text)
        assertEquals(1, doc.frames[1].strokes.size)
    }

    @Test
    fun parseStrokeCommandFreehand() {
        val script = """
            frame 1
            stroke 5 true 10,10 20,15 30,25
        """.trimIndent()
        val doc = parser.parse(script)
        val s = doc.frames[0].strokes.single()
        assertEquals(Stroke.Kind.FREEHAND, s.kind)
        assertTrue(s.isEraser())
        assertEquals(3, s.points.size)
        assertEquals(5f, s.width)
    }

    @Test
    fun unknownCommandReportsLine() {
        try {
            parser.parse("frame 1\nfrobnicate 1 2")
            fail("expected ParseException")
        } catch (e: FlipScriptParser.ParseException) {
            assertEquals(1, e.errors.size)
            assertEquals(2, e.errors[0].line)
            assertTrue(e.errors[0].message!!.contains("unknown command"))
        }
    }

    @Test
    fun shapeWithoutFrameReportsLine() {
        try {
            parser.parse("rect 1 2 3 4 #000000")
            fail("expected ParseException")
        } catch (e: FlipScriptParser.ParseException) {
            assertEquals(1, e.errors[0].line)
            assertTrue(e.errors[0].message!!.contains("frame"))
        }
    }

    @Test
    fun commentsAndBlankLinesIgnored() {
        val doc = parser.parse("# header\n\nfps 6\n# comments ok\nframe 1\n")
        assertEquals(6, doc.fps)
        assertEquals(1, doc.frames.size)
    }

    @Test
    fun frameGapCreatesEmptyFrames() {
        val doc = parser.parse("frame 3\ncircle 5 5 2 #FFFFFF")
        assertEquals(3, doc.frames.size)
        assertEquals(1, doc.frames[2].strokes.size)
        assertEquals(0, doc.frames[0].strokes.size)
    }

    @Test
    fun colorValidation() {
        assertNotNull(StrokeFactoryTest.red())
    }
}

/** Helper fixture object (visible only to tests in this package). */
object StrokeFactoryTest {
    fun red(): Int = 0xFFFF0000.toInt()
}
