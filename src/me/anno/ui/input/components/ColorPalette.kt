package me.anno.ui.input.components

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelGroup
import me.anno.utils.structures.lists.Lists.createArrayList

// maximum size???...
open class ColorPalette(
    private val dimX: Int,
    private val dimY: Int,
    style: Style
) : PanelGroup(style) {

    override val children: List<Panel> = createArrayList(dimX * dimY) {
        ColorField(this, it % dimX, it / dimX, 0, style)
    }

    override fun remove(child: Panel) {}

    var onColorSelected: (color: Int) -> Unit = { }

    override fun calculateSize(w: Int, h: Int) {
        minW = w
        minH = minW * dimY / dimX
    }

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        val h = height
        for (j in 0 until dimY) {
            val y2 = y + j * h / dimY
            val y3 = y + (j + 1) * h / dimY
            for (i in 0 until dimX) {
                val x2 = x + i * h / dimY
                val x3 = x + (i + 1) * h / dimY
                children[i + j * dimX].setPosSize(x2, y2, x3 - x2, y3 - y2)
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
}