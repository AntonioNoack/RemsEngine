package me.anno.ui.base.text

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.ui.Style
import kotlin.math.abs

open class UpdatingTextPanel(updateMillis: Long, style: Style, val getValue: () -> String?) :
    TextPanel(getValue() ?: "", style) {

    val updateNanos = updateMillis * MILLIS_TO_NANOS
    var lastUpdate = 0L

    override fun onUpdate() {
        val time = Time.nanoTime
        if (abs(lastUpdate - time) >= updateNanos) {
            val value = getValue()
            isVisible = value != null
            text = value ?: text
            lastUpdate = time
        }
        super.onUpdate()
    }

}