package me.anno.ui.base.groups

import me.anno.ui.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.InputVisibility
import me.anno.ui.style.Style
import me.anno.utils.types.Strings.isBlank2

open class TitledListY(val title: String, val visibilityKey: String, sorter: Comparator<Panel>?, style: Style) :
    PanelListY(sorter, style), TextStyleable {

    constructor(style: Style) : this("", "", null, style)

    constructor(title: String, visibilityKey: String, style: Style) : this(title, visibilityKey, null, style)

    val titleView = if (title.isBlank2()) null else TextPanel(title, style)

    init {
        if (titleView != null) {
            this.add(titleView)
            titleView.addOnClickListener { x, y, button, long ->
                if (button.isLeft && !long) {
                    InputVisibility.toggle(visibilityKey, this)
                } else this@TitledListY.onMouseClicked(x, y, button, long)
                true
            }
        }
        disableConstantSpaceForWeightedChildren = true
    }

    override var textColor: Int
        get() = titleView?.textColor ?: 0
        set(value) {
            titleView?.textColor = value
        }

    override var textSize: Float
        get() = titleView?.textSize ?: 0f
        set(value) {
            titleView?.textSize = value
        }

    override var isBold: Boolean
        get() = titleView?.isBold == true
        set(value) {
            titleView?.isBold = value
        }

    override var isItalic: Boolean
        get() = titleView?.isItalic == true
        set(value) {
            titleView?.isItalic = value
        }

    override fun clear() {
        super.clear()
        if (titleView != null) add(titleView)
    }

    override fun clone(): TitledListY {
        val clone = TitledListY(title, visibilityKey, sorter, style)
        copy(clone)
        return clone
    }

    override val className = "TitledListY"

}