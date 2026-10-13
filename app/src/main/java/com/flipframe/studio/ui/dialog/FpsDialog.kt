package com.flipframe.studio.ui.dialog

import android.app.AlertDialog
import android.widget.SeekBar
import android.widget.LinearLayout
import android.widget.TextView
import com.flipframe.studio.R

/** Frame rate dialog: 1..60 fps. */
object FpsDialog {

    fun show(activity: android.app.Activity, current: Int, onFps: (Int) -> Unit) {
        val context = activity
        val layout = LinearLayout(context)
        val label = TextView(context).apply { text = "Frame rate: ${current} fps" }
        val bar = SeekBar(context).apply {
            max = 59
            progress = (current - 1).coerceIn(0, 59)
        }
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 4))
        layout.addView(label)
        layout.addView(bar)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                label.text = "Frame rate: ${progress + 1} fps"
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        AlertDialog.Builder(context, R.style.Theme_FlipFrame)
            .setTitle("Frame rate")
            .setView(layout)
            .setPositiveButton("Use") { _, _ -> onFps(bar.progress + 1) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(context: android.content.Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}
