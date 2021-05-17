package me.anno.ui.base.text

import me.anno.gpu.GFX
import me.anno.ui.base.Visibility
import me.anno.ui.style.Style
import me.anno.utils.types.Strings.isBlank2
import kotlin.math.abs

class UpdatingTextPanel (updateMillis: Long, style: Style, val getValue: () -> String?): TextPanel(getValue() ?: "", style){

    val updateNanos = updateMillis * 1_000_000
    var lastUpdate = 0L

    override fun tickUpdate() {
        val time = GFX.gameTime
        if(abs(lastUpdate - time) >= updateNanos){
            val value = getValue()
            visibility = Visibility[value != null]
            text = value ?: text
            lastUpdate = time
        }
        super.tickUpdate()
    }

}