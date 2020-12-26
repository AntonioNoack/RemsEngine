package me.anno.ui.base.constraints

import me.anno.ui.base.Panel
import kotlin.math.max
import kotlin.math.min

class WrapAlign(val alignX: AxisAlignment?, val alignY: AxisAlignment?): Constraint(10){
    
    override fun apply(panel: Panel) {
        alignX?.apply {
            // delta is the movement to the right;
            // therefore it must not be < 0
            val delta = max(0, getOffset(panel.w, panel.minW))
            panel.x = panel.x + delta
            panel.w = min(panel.minW, panel.w)
        }
        alignY?.apply {
            // delta is the movement to the bottom;
            // therefore it must not be < 0
            val delta = max(0, getOffset(panel.h, panel.minH))
            panel.y = panel.y + delta
            panel.h = min(panel.minH, panel.h)
        }
    }

    override fun toString() = "Wrap($alignX $alignY)"

    companion object {

        val CenterX = WrapAlign(AxisAlignment.CENTER, null)
        val CenterY = WrapAlign(null, AxisAlignment.CENTER)

        val Center = WrapAlign(AxisAlignment.CENTER, AxisAlignment.CENTER)

        val LeftTop = WrapAlign(AxisAlignment.MIN, AxisAlignment.MIN)
        val Left = WrapAlign(AxisAlignment.MIN, null)
        val LeftBottom = WrapAlign(AxisAlignment.MIN, AxisAlignment.MAX)

        val RightBottom = WrapAlign(AxisAlignment.MAX, AxisAlignment.MAX)

        val TopFill = WrapAlign(null, AxisAlignment.MIN)
        val BottomFill = WrapAlign(null, AxisAlignment.MAX)



    }

}