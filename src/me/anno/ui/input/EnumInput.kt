package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style

class EnumInput(private val title: String, withTitle: Boolean, startValue: String,
                private val options: List<String>, style: Style): PanelListX(style){

    private val titlePanel = if(withTitle) TextPanel("$title:", style) else null
    private val inputPanel = TextPanel(startValue, style.getChild("italic"))

    private var changeListener = { _: String -> }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused = titlePanel?.isInFocus == true || inputPanel.isInFocus
        if(focused) isSelectedListener?.invoke()
        super.onDraw(x0, y0, x1, y1)
    }

    init {
        if(titlePanel != null) this += titlePanel
        this += inputPanel
    }

    fun setChangeListener(listener: (value: String) -> Unit): EnumInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): EnumInput {
        isSelectedListener = listener
        return this
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        GFX.openMenu(this.x, this.y, "Select the $title", options.map { fontName ->
            fontName to {
                inputPanel.text = fontName
                changeListener(fontName)
            }
        })
    }

    override fun getCursor(): Long = Cursor.drag
    override fun getClassName() = "EnumInput"
    
}