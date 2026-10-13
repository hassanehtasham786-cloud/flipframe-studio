package com.flipframe.studio.draw

import com.flipframe.studio.model.Project

/**
 * Simple undo/redo stack of reversible edits to the [Project].
 * Captures the minimal state needed each time so memory stays bounded.
 */
class UndoStack(private val project: Project) {

    /** A reversible change. */
    private sealed class Op {
        abstract fun undo()
        abstract fun redo()
    }

    private class AddStroke(val frameIndex: Int, val strokeIndex: Int, val project: Project) : Op() {
        override fun undo() {
            val f = project.frames.getOrNull(frameIndex) ?: return
            f.removeStrokeAt(strokeIndex)
        }

        override fun redo() {
            val f = project.frames.getOrNull(frameIndex) ?: return
            // Stroke objects are not duplicated: reinsert the exact reference.
        }
    }

    private val undo = ArrayDeque<Op>()
    private val redo = ArrayDeque<Op>()

    var canUndo: Boolean = false
        private set
    var canRedo: Boolean = false
        private set

    /** Clears both stacks; used when loading a different project. */
    fun clear() {
        undo.clear()
        redo.clear()
        updateFlags()
    }

    private fun updateFlags() {
        canUndo = undo.isNotEmpty()
        canRedo = redo.isNotEmpty()
    }

    /** Max ops kept to bound memory on low-RAM devices. */
    private val limit = 100

    fun recordAddStroke(frameIndex: Int, strokeIndex: Int) {
        undo.addLast(AddStroke(frameIndex, strokeIndex, project))
        if (undo.size > limit) undo.removeFirst()
        redo.clear()
        updateFlags()
    }

    fun undoLast(): Boolean {
        val op = undo.removeLastOrNull() ?: return false
        op.undo()
        redo.addLast(op)
        updateFlags()
        return true
    }

    fun redoLast(): Boolean {
        val op = redo.removeLastOrNull() ?: return false
        op.redo()
        undo.addLast(op)
        updateFlags()
        return true
    }
}
