package me.anno.ui.base.constraints

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.Panel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.Style
import me.anno.maths.Maths
import kotlin.math.min

class SizeLimitingContainer(child: Panel, var sizeX: Int, var sizeY: Int, style: Style) :
    PanelContainer(child, Padding.Zero, style) {

    override fun calculateSize(w: Int, h: Int) {
        val limitedW = if (sizeX < 0) w else Maths.min(w, sizeX)
        val limitedH = if (sizeY < 0) h else Maths.min(h, sizeY)
        super.calculateSize(limitedW, limitedH)
        if (sizeX >= 0) minW = min(minW, padding.width + sizeX)
        if (sizeY >= 0) minH = min(minH, padding.height + sizeY)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SizeLimitingContainer
        dst.sizeX = sizeX
        dst.sizeY = sizeY
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("sizeX", sizeX)
        writer.writeInt("sizeY", sizeY)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "sizeX" -> sizeX = value
            "sizeY" -> sizeY = value
            else -> super.readInt(name, value)
        }
    }

    override val className: String get() = "SizeLimitingContainer"

}