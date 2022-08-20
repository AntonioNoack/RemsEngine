package me.anno.ui.base

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.Panel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.style.Style

open class SpacerPanel(
    var sizeX: Int,
    var sizeY: Int,
    style: Style
) : Panel(style.getChild("spacer")) {

    constructor(style: Style) : this(10, 10, style)

    init {
        when {
            sizeX == 0 -> layoutConstraints += WrapAlign.TopFill
            sizeY == 0 -> layoutConstraints += WrapAlign.Left
        }
    }

    override fun calculateSize(w: Int, h: Int) {
        minW = sizeX
        minH = sizeY
    }

    fun setColor(color: Int): SpacerPanel {
        backgroundColor = color
        return this
    }

    override fun clone(): SpacerPanel {
        val clone = SpacerPanel(sizeX, sizeY, style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SpacerPanel
        clone.sizeX = sizeX
        clone.sizeY = sizeY
    }

    override val className = "SpacerPanel"

}