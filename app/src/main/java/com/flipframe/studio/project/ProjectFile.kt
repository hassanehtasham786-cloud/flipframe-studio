package com.flipframe.studio.project

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.flipframe.studio.flipscript.FlipScriptParser
import com.flipframe.studio.flipscript.FlipScriptRenderer
import com.flipframe.studio.model.FlipDocument
import com.flipframe.studio.model.Project
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Reads and writes `.flipframe` project archives (ZIP containers).
 *
 * Layout:
 * ```
 * project.json     - metadata + settings
 * animation.flip   - editable FlipScript source
 * AGENT_GUIDE.md   - instructions for coding agents
 * ```
 */
object ProjectFile {

    const val ENTRY_PROJECT_JSON = "project.json"
    const val ENTRY_SCRIPT = "animation.flip"
    const val ENTRY_AGENT_GUIDE = "AGENT_GUIDE.md"
    const val FILE_EXT = ".flipframe"

    /** Serializes [doc] as a `.flipframe` archive into [out]. */
    fun save(doc: FlipDocument, out: OutputStream) {
        val script = FlipScriptRenderer.render(doc.project)
        val metadata = doc.project.metadataJson().put("script", script).toString(2)
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_PROJECT_JSON))
            zip.write(metadata.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(ENTRY_SCRIPT))
            zip.write(script.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(ENTRY_AGENT_GUIDE))
            zip.write(AgentGuide.MARKDOWN.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    /** Saves [doc] to a file named `<name>.flipframe` in the app's Documents dir. */
    fun saveToAppDocuments(context: Context, doc: FlipDocument): File {
        val dir = File(context.getExternalFilesDir(null)?.parentFile?.absolutePath ?: context.filesDir.parentFile!!.absolutePath, "Documents")
        val fallback = File(context.filesDir, "Documents")
        val target = (if (dir.canWrite()) dir else fallback).apply { mkdirs() }
        val file = File(target, uniqueFileName(target, safeFileName(doc.project.name) + FILE_EXT))
        FileOutputStream(file).use { save(doc, it) }
        return file
    }

    private fun safeFileName(name: String): String {
        val cleaned = name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifEmpty { "My_Animation" }
        return cleaned.replace(' ', '_')
    }

    private fun uniqueFileName(dir: File, desired: String): String {
        var candidate = desired
        var n = 1
        while (File(dir, candidate).exists()) {
            val dot = desired.lastIndexOf('.')
            candidate = if (dot > 0) {
                desired.substring(0, dot) + "_" + n + desired.substring(dot)
            } else {
                desired + "_" + n
            }
            n++
        }
        return candidate
    }

    /** Reads a `.flipframe` from a content [Uri] (SAF picker) and returns metadata. */
    fun loadFromUri(context: Context, uri: Uri): Project? {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { return loadFromStream(it) }
    }

    /** Reads a `.flipframe` archive from raw bytes. */
    fun loadFromStream(input: InputStream): Project? {
        val temp = File.createTempFile("flipframe", ".zip")
        try {
            FileOutputStream(temp).use { input.copyTo(it) }
            ZipFile(temp).use { zip ->
                val pj = zip.getEntry(ENTRY_PROJECT_JSON) ?: return null
                val json = zip.getInputStream(pj).bufferedReader().readText()
                return parseProjectJson(json)
            }
        } catch (e: Exception) {
            return null
        } finally {
            temp.delete()
        }
    }

    /** Reads a `.flipframe` from a file on disk (used by autosave recovery). */
    fun loadFromFile(file: File): Project? {
        if (!file.exists()) return null
        return try {
            ZipFile(file).use { zip ->
                val pj = zip.getEntry(ENTRY_PROJECT_JSON) ?: return null
                val json = zip.getInputStream(pj).bufferedReader().readText()
                parseProjectJson(json)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reads `project.json`; tolerates v1 archives where stroke JSON is embedded in the
     * metadata object and newer archives where it lives under `animation.flip`.
     */
    fun parseProjectJson(json: String): Project? {
        return try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: root
            Project.fromJson(data)
        } catch (e: Exception) {
            null
        }
    }

    /** Parses the FlipScript entry of an archive when agents edit it externally. */
    fun parseScriptFromArchive(file: File): String? {
        if (!file.exists()) return null
        return try {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(ENTRY_SCRIPT) ?: return null
                zip.getInputStream(entry).bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun queryDisplayName(context: Context, uri: Uri): String {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "project"
    }
}
