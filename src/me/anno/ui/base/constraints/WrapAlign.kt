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
            val delta = max(0, getOffset(panel.width, panel.minW))
            panel.x = panel.x + delta
            panel.width = min(panel.minW, panel.width)
        }
        alignY?.apply {
            // delta is the movement to the bottom;
            // therefore it must not be < 0
            val delta = max(0, getOffset(panel.height, panel.minH))
            panel.y = panel.y + delta
            panel.height = min(panel.minH, panel.height)
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

    override val className: String get() = "WrapAlign"

    override fun clone() = WrapAlign(alignX, alignY)

    companion object {

        @JvmField
        val CenterX = WrapAlign(AxisAlignment.CENTER, null)
        @JvmField
        val CenterY = WrapAlign(null, AxisAlignment.CENTER)

        @JvmField
        val Center = WrapAlign(AxisAlignment.CENTER, AxisAlignment.CENTER)

        @JvmField
        val LeftTop = WrapAlign(AxisAlignment.MIN, AxisAlignment.MIN)
        @JvmField
        val Left = WrapAlign(AxisAlignment.MIN, null)
        @JvmField
        val LeftCenter = WrapAlign(AxisAlignment.MIN, AxisAlignment.CENTER)
        @JvmField
        val LeftBottom = WrapAlign(AxisAlignment.MIN, AxisAlignment.MAX)

        @JvmField
        val RightBottom = WrapAlign(AxisAlignment.MAX, AxisAlignment.MAX)

        @JvmField
        val TopFill = WrapAlign(null, AxisAlignment.MIN)
        @JvmField
        val BottomFill = WrapAlign(null, AxisAlignment.MAX)

    }

}