package me.anno.ui.base.groups

import me.anno.Time
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.Style
import kotlin.math.abs

@Suppress("unused")
class UpdatingContainer(updateMillis: Long, val getter: () -> Panel?, style: Style) :
    PanelContainer(getter() ?: Panel(style), Padding.Zero, style) {

    val updateNanos = updateMillis * 1_000_000
    var lastUpdate = 0L

    override fun onUpdate() {
        val time = Time.nanoTime
        if (abs(lastUpdate - time) >= updateNanos) {
            val value = getter()
            isVisible = value != null
            if (value != null) {
                child = value
                child.parent = this
                child.window = window
            }
            lastUpdate = time
        }
        super.onUpdate()
    }

}