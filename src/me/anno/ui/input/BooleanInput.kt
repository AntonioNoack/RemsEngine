package me.anno.ui.input

import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style

// checkbox with title
/**
 * in a Transform child class, all inputs should be created using the VI function, if possible,
 * because it forces the programmer to set a tool tip text
 * */
class BooleanInput(title: String, startValue: Boolean, style: Style): PanelListX(style){

    val checkView = Checkbox(startValue, style.getSize("fontSize",10), style)
    val titleView = TextPanel("$title:", style)

    init {
        this += titleView
        titleView.padding.right = 5
        this += checkView
        this += WrapAlign.LeftTop
    }

    fun setChangeListener(listener: (value: Boolean) -> Unit): BooleanInput {
        checkView.setChangeListener(listener)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if(isInFocus) isSelectedListener?.invoke()
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): BooleanInput {
        isSelectedListener = listener
        return this
    }

    override fun onCopyRequested(x: Float, y: Float) = checkView.isChecked.toString()
    override fun getClassName() = "BooleanInput"

}