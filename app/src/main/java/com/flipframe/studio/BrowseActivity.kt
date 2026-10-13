package com.flipframe.studio

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.flipframe.studio.flipscript.FlipScriptParser
import com.flipframe.studio.flipscript.FlipScriptRenderer
import com.flipframe.studio.model.Project
import java.io.File

/**
 * Lists recently saved projects from app Documents and gives a copyable view of the
 * FlipScript stored inside each one. Coding agents parse the shown script; users get a
 * quick read-only preview.
 */
class BrowseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroll)

        title("FlipScript Browser")
        root.addView(label("Recently saved projects", 18f, R.color.accent))

        val dir = documentsDir()
        val files = dir.listFiles { f -> f.extension == "flipframe" }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        if (files.isEmpty()) {
            root.addView(label("No saved projects yet. Save one from the editor first.", 14f, R.color.text_dim))
            return
        }

        for (file in files.take(10)) {
            root.addView(card(file.name))
            val script = readScript(file)
            if (script != null) {
                root.addView(mono(script.trim()))
            } else {
                root.addView(label("  (no animation.flip entry)", 13f, R.color.danger))
            }
        }
    }

    private fun readScript(file: File): String? {
        val raw = runCatching {
            java.util.zip.ZipFile(file).use { zip ->
                zip.getEntry("animation.flip")?.let { e ->
                    zip.getInputStream(e).bufferedReader().readText()
                }
            }
        }.getOrNull() ?: return null
        // Re-render from project strokes to reflect the latest drawing session, since
        // potentially newer app saves keep animation.flip in sync.
        val project = com.flipframe.studio.project.ProjectFile.parseProjectJson(runCatching {
            java.util.zip.ZipFile(file).use { zip ->
                zip.getEntry("project.json")!!.let { e ->
                    zip.getInputStream(e).bufferedReader().readText()
                }
            }
        }.getOrDefault("")) ?: return raw
        return FlipScriptRenderer.render(project)
    }

    private fun documentsDir(): File {
        val external = getExternalFilesDir(null)?.parentFile?.absolutePath
        return if (external != null && File(external, "Documents").canWrite()) {
            File(external, "Documents")
        } else {
            File(filesDir, "Documents")
        }.apply { mkdirs() }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun title(text: String) {
        setTitle(text)
    }

    private fun card(name: String): TextView {
        val v = label(name, 15f, R.color.text_main)
        v.setPadding(0, dp(12), 0, dp(4))
        return v
    }

    private fun label(text: String, size: Float, colorRes: Int): TextView {
        val v = TextView(this)
        v.text = text
        v.textSize = size
        v.setTextColor(getColor(colorRes))
        return v
    }

    private fun mono(text: String): TextView {
        val v = label(text, 12f, R.color.text_dim)
        v.typeface = android.graphics.Typeface.MONOSPACE
        v.background = getDrawable(R.drawable.bg_button)
        v.setPadding(dp(12), dp(12), dp(12), dp(12))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(6)
        v.layoutParams = lp
        return v
    }

    private fun parse(script: String): FlipScriptParser.Document? = runCatching {
        FlipScriptParser().parse(script)
    }.getOrNull()

    private fun log(tag: String, msg: String) {
        Log.d("BrowseActivity", "$tag: $msg")
    }
}
