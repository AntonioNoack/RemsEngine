package me.anno.ui.input.components

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style

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
        this.w = minW
        this.h = minH
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        for (j in 0 until dimY) {
            val y2 = y + j * h / dimY
            val y3 = y + (j + 1) * h / dimY
            for (i in 0 until dimX) {
                val x2 = x + i * w / dimX
                val x3 = x + (i + 1) * w / dimX
                val index = getIndex(i, j)
                val child = children[index]
                child.placeInParent(x2, y2)
                child.w = x3 - x2
                child.h = y3 - y2
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
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ColorPalette
        // !! this can be incorrect, if the function references this special instance
        clone.onColorSelected = onColorSelected
    }

    override val className: String = "ColorPalette"

}