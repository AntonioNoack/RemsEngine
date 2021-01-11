package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.input.components.EnumValuePanel
import me.anno.ui.style.Style

class EnumInput(
    private val title: String, withTitle: Boolean, startValue: String,
    private val options: List<String>, style: Style
) : PanelListX(style) {

    constructor(title: String, ttt: String, startValue: String, options: List<String>, style: Style) :
            this(title, true, startValue, options, style) {
        setTooltip(ttt)
    }

    constructor(title: String, ttt: String, dictPath: String, startValue: String, options: List<String>, style: Style) :
            this(Dict[title, dictPath], true, startValue, options, style) {
        setTooltip(Dict[ttt, "$dictPath.desc"])
    }

    var lastIndex = options.indexOf(startValue)

    val titleView = if (withTitle) TextPanel("$title:", style) else null
    val inputPanel = EnumValuePanel(startValue, this, style)

    init {
        titleView?.enableHoverColor = true
        inputPanel.enableHoverColor = true
    }

    fun moveDown(direction: Int) {
        val oldValue = inputPanel.text
        val index = lastIndex + direction
        lastIndex = (index + 2 * options.size) % options.size
        val newValue = options[lastIndex]
        if (oldValue != newValue) {
            inputPanel.text = newValue
            changeListener(newValue, lastIndex, options)
        }
    }

    private var changeListener = { _: String, _: Int, _: List<String> -> }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused = titleView?.isInFocus == true || inputPanel.isInFocus
        if (focused) isSelectedListener?.invoke()
        super.onDraw(x0, y0, x1, y1)
    }

    init {
        if (titleView != null) {
            this += titleView
            titleView.focusTextColor = titleView.textColor
        }
        this += inputPanel
    }

    fun setChangeListener(listener: (value: String, index: Int, values: List<String>) -> Unit): EnumInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): EnumInput {
        isSelectedListener = listener
        return this
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        openMenu(
            this.x, this.y,
            NameDesc("Select the %1", "", "ui.input.enum.menuTitle")
                .with("%1", title),
            options.mapIndexed { index, fontName ->
                MenuOption(NameDesc(fontName, "", "")) {
                    inputPanel.text = fontName
                    lastIndex = index
                    changeListener(fontName, index, options)
                }
            })
    }

    override fun getCursor(): Long = Cursor.drag

}