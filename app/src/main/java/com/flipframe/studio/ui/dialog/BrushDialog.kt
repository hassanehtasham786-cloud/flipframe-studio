package com.flipframe.studio.ui.dialog

import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.flipframe.studio.R

/** Brush size dialog: 1..40 px document stroke width. */
object BrushDialog {

    fun show(activity: android.app.Activity, current: Float, onWidth: (Float) -> Unit) {
        val context = activity
        val layout = LinearLayout(context)
        val label = TextView(context).apply {
            text = "Size: ${current.toInt()} px"
            textSize = 15f
        }
        val bar = SeekBar(context).apply {
            max = 39
            progress = (current.toInt() - 1).coerceIn(0, 39)
        }
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 4))
        layout.addView(label)
        layout.addView(bar)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                label.text = "Size: ${progress + 1} px"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        AlertDialog.Builder(context, R.style.Theme_FlipFrame)
            .setTitle("Brush size")
            .setView(layout)
            .setPositiveButton("Use") { _, _ -> onWidth((bar.progress + 1).toFloat()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(context: android.content.Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}
