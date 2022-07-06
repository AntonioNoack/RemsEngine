package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.components.EnumValuePanel
import me.anno.ui.style.Style
import me.anno.utils.Color.withAlpha
import me.anno.utils.strings.StringHelper.camelCaseToTitle
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

@Suppress("MemberVisibilityCanBePrivate")
open class EnumInput(
    val title: String, withTitle: Boolean, startValue: String,
    val options: List<NameDesc>, style: Style
) : PanelListX(style), InputPanel<NameDesc> {

    constructor(title: String, ttt: String, startValue: String, options: List<NameDesc>, style: Style) :
            this(title, true, startValue, options, style) {
        tooltip = ttt
    }

    constructor(
        title: String,
        ttt: String,
        dictPath: String,
        startValue: String,
        options: List<NameDesc>,
        style: Style
    ) : this(Dict[title, dictPath], true, startValue, options, style) {
        tooltip = Dict[ttt, "$dictPath.desc"]
    }

    constructor(
        title: String,
        ttt: String,
        dictPath: String,
        startValue: NameDesc,
        options: List<NameDesc>,
        style: Style
    ) : this(Dict[title, dictPath], true, startValue.name, options, style) {
        tooltip = Dict[ttt, "$dictPath.desc"]
    }

    constructor(name: NameDesc, startValue: NameDesc, options: List<NameDesc>, style: Style) :
            this(name.name, true, startValue.name, options, style) {
        tooltip = name.desc
    }

    var lastIndex = options.indexOfFirst { it.name == startValue }

    val titleView = if (withTitle) TextPanel("$title:", style) else null
    val inputPanel = EnumValuePanel(startValue, this, style)

    init {
        titleView?.enableHoverColor = true
        inputPanel.enableHoverColor = true
    }

    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override fun tickUpdate() {
        val alpha = if (isInputAllowed) 1f else 0.5f
        val titleView = titleView
        if (titleView != null) titleView.textColor = titleView.textColor.withAlpha(alpha)
        inputPanel.textColor = inputPanel.textColor.withAlpha(alpha)
        super.tickUpdate()
    }

    override var lastValue =
        options.firstOrNull { it.name == startValue }
            ?: options.firstOrNull { it.englishName == startValue }
            ?: options.first()

    fun setValue(option: NameDesc, index: Int, notify: Boolean = true) {
        inputPanel.text = option.name
        inputPanel.tooltip = option.desc
        lastIndex = index
        lastValue = option
        if (notify) changeListener(option.name, index, options)
        invalidateLayout() // layout, because the drawn length can change
    }

    override fun setValue(value: NameDesc, notify: Boolean): EnumInput {
        // what if the index is not found?
        setValue(value, options.indexOf(value), notify)
        return this
    }

    fun moveDown(direction: Int) {
        if (isInputAllowed) {
            val oldValue = inputPanel.text
            val index = lastIndex + direction
            val index2 = (index + 2 * options.size) % options.size
            val newValue = options[index2]
            if (oldValue != newValue.name) {
                setValue(newValue, index2)
            }
        }
    }

    var changeListener = { _: String, _: Int, _: List<NameDesc> -> }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused = titleView?.isInFocus == true || inputPanel.isInFocus
        if (focused) isSelectedListener?.invoke()
        super.onDraw(x0, y0, x1, y1)
    }

    init {
        if (titleView != null) {
            this += titleView
            titleView.disableFocusColors()
        }
        this += inputPanel
    }

    fun setOption(index: Int) {
        if (index < 0) return
        inputPanel.text = options[index].name
    }

    fun setChangeListener(listener: (value: String, index: Int, values: List<NameDesc>) -> Unit): EnumInput {
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
            windowStack, this.x, this.y,
            NameDesc("Select the %1", "", "ui.input.enum.menuTitle")
                .with("%1", title),
            options.mapIndexed { index, option ->
                MenuOption(option) {
                    setValue(option, index)
                }
            })
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return if (isInputAllowed) {
            when (action) {
                "Up" -> {
                    setIndex((lastIndex - 1 + options.size) % options.size)
                    true
                }
                "Down" -> {
                    setIndex((lastIndex - 1 + options.size) % options.size)
                    true
                }
                else -> super.onGotAction(x, y, dx, dy, action, isContinuous)
            }
        } else super.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    fun setIndex(index: Int) {
        setValue(options[index], index, true)
    }

    override fun getCursor(): Long = Cursor.drag

    companion object {

        fun createInput(title: String, value: Enum<*>, style: Style): EnumInput {
            val values = getEnumConstants(value.javaClass)
            val ttt = value.javaClass.simpleName.camelCaseToTitle()
            val valueName = enumToNameDesc(value).name
            return EnumInput(title, ttt, valueName, values.map { enumToNameDesc(it) }, style)
        }

        fun getEnumConstants(clazz: Class<*>): Array<out Any> {
            return if (clazz.isEnum) clazz.enumConstants!!
            else getEnumConstants(clazz.superclass)
        }

        fun enumToNameDesc(instance: Any): NameDesc {
            val clazz = instance::class
            val naming = clazz.memberProperties
                .firstOrNull { it.name == "naming" }?.getter?.call(instance) as? NameDesc
            if (naming != null) return naming
            val desc = clazz.memberProperties
                .firstOrNull { it.name == "desc" || it.name == "description" }
                ?: return NameDesc(instance.toString())
            val desc2 = (desc as KProperty<*>).getter.call(instance)?.toString() ?: ""
            return NameDesc(instance.toString(), desc2, "")
        }

    }

}