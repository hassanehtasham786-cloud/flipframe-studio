package com.flipframe.studio.ui.dialog

import android.app.AlertDialog
import android.graphics.Color
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import com.flipframe.studio.R
import com.flipframe.studio.ui.dialog.ColorPalette.PRESETS

/** Color picker with a compact grid of common colors plus an arbitrary hex field. */
object ColorDialog {

    fun show(activity: android.app.Activity, current: Int, onColor: (Int) -> Unit) {
        val context = activity
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 4))
        }
        val grid = GridLayout(context).apply { columnCount = 6 }
        for (c in PRESETS) {
            val v = android.view.View(context)
            v.setBackgroundColor(c)
            val lp = GridLayout.LayoutParams()
            lp.width = dp(context, 40)
            lp.height = dp(context, 40)
            lp.setMargins(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4))
            v.layoutParams = lp
            v.setOnClickListener { }
            grid.addView(v)
        }
        layout.addView(grid)

        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val hex = EditText(context).apply {
            hint = "#RRGGBB"
            setText(ColorPalette.toHex(current))
            setSingleLine()
        }
        row.addView(hex, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        layout.addView(row)

        // Wire preset taps after `dialog` is assigned (simpler than capturing late).
        var dialogRef: AlertDialog? = null
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            val color = PRESETS.getOrNull(i) ?: continue
            child.setOnClickListener {
                onColor(color)
                dialogRef?.dismiss()
            }
        }

        dialogRef = AlertDialog.Builder(context, R.style.Theme_FlipFrame)
            .setTitle("Brush color")
            .setView(layout)
            .setPositiveButton("Use") { _, _ ->
                ColorPalette.parseHex(hex.text.toString())?.let(onColor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun dp(context: android.content.Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}

/** Palette constants and hex helpers shared by dialogs. */
object ColorPalette {
    val PRESETS: IntArray = intArrayOf(
        Color.BLACK, Color.WHITE, Color.argb(255, 229, 57, 53),
        Color.argb(255, 255, 152, 0), Color.argb(255, 253, 216, 53),
        Color.argb(255, 67, 160, 71), Color.argb(255, 30, 136, 229),
        Color.argb(255, 142, 36, 170), Color.argb(255, 121, 85, 72),
        Color.argb(255, 158, 158, 158), Color.argb(255, 240, 98, 146),
        Color.argb(255, 0, 188, 212)
    )

    fun parseHex(text: String): Int? {
        val t = text.trim()
        if (!t.startsWith("#")) return null
        val hex = t.substring(1)
        return when (hex.length) {
            6 -> try {
                Color.parseColor("#$hex")
            } catch (e: Exception) {
                null
            }
            8 -> try {
                hex.toLong(16).toInt()
            } catch (e: NumberFormatException) {
                null
            }
            else -> null
        }
    }

    fun toHex(color: Int): String = String.format("#%06X", color and 0xFFFFFF)
}
