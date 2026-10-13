package com.flipframe.studio

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flipframe.studio.draw.DrawingView
import com.flipframe.studio.draw.UndoStack
import com.flipframe.studio.draw.onion.OnionRendererFacade
import com.flipframe.studio.model.FlipDocument
import com.flipframe.studio.model.Project
import com.flipframe.studio.model.Stroke
import com.flipframe.studio.project.ProjectFile
import com.flipframe.studio.ui.dialog.ColorDialog
import com.flipframe.studio.ui.dialog.BrushDialog
import com.flipframe.studio.ui.dialog.FpsDialog
import com.flipframe.studio.ui.dialog.ProjectPickerDialog

class MainActivity : AppCompatActivity(), DrawingView.Listener {

    private lateinit var doc: FlipDocument
    private lateinit var undo: UndoStack
    private lateinit var canvas: DrawingView

    private var currentFrame = 0
    private var playing = false
    private var brushColor = Color.BLACK
    private var brushWidth = 6f
    private var eraser = false
    private var onion = true
    private var dirty = false

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!playing) return
            currentFrame = (currentFrame + 1) % doc.project.frameCount
            refreshCanvas()
            handler.postDelayed(this, (1000f / doc.project.fps).toLong())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        doc = FlipDocument(Project())
        undo = UndoStack(doc.project)
        canvas = findViewById(R.id.canvas)
        canvas.listener = this
        wireUi()
        refreshCanvas()
        refreshTitle()
        showStatus("Ready - draw on the canvas, then Save Project to keep your work")
    }

    private fun project(): Project = doc.project

    private fun wireUi() {
        btn(R.id.btn_color).setOnClickListener { ColorDialog.show(this, brushColor) { c -> brushColor = c; pushBrush() } }
        btn(R.id.btn_brush).setOnClickListener { BrushDialog.show(this, brushWidth) { w -> brushWidth = w; pushBrush() } }
        btn(R.id.btn_eraser).setOnClickListener {
            eraser = !eraser
            pushBrush()
            toast(if (eraser) "Eraser ON" else "Pen ON")
        }
        btn(R.id.btn_undo).setOnClickListener { if (undo.undoLast()) { refreshCanvas(); markDirty("Undo") } else toast("Nothing to undo") }
        btn(R.id.btn_redo).setOnClickListener { if (undo.redoLast()) { refreshCanvas(); markDirty("Redo") } else toast("Nothing to redo") }
        btn(R.id.btn_clear).setOnClickListener {
            val f = project().frameAt(currentFrame)
            f.clearStrokes()
            undo.clear()
            refreshCanvas()
            markDirty("Cleared frame ${currentFrame + 1}")
        }
        btn(R.id.btn_prev).setOnClickListener { step(-1) }
        btn(R.id.btn_next).setOnClickListener { step(1) }
        btn(R.id.btn_play).setOnClickListener { togglePlay() }
        btn(R.id.btn_duplicate).setOnClickListener {
            val at = project().duplicateFrame(currentFrame)
            if (at >= 0) {
                currentFrame = at
                refreshCanvas()
                refreshTitle()
                markDirty("Frame duplicated")
            }
        }
        btn(R.id.btn_insert).setOnClickListener {
            project().insertFrame(currentFrame + 1, com.flipframe.studio.model.Frame())
            currentFrame++
            refreshCanvas()
            refreshTitle()
            markDirty("Frame added")
        }
        btn(R.id.btn_delete).setOnClickListener {
            val before = project().frameCount
            project().removeFrameAt(currentFrame)
            if (project().frameCount != before) {
                if (currentFrame >= project().frameCount) currentFrame = project().frameCount - 1
                refreshCanvas()
                refreshTitle()
                markDirty("Frame deleted")
            } else {
                toast("Cannot delete the only frame")
            }
        }
        btn(R.id.btn_fps).setOnClickListener { FpsDialog.show(this, project().fps) { v -> project().fps = v; updateFpsLabel(); markDirty("FPS set to $v") } }
        btn(R.id.btn_export_png).setOnClickListener { exportCurrentFramePng() }
        btn(R.id.btn_flipscript).setOnClickListener { openBrowser() }
        btn(R.id.btn_project).setOnClickListener {
            ProjectPickerDialog.show(this, ::saveProject, ::openProjectPicker, ::newProject)
        }
    }

    private fun btn(id: Int): Button = findViewById(id)

    private fun pushBrush() {
        canvas.setBrush(brushColor, brushWidth, eraser)
        btn(R.id.btn_eraser).setBackgroundColor(if (eraser) Color.argb(255, 44, 51, 64) else Color.TRANSPARENT)
    }

    private fun step(delta: Int) {
        val n = project().frameCount
        currentFrame = (currentFrame + delta + n) % n
        refreshCanvas()
        refreshTitle()
    }

    private fun togglePlay() {
        playing = !playing
        btn(R.id.btn_play).text = if (playing) "Stop" else "Play"
        if (playing) {
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, (1000f / project().fps).toLong())
            toast("Playing")
        } else {
            handler.removeCallbacks(tick)
            refreshCanvas()
        }
    }

    override fun onStrokeCommitted(stroke: Stroke) {
        val f = project().frameAt(currentFrame)
        f.addStroke(stroke)
        undo.recordAddStroke(currentFrame, f.strokes.size - 1)
        refreshCanvas()
        markDirty(null)
    }

    override fun onDocumentTouched() {
        if (playing) togglePlay()
    }

    private fun refreshCanvas() {
        canvas.setDocument(project().width, project().height, project().background)
        canvas.setStrokesProvider { project().frameAt(currentFrame).strokes.toList() }
        if (onion) {
            val prev = project().frames.getOrNull(currentFrame - 1)?.let {
                OnionRendererFacade.render(this, it, project())
            }
            val next = project().frames.getOrNull(currentFrame + 1)?.let {
                OnionRendererFacade.render(this, it, project())
            }
            canvas.setOnion(prev, next, 0.3f)
        } else {
            canvas.setOnion(null, null, 0f)
        }
        findViewById<TextView>(R.id.frame_label).text = "${currentFrame + 1} / ${project().frameCount}"
        canvas.invalidate()
    }

    private fun refreshTitle() {
        findViewById<TextView>(R.id.title).text = project().name
        updateFpsLabel()
    }

    private fun updateFpsLabel() {
        btn(R.id.btn_fps).text = "FPS ${project().fps}"
    }

    private fun markDirty(action: String?) {
        dirty = true
        findViewById<TextView>(R.id.save_status).text = "Unsaved"
        action?.let { showStatus(it) }
    }

    private fun showStatus(text: String) {
        findViewById<TextView>(R.id.status).text = text
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    // ---- Persistence ----

    private fun saveProject() {
        val file = ProjectFile.saveToAppDocuments(this, doc)
        dirty = false
        findViewById<TextView>(R.id.save_status).text = "Saved"
        showStatus("Saved ${file.name} (${file.length() / 1024} KB) in app Documents")
        toast("Saved ${file.name}")
    }

    private fun openProjectPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            type = "application/zip"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/octet-stream"))
        }
        startActivityForResult(intent, RC_OPEN)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_OPEN && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            openFromUri(uri)
        }
    }

    private fun openFromUri(uri: Uri) {
        val project = ProjectFile.loadFromUri(this, uri)
        if (project == null) {
            toast("Could not open project - is it a .flipframe file?")
            return
        }
        doc = FlipDocument(project)
        undo = UndoStack(project)
        currentFrame = 0
        pushBrush()
        refreshCanvas()
        refreshTitle()
        dirty = false
        findViewById<TextView>(R.id.save_status).text = "Saved"
        showStatus("Opened ${ProjectFile.queryDisplayName(this, uri)} - ${project.frameCount} frames")
    }

    private fun newProject() {
        doc = FlipDocument(Project(name = "Untitled"))
        undo.clear()
        currentFrame = 0
        playing = false
        btn(R.id.btn_play).text = "Play"
        pushBrush()
        refreshCanvas()
        refreshTitle()
        dirty = false
        findViewById<TextView>(R.id.save_status).text = "Saved"
        showStatus("Started a new blank project")
    }

    private fun openBrowser() {
        if (dirty) {
            toast("Save first (unsaved strokes are not in the script yet)")
            return
        }
        val intent = Intent(this, BrowseActivity::class.java)
        startActivity(intent)
    }

    private fun exportCurrentFramePng() {
        val bmp = project().renderFrameBitmap(currentFrame)
        val file = ProjectFile.saveToAppDocuments(this, doc) // marker; real export below
        val out = java.io.File(file.parentFile, "frame_${currentFrame + 1}.png")
        java.io.FileOutputStream(out).use { f ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, f)
        }
        showStatus("Exported ${out.name} (${out.length() / 1024} KB) in app Documents")
        toast("Exported ${out.name}")
    }

    companion object {
        private const val RC_OPEN = 21
    }
}
