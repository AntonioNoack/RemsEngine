package me.anno.ui.base.constraints

import me.anno.io.base.BaseWriter
import me.anno.ui.Panel
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
class WrapAlign(
    var alignX: AxisAlignment?,
    var alignY: AxisAlignment?
) : Constraint(10) {

    constructor() : this(null, null)

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

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("alignX", alignX?.id ?: -1)
        writer.writeInt("alignY", alignY?.id ?: -1)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "alignX" -> alignX = AxisAlignment.find(value)
            "alignY" -> alignY = AxisAlignment.find(value)
            else -> super.readInt(name, value)
        }
    }

    override val className = "WrapAlign"

    override fun clone() = WrapAlign(alignX, alignY)

    companion object {

        @JvmStatic
        val CenterX = WrapAlign(AxisAlignment.CENTER, null)
        @JvmStatic
        val CenterY = WrapAlign(null, AxisAlignment.CENTER)

        @JvmStatic
        val Center = WrapAlign(AxisAlignment.CENTER, AxisAlignment.CENTER)

        @JvmStatic
        val LeftTop = WrapAlign(AxisAlignment.MIN, AxisAlignment.MIN)
        @JvmStatic
        val Left = WrapAlign(AxisAlignment.MIN, null)
        @JvmStatic
        val LeftCenter = WrapAlign(AxisAlignment.MIN, AxisAlignment.CENTER)
        @JvmStatic
        val LeftBottom = WrapAlign(AxisAlignment.MIN, AxisAlignment.MAX)

        @JvmStatic
        val RightBottom = WrapAlign(AxisAlignment.MAX, AxisAlignment.MAX)

        @JvmStatic
        val TopFill = WrapAlign(null, AxisAlignment.MIN)
        @JvmStatic
        val BottomFill = WrapAlign(null, AxisAlignment.MAX)

    }

}