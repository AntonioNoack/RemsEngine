package me.anno.ui.input

import me.anno.language.translation.Dict
import me.anno.ui.base.TextPanel
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.input.components.Checkbox
import me.anno.ui.style.Style

/**
 * Checkbox with title
 * in a Transform child class, all inputs should be created using the VI function, if possible,
 * because it forces the programmer to set a tool tip text
 * */
class BooleanInput(val title: String, startValue: Boolean, style: Style) : PanelListX(style) {

    constructor(title: String, description: String, dictPath: String, startValue: Boolean, style: Style) :
            this(Dict[title, dictPath], Dict[description, "$dictPath.desc"], startValue, style)

    constructor(title: String, description: String, startValue: Boolean, style: Style) :
            this(title, startValue, style) {
        setTooltip(description)
    }

    private val titleView = TextPanel("$title:", style)
    private val checkView = Checkbox(startValue, style.getSize("fontSize", 10), style)

    init {
        this += titleView
        titleView.enableHoverColor = true
        titleView.padding.right = 5
        titleView.focusTextColor = titleView.textColor
        this += checkView
        this += WrapAlign.LeftTop
    }

    fun setChangeListener(listener: (value: Boolean) -> Unit): BooleanInput {
        checkView.setChangeListener(listener)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if (isInFocus) isSelectedListener?.invoke()
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): BooleanInput {
        isSelectedListener = listener
        return this
    }

    override fun onCopyRequested(x: Float, y: Float) = checkView.isChecked.toString()

}