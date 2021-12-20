package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.studio.rems.RemsStudio.project
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.style.Style
import org.apache.logging.log4j.LogManager

// maximum size???...
class ColorPalette(
    private val dimX: Int,
    private val dimY: Int,
    style: Style
) : PanelGroup(style) {

    constructor(base: ColorPalette) : this(base.dimX, base.dimY, base.style) {
        base.copy(this)
    }

    override val className get() = "ColorPalette"

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

    fun getColor(x: Int, y: Int) = project?.config?.get("color.$x.$y", 0) ?: 0
    fun setColor(x: Int, y: Int, value: Int) {
        project?.config?.set("color.$x.$y", value)
    }

    private fun getIndex(x: Int, y: Int): Int = x + y * dimX

    override fun clone() = ColorPalette(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ColorPalette
        // !! this can be incorrect, if the function references this special instance
        clone.onColorSelected = onColorSelected
    }

}