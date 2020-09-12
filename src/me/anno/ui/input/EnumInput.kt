package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import me.anno.utils.isDownKey
import me.anno.utils.isUpKey

class EnumInput(
    private val title: String, withTitle: Boolean, startValue: String,
    private val options: List<String>, style: Style
) : PanelListX(style) {

    var lastIndex = options.indexOf(startValue)

    private val titlePanel = if (withTitle) TextPanel("$title:", style) else null
    private val inputPanel = object : TextPanel(startValue, style.getChild("italic")) {
        override fun acceptsChar(char: Int) = char.isUpKey() || char.isDownKey()
        override fun onKeyTyped(x: Float, y: Float, key: Int) {
            if(key.isDownKey()) moveDown(1)
            else if(key.isUpKey()) moveDown(-1)
        }
        override fun isKeyInput() = true
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
        val focused = titlePanel?.isInFocus == true || inputPanel.isInFocus
        if (focused) isSelectedListener?.invoke()
        super.onDraw(x0, y0, x1, y1)
    }

    init {
        if (titlePanel != null) this += titlePanel
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
        GFX.openMenu(this.x, this.y, "Select the $title", options.mapIndexed { index, fontName ->
            fontName to {
                inputPanel.text = fontName
                lastIndex = index
                changeListener(fontName, index, options)
            }
        })
    }

    override fun getCursor(): Long = Cursor.drag
    override fun getClassName() = "EnumInput"

}