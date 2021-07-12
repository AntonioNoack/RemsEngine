package me.anno.ui.base.groups

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.InputVisibility
import me.anno.ui.style.Style
import me.anno.utils.types.Strings.isBlank2

open class TitledListY(val title: String, val visibilityKey: String, sorter: Comparator<Panel>?, style: Style) :
    PanelListY(sorter, style) {

    constructor(title: String, visibilityKey: String, style: Style) : this(title, visibilityKey, null, style)

    val titleView = if (title.isBlank2()) null else TextPanel(title, style)

    init {
        if (titleView != null) {
            this.add(titleView)
            titleView.setOnClickListener { x, y, button, long ->
                if (button.isLeft && !long) {
                    InputVisibility.toggle(visibilityKey, this)
                } else this@TitledListY.onMouseClicked(x, y, button, long)
            }
        }
        disableConstantSpaceForWeightedChildren = true
    }

    override fun clear() {
        super.clear()
        if (titleView != null) add(titleView)
    }

}