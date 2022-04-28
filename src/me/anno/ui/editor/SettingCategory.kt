package me.anno.ui.editor

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Panel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.InputVisibility
import me.anno.ui.style.Style
import me.anno.utils.input.Keys.isClickKey
import kotlin.math.max

// todo when the scrollbar is enabled, the size calculation is incorrect
open class SettingCategory(
    val title: String,
    val visibilityKey: String,
    withScrollbar: Boolean,
    val canCopyTitleText: Boolean,
    style: Style
) : PanelGroup(style) {

    constructor(title: String, style: Style) :
            this(title, title, false, false, style)

    constructor(title: String, description: String, dictPath: String, style: Style) :
            this(title, description, dictPath, false, style)

    constructor(title: String, description: String, dictPath: String, withScrollbar: Boolean, style: Style) :
            this(Dict[title, dictPath], title, withScrollbar, false, style) {
        tooltip = Dict[description, "$dictPath.desc"]
    }

    val titlePanel = object : TextPanel(title, style.getChild("group")) {
        override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
            if (button.isLeft && !long) toggle()
            else super.onMouseClicked(x, y, button, long)
        }

        override fun onCopyRequested(x: Float, y: Float): Any? {
            return if (canCopyTitleText) text
            else uiParent?.onCopyRequested(x, y)
        }
    }

    val content = object : PanelListY(style) {
        override var visibility: Visibility
            get() = InputVisibility[visibilityKey]
            set(_) {}
    }

    val scrollbar: ScrollPanelY? =
        if (withScrollbar) {
            object : ScrollPanelY(content, Padding.Zero, style) {
                override var visibility: Visibility
                    get() = InputVisibility[visibilityKey]
                    set(_) {}
            }
        } else null

    val padding = Padding((titlePanel.font.size * .667f).toInt(), 0, 0, 0)

    init {
        titlePanel.parent = this
        // titlePanel.enableHoverColor = true
        titlePanel.textColor = mixARGB(titlePanel.textColor, titlePanel.textColor and 0xffffff, 0.5f)
        titlePanel.focusTextColor = -1//titlePanel.textColor
        (scrollbar ?: content).parent = this
    }

    fun show2() {
        InputVisibility.show(visibilityKey, null)
    }

    fun toggle() {
        InputVisibility.toggle(visibilityKey, this)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        if (key.isClickKey()) toggle()
    }

    override fun acceptsChar(char: Int) = char.isClickKey()
    override fun isKeyInput() = true

    override val children: List<Panel> = listOf(this.titlePanel, scrollbar ?: content)
    override fun remove(child: Panel) {
        throw RuntimeException("Not supported!")
    }

    val isEmpty
        get(): Boolean {
            val children = content.children
            for (index in children.indices) {
                val child = children[index]
                if (child.visibility == Visibility.VISIBLE) {
                    return false
                }
            }
            return true
        }

    override fun calculateSize(w: Int, h: Int) {
        // println("[$gameTime] calculateSize($title): $w x $h, $minW x $minH, ${this.w} x ${this.h}")
        if (isEmpty) {
            minW = 0
            minH = 0
        } else {
            titlePanel.calculateSize(w, h)
            val panel2 = scrollbar ?: content
            if (panel2.visibility == Visibility.GONE) {
                minW = titlePanel.minW
                minH = titlePanel.minH
            } else {
                panel2.calculateSize(w - padding.width, h)
                minW = max(titlePanel.minW, content.minW + padding.width)
                minH = titlePanel.minH + content.minH + padding.height
            }
        }
    }

    // todo we should try to reduce 2^x layout classes, maybe by caching results
    // todo maybe within a frame, already would be good enough
    // todo test with deeply cascaded layouts :)

    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        titlePanel.setPosition(x, y)
        val panel2 = scrollbar ?: content
        // println("[$gameTime] setPosition($title): $w x $h, ${panel2.minW} x ${panel2.minH}, ${panel2.w} x ${panel2.h}")
        panel2.setPosition(x + padding.left, y + titlePanel.minH + padding.top)
    }

    operator fun plusAssign(child: Panel) {
        content.add(child)
    }

    override fun addChild(child: PrefabSaveable) {
        content.addChild(child)
    }

}