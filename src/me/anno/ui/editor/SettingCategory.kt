package me.anno.ui.editor

import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.ui.base.Panel
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.InputVisibility
import me.anno.ui.style.Style
import me.anno.utils.input.Keys.isClickKey
import me.anno.utils.Maths.mixARGB
import kotlin.math.max

open class SettingCategory(
    val title: String,
    style: Style,
    val canCopyTitleText: Boolean = false
) : PanelGroup(style) {

    constructor(title: String, description: String, dictPath: String, style: Style) :
            this(Dict[title, dictPath], style){
        setTooltip(Dict[description, "$dictPath.desc"])
    }

    val titlePanel = object : TextPanel(title, style.getChild("group")) {
        override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
            if (button.isLeft && !long) toggle()
            else super.onMouseClicked(x, y, button, long)
        }

        override fun onCopyRequested(x: Float, y: Float): String? {
            return if (canCopyTitleText) text
            else parent?.onCopyRequested(x, y)
        }
    }

    val content = object : PanelListY(style) {
        override var visibility: Visibility
            get() = InputVisibility[title]
            set(_) {}
    }

    val padding = Padding((titlePanel.font.size * .667f).toInt(), 0, 0, 0)

    init {
        titlePanel.parent = this
        // titlePanel.enableHoverColor = true
        titlePanel.textColor = mixARGB(titlePanel.textColor, titlePanel.textColor and 0xffffff, 0.5f)
        titlePanel.focusTextColor = -1//titlePanel.textColor
        content.parent = this
        content.visibility = Visibility.GONE
    }

    fun show2() {
        InputVisibility.show(title, null)
    }

    fun toggle() {
        InputVisibility.toggle(title, this)
    }

    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        if (key.isClickKey()) toggle()
    }

    override fun acceptsChar(char: Int) = char.isClickKey()
    override fun isKeyInput() = true

    override val children: List<Panel> = listOf(this.titlePanel, content)
    override fun remove(child: Panel) {
        throw RuntimeException("Not supported!")
    }

    val isEmpty
        get() = content
            .children
            .firstOrNull { it.visibility == Visibility.VISIBLE } == null

    override fun calculateSize(w: Int, h: Int) {
        if (isEmpty) {
            minW = 0
            minH = 0
        } else {
            titlePanel.calculateSize(w, h)
            if (content.visibility == Visibility.GONE) {
                minW = titlePanel.minW
                minH = titlePanel.minH
            } else {
                content.calculateSize(w - padding.width, h)
                minW = max(titlePanel.minW, content.minW + padding.width)
                minH = titlePanel.minH + content.minH + padding.height
            }
        }
    }

    override fun placeInParent(x: Int, y: Int) {
        super.placeInParent(x, y)
        titlePanel.placeInParent(x, y)
        content.placeInParent(x + padding.left, y + titlePanel.minH + padding.top)
    }

    fun add(child: Panel){
        content += child
    }

    operator fun plusAssign(child: Panel) {
        content += child
    }

}