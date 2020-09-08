package me.anno.ui.base.constraints

import me.anno.ui.base.Constraint
import me.anno.ui.base.Panel
import kotlin.math.min

class WrapAlign(val alignX: AxisAlignment?, val alignY: AxisAlignment?): Constraint(10){
    
    override fun apply(panel: Panel) {
        alignX?.apply {
            val delta = getOffset(panel.w, panel.minW)
            panel.x = panel.x + delta
            panel.w = panel.minW
        }
        alignY?.apply {
            val delta = getOffset(panel.h, panel.minH)
            panel.y = panel.y + delta
            panel.h = panel.minH
        }
    }

    override fun toString() = "Wrap($alignX $alignY)"

    companion object {
        val CenterX = WrapAlign(AxisAlignment.CENTER, null)
        val CenterY = WrapAlign(null, AxisAlignment.CENTER)
        val Center = WrapAlign(AxisAlignment.CENTER, AxisAlignment.CENTER)
        val LeftTop = WrapAlign(AxisAlignment.MIN, AxisAlignment.MIN)
        val Left = WrapAlign(AxisAlignment.MIN, null)
        val Top = WrapAlign(null, AxisAlignment.MIN)
    }

}