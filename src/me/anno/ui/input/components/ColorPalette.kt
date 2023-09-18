package me.anno.ui.input.components

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.Style

// maximum size???...
open class ColorPalette(
    private val dimX: Int,
    private val dimY: Int,
    style: Style
) : PanelGroup(style) {

    override val children: List<Panel> = Array(dimX * dimY) {
        ColorField(this, it % dimX, it / dimX, 0, style)
    }.toList()

    override fun remove(child: Panel) {}

    var onColorSelected: (color: Int) -> Unit = { }

    override fun calculateSize(w: Int, h: Int) {
        minW = w
        minH = minW * dimY / dimX
        this.width = minW
        this.height = minH
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        for (j in 0 until dimY) {
            val y2 = y + j * height / dimY
            val y3 = y + (j + 1) * height / dimY
            for (i in 0 until dimX) {
                val x2 = x + i * width / dimX
                val x3 = x + (i + 1) * width / dimX
                val index = getIndex(i, j)
                val child = children[index]
                child.setPosition(x2, y2)
                child.width = x3 - x2
                child.height = y3 - y2
            }
        }
    }

    private fun getKey(x: Int, y: Int) = "ui.palette.color[$x,$y]"

    open fun getColor(x: Int, y: Int): Int {
        return DefaultConfig[getKey(x, y), 0]
    }

    open fun setColor(x: Int, y: Int, color: Int) {
        DefaultConfig[getKey(x, y)] = color
    }

    private fun getIndex(x: Int, y: Int): Int = x + y * dimX

    override fun clone(): ColorPalette {
        val clone = ColorPalette(dimX, dimY, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as ColorPalette
        // !! this can be incorrect, if the function references this special instance
        dst.onColorSelected = onColorSelected
    }

    override val className: String get() = "ColorPalette"

}