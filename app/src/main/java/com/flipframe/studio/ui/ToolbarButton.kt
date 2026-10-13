package com.flipframe.studio.ui

import android.view.View
import com.flipframe.studio.R

/** Toolbar button theme sugar shared between the editor and browser screens. */
object ToolbarButton {
    fun style(v: View) {
        v.setBackgroundResource(R.drawable.bg_button)
        v.minimumWidth = (64 * v.resources.displayMetrics.density).toInt()
    }
}
