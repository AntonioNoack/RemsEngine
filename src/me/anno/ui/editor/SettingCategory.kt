package me.anno.ui.editor

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.min
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.input.InputVisibility
import me.anno.ui.input.components.TitlePanel
import me.anno.utils.Color.mulAlpha
import kotlin.math.max

open class SettingCategory private constructor(
    nameDesc: NameDesc, val visibilityKey: String,
    withScrollbar: Boolean, style: Style
) : PanelGroup(style) {

    constructor(nameDesc: NameDesc, style: Style) : this(nameDesc, false, style)
    constructor(nameDesc: NameDesc, withScrollbar: Boolean, style: Style) :
            this(nameDesc, nameDesc.key, withScrollbar, style) {
        tooltip = nameDesc.desc
    }

    @Suppress("LeakingThis")
    val titlePanel = TitlePanel(nameDesc, this, style.getChild("group"))

    val content = PanelListY(style)
    val child = if (withScrollbar) ScrollPanelY(content, Padding.Zero, style) else content
    val padding = Padding((titlePanel.font.size * .667f).toInt(), 0, 0, 0)

    init {
        titlePanel.addLeftClickListener {
            InputVisibility.toggle(visibilityKey, this)
        }
        titlePanel.parent = this
        titlePanel.textColor = titlePanel.textColor.mulAlpha(0.5f)
        titlePanel.focusTextColor = -1
        child.parent = this
    }

    fun showContent(): SettingCategory {
        InputVisibility.show(visibilityKey, child)
        return this
    }

    var visibilityByKey: Boolean
        get() = InputVisibility[visibilityKey]
        set(value) {
            InputVisibility[visibilityKey] = value
        }

    override fun onUpdate() {
        val visible = InputVisibility[visibilityKey]
        child.isVisible = visible
        super.onUpdate()
    }

    override val children: List<Panel> = listOf(titlePanel, child)
    override fun remove(child: Panel) {
        throw RuntimeException("Not supported!")
    }

    val isEmpty
        get(): Boolean {
            val children = content.children
            for (index in children.indices) {
                val child = children[index]
                if (child.isVisible) {
                    return false
                }
            }
            return true
        }

    override fun calculateSize(w: Int, h: Int) {
        this.width = w
        this.height = h
        if (isEmpty) {
            minW = 0
            minH = 0
        } else {
            titlePanel.calculateSize(w, h)
            if (child.isVisible) {
                child.calculateSize(w - padding.width, h)
                minW = max(titlePanel.minW, content.minW + padding.width)
                minH = titlePanel.minH + content.minH + padding.height
            } else {
                minW = titlePanel.minW
                minH = titlePanel.minH
            }
        }
    }

    override fun placeChildren(x: Int, y: Int, width: Int, height: Int) {
        titlePanel.setPosSize(x, y, min(titlePanel.minW, width), min(titlePanel.minH, height))
        child.setPosSize(
            x + padding.left, y + titlePanel.minH + padding.top,
            if (child.isVisible) width - padding.width else 1,
            if (child.isVisible) height - titlePanel.height - padding.height else 1
        )
    }

    operator fun plusAssign(child: Panel) {
        content.add(child)
    }

    override fun addChild(child: PrefabSaveable) {
        content.addChild(child)
    }
}