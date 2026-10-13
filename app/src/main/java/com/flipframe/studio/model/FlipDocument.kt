package com.flipframe.studio.model

import com.flipframe.studio.flipscript.FlipScriptParser
import com.flipframe.studio.flipscript.FlipScriptRenderer

/**
 * Combines the stroke/bitamp editor document ([Project]) with the code-facing
 * representation ([FlipScript]).
 */
class FlipDocument(val project: Project) {

    val script: StringBuilder by lazy { StringBuilder(FlipScriptRenderer.render(project)) }

    /** Re-runs [parser] on the current script and merges the result into this document. */
    fun applyScript(parser: FlipScriptParser = FlipScriptParser()) {
        val doc = parser.parse(script.toString())
        project.name = doc.name
        project.width = doc.width
        project.height = doc.height
        project.fps = doc.fps
        project.background = doc.background
        project.frames = doc.frames
    }

    fun newScript(): StringBuilder {
        script.clear()
        script.append(FlipScriptRenderer.render(project))
        return script
    }

    companion object {
        const val FILE_EXT = ".flipscript"
        const val SAVE_FILE_EXT = ".flipframe"

        /** 1 dp == 1 px at the baseline (mdpi) density used by document units. */
        const val DP_PER_PX = 1f
        const val PX_PER_MM = 4f
    }
}

/** Rendering conversion helpers, mostly used by overlay UI like onion skin. */
object Units {
    /** Converts document coordinates to view pixel coordinates. */
    fun docToView(value: Float, viewSize: Int, docSize: Int): Float =
        if (docSize <= 0) value else value * viewSize / docSize.toFloat()

    /** Converts view pixels back into document units. */
    fun viewToDoc(value: Float, viewSize: Int, docSize: Int): Float =
        if (viewSize <= 0) value else value * docSize / viewSize.toFloat()
}
