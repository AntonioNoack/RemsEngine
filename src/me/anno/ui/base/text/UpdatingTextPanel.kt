package me.anno.ui.base.text

import me.anno.Engine
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import kotlin.math.abs

class UpdatingTextPanel(updateMillis: Long, style: Style, val getValue: () -> String?) :
    TextPanel(getValue() ?: "", style) {

    val updateNanos = updateMillis * 1_000_000
    var lastUpdate = 0L

    override fun tickUpdate() {
        val time = Engine.gameTime
        if (abs(lastUpdate - time) >= updateNanos) {
            val value = getValue()
            visibility = Visibility[value != null]
            text = value ?: text
            lastUpdate = time
        }
        super.tickUpdate()
    }

}