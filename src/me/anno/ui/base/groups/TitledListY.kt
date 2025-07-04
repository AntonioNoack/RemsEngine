package me.anno.ui.base.groups

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.InputVisibility
import me.anno.ui.input.components.TitlePanel
import me.anno.utils.types.Strings.isBlank2

open class TitledListY(val title: NameDesc, val visibilityKey: String, style: Style) :
    PanelListY(style), TextStyleable {

    val titleView = if (title.name.isBlank2()) null else TitlePanel(title, this, style)

    init {
        if (titleView != null) {
            this.add(titleView)
            titleView.addLeftClickListener {
                InputVisibility.toggle(visibilityKey)
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
        val clone = TitledListY(title, visibilityKey, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyIntoExceptChildren(dst)
    }
}