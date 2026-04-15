package me.anno.ui.input.components

import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.input.InputVisibility
import me.anno.ui.input.NumberInput
import me.anno.utils.Color.mixARGB2

class VectorInputList(val visibilityKey: String, style: Style) : PanelListX(style) {

    companion object {
        val axisColors = intArrayOf(0xff7777, 0x88ff88, 0x9999ff, 0xcccccc)
        fun modifyTextColor(input: NumberInput<*>, index: Int) {
            input.textColor = mixARGB2(input.textColor, axisColors[index], 0.4f)
        }
    }

    override var isEnabled: Boolean
        get() = InputVisibility[visibilityKey]
        set(_) {}

    init {
        onlyUseWeights = true
        alignmentX = AxisAlignment.FILL
    }
}