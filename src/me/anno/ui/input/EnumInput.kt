package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.EnumValuePanel
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.types.Strings.camelCaseToTitle
import me.anno.utils.types.Strings.ifBlank2
import me.anno.utils.types.Strings.isNotBlank2

@Suppress("MemberVisibilityCanBePrivate")
open class EnumInput(
    val nameDesc: NameDesc, withTitle: Boolean, startValue: NameDesc,
    val options: List<NameDesc>, style: Style
) : PanelListX(style), InputPanel<NameDesc>, TextStyleable {

    constructor(startValue: NameDesc, options: List<NameDesc>, style: Style) :
            this(NameDesc.EMPTY, false, startValue, options, style)

    constructor(name: NameDesc, startValue: NameDesc, options: List<NameDesc>, style: Style) :
            this(name, name.name.isNotBlank2(), startValue, options, style)

    var lastIndex = options.indexOfFirst { it == startValue }

    val titleView = if (withTitle) TextPanel("${nameDesc.name}:", style) else null
    val inputPanel = EnumValuePanel(startValue, this, style)

    init {
        titleView?.enableHoverColor = true
        inputPanel.enableHoverColor = true
        tooltip = nameDesc.desc
    }

    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                titleView?.enableHoverColor = value
                inputPanel.enableHoverColor = value
                inputPanel.enableFocusColor = value
                invalidateDrawing()
            }
        }

    override fun onUpdate() {
        val alpha = if (isInputAllowed) 255 else 127
        val titleView = titleView
        if (titleView != null) titleView.textColor = titleView.textColor.withAlpha(alpha)
        inputPanel.textColor = inputPanel.textColor.withAlpha(alpha)
        super.onUpdate()
    }

    override var value =
        options.firstOrNull { it == startValue } ?: options.first()

    fun setValue1(option: NameDesc, index: Int, notify: Boolean = true) {
        inputPanel.text = option.name
        inputPanel.tooltip = option.desc
        lastIndex = index
        value = option
        if (notify) changeListener(option, index, options)
        invalidateLayout() // layout, because the drawn length can change
    }

    override fun setValue(newValue: NameDesc, mask: Int, notify: Boolean): Panel {
        // what if the index is not found?
        setValue1(newValue, options.indexOf(newValue), notify)
        return this
    }

    fun moveDown(direction: Int) {
        if (isInputAllowed) {
            val oldValue = inputPanel.text
            val index = lastIndex + direction
            val index2 = (index + 2 * options.size) % options.size
            val newValue = options[index2]
            if (oldValue != newValue.name) {
                setValue1(newValue, index2)
            }
        }
    }

    var changeListener = { _: NameDesc, _: Int, _: List<NameDesc> -> }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused = titleView?.isInFocus == true || inputPanel.isInFocus
        if (focused) isSelectedListener?.invoke()
        super.draw(x0, y0, x1, y1)
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

    fun setChangeListener(listener: (value: NameDesc, index: Int, values: List<NameDesc>) -> Unit): EnumInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): EnumInput {
        isSelectedListener = listener
        return this
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (isInputAllowed) openMenu(
            windowStack, this.x, this.y,
            NameDesc("Select the %1", "", "ui.input.enum.menuTitle")
                .with("%1", nameDesc.name.ifBlank2("Value")),
            options.mapIndexed { index, option ->
                MenuOption(option) {
                    setValue1(option, index)
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

    override fun getCursor(): Cursor? {
        return if (isInputAllowed) Cursor.drag
        else null
    }

    override var textSize: Float
        get() = inputPanel.textSize
        set(value) {
            inputPanel.textSize = value
            titleView?.textSize = value
        }

    override var textColor: Int
        get() = inputPanel.textColor
        set(value) {
            inputPanel.textColor = value
            titleView?.textColor = value
        }

    override var isBold: Boolean
        get() = inputPanel.isBold
        set(value) {
            inputPanel.isBold = value
            titleView?.isBold = value
        }

    override var isItalic: Boolean
        get() = inputPanel.isItalic
        set(value) {
            inputPanel.isItalic = value
            titleView?.isItalic = value
        }

    override fun clone(): EnumInput {
        val clone = EnumInput(nameDesc, titleView != null, value, options, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyIntoExceptChildren(dst)
    }

    companion object {

        fun createInput(title: String, value: Enum<*>, style: Style): EnumInput {
            val values = getEnumConstants(value.javaClass)
            val ttt = value::class.simpleName?.camelCaseToTitle() ?: "?"
            val valueName = enumToNameDesc(value)
            return EnumInput(NameDesc(title, ttt, ""), valueName, values.map(::enumToNameDesc), style)
        }

        fun getEnumConstants(clazz: Class<*>): List<Enum<*>> {
            return if (clazz.isEnum) clazz.enumConstants!!.toList().filterIsInstance2(Enum::class)
            else getEnumConstants(clazz.superclass)
        }

        fun enumToNameDesc(instance: Enum<*>): NameDesc {
            val reflections = getReflections(instance)
            val naming = reflections[instance, "naming"] as? NameDesc
            if (naming != null) return naming
            val desc0 = reflections[instance, "desc"]
                ?: reflections[instance, "description"]
            val desc1 = desc0 as? String
                ?: return NameDesc(instance.toString())
            return NameDesc(instance.toString(), desc1, "")
        }
    }
}