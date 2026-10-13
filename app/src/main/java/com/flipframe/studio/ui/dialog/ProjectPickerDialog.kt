package com.flipframe.studio.ui.dialog

import android.app.AlertDialog
import com.flipframe.studio.R

/** Simple menu used by the "Save Project" header button. */
object ProjectPickerDialog {

    fun show(
        activity: android.app.Activity,
        onSave: () -> Unit,
        onOpen: () -> Unit,
        onNew: () -> Unit
    ) {
        val options = arrayOf("Save project", "Open project…", "New project", "Cancel")
        AlertDialog.Builder(activity, R.style.Theme_FlipFrame)
            .setTitle("Project")
            .setItems(options) { d, which ->
                when (which) {
                    0 -> onSave()
                    1 -> onOpen()
                    2 -> onNew()
                }
                d.dismiss()
            }
            .show()
    }
}
