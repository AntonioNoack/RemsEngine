package me.anno.ui.input.components

import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.input.InputVisibility
import me.anno.ui.style.Style

class VectorInputList(val visibilityKey: String, style: Style) : PanelListX(style) {
    override var isVisible: Boolean
        get() = InputVisibility[visibilityKey]
        set(_) {}

    init {
        disableConstantSpaceForWeightedChildren = true
        add(WrapAlign.TopFill)
    }
}