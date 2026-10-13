package com.flipframe.studio

import com.flipframe.studio.flipscript.FlipScriptParser
import com.flipframe.studio.flipscript.FlipScriptRenderer
import com.flipframe.studio.model.Frame
import com.flipframe.studio.model.Project
import com.flipframe.studio.model.Stroke
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSerializationTest {

    @Test
    fun projectJsonRoundTripPreservesStrokes() {
        val p = Project(name = "Test", width = 640, height = 360, fps = 12)
        val f = p.frameAt(0)
        f.addStroke(Stroke.rect(10f, 20f, 30f, 40f, 0xFFFF0000.toInt()))
        f.addStroke(Stroke.line(0f, 0f, 5f, 5f, 0xFF00FF00.toInt(), 7f))
        f.addStroke(Stroke.circle(50f, 60f, 20f, 0xFF0000FF.toInt()))
        f.addStroke(Stroke.text(1f, 2f, 32f, "Hello world", 0xFF000000.toInt()))

        val json = p.metadataJson().toString()
        val restored = Project.fromJson(JSONObject(json))

        assertEquals(p.name, restored.name)
        assertEquals(p.width, restored.width)
        assertEquals(p.height, restored.height)
        assertEquals(p.fps, restored.fps)
        assertEquals(p.frames.size, restored.frames.size)
        val rf = restored.frameAt(0)
        assertEquals(f.strokes.size, rf.strokes.size)
        for (i in f.strokes.indices) {
            val a = f.strokes[i]
            val b = rf.strokes[i]
            assertEquals(a.kind, b.kind)
            assertEquals(a.color, b.color)
            assertEquals(a.width, b.width)
            assertEquals(a.points.size, b.points.size)
            assertEquals(a.text, b.text)
        }
    }

    @Test
    fun scriptRenderParsesBack() {
        val p = Project(name = "Loop", width = 480, height = 320, fps = 10)
        p.frameAt(0).addStroke(Stroke.rect(1f, 2f, 3f, 4f, 0xFF123456.toInt()))
        p.frameAt(1).addStroke(Stroke.text(5f, 6f, 20f, "Abc", 0xFF654321.toInt()))

        val script = FlipScriptRenderer.render(p)
        val doc = FlipScriptParser().parse(script)
        assertEquals("Loop", doc.name)
        assertEquals(480, doc.width)
        assertEquals(320, doc.height)
        assertEquals(10, doc.fps)
        assertEquals(2, doc.frames.size)
        assertEquals(Stroke.Kind.RECT, doc.frames[0].strokes[0].kind)
        assertEquals(Stroke.Kind.TEXT, doc.frames[1].strokes[0].kind)
        assertEquals("Abc", doc.frames[1].strokes[0].text)
    }

    @Test
    fun duplicateFrameCopiesStrokes() {
        val p = Project()
        p.frameAt(0).addStroke(Stroke.line(0f, 0f, 1f, 1f, 0xFF000000.toInt(), 2f))
        val at = p.duplicateFrame(0)
        assertEquals(1, at)
        assertEquals(2, p.frames.size)
        assertEquals(1, p.frames[1].strokes.size)
    }

    @Test
    fun deleteLastFrameKeepsOne() {
        val p = Project()
        p.removeFrameAt(0)
        assertEquals(1, p.frames.size)
    }

    @Test
    fun moveFrameReorders() {
        val p = Project()
        p.frameAt(0).addStroke(Stroke.rect(0f, 0f, 1f, 1f, 0xFF000000.toInt()))
        p.addFrame(Frame())
        p.moveFrame(0, 1)
        assertTrue(p.frames[1].strokes.isNotEmpty())
    }

    @Test
    fun eraserStrokeSurvivesRoundTrip() {
        val p = Project()
        val s = Stroke(0xFF000000.toInt(), 8f, eraser = true)
        s.addPoint(1f, 1f)
        s.addPoint(9f, 9f)
        p.frameAt(0).addStroke(s)
        val json = p.metadataJson().toString()
        val restored = Project.fromJson(JSONObject(json))
        val rs = restored.frameAt(0).strokes.single()
        assertTrue(rs.isEraser())
        assertEquals(2, rs.points.size)
        assertNotNull(Stroke.fromJson(rs.toJson()))
    }
}
