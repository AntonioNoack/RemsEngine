package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style

class EnumInput(title: String, startValue: String, val options: List<String>, style: Style): PanelListX(style){

    val titlePanel = TextPanel(title, style)
    val inputPanel = TextPanel(startValue, style.getChild("deep"))

    var changeListener = { value: String -> }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused = titlePanel.isInFocus || inputPanel.isInFocus
        if(focused) isSelectedListener?.invoke()
        inputPanel.visibility = if(focused) Visibility.VISIBLE else Visibility.GONE
        super.draw(x0, y0, x1, y1)
    }

    init {
        this += titlePanel
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

    override fun getCursor(): Long = Cursor.drag
    
}