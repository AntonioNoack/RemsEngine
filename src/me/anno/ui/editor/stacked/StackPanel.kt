package me.anno.ui.editor.stacked

import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX.openMenu
import me.anno.input.MouseButton
import me.anno.objects.Inspectable
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.PropertyInspector
import me.anno.ui.editor.SettingCategory

/**
 * done allow the user to add fields
 * done allow the user to customize fields
 * todo allow the user to remove fields
 * todo reorder fields by dragging up/down
 * todo copy fields
 * todo paste fields
 * todo add left-padding to all fields...
 * */
abstract class StackPanel(
    title: String,
    tooltipText: String,
    val options: List<Option>,
    val values: List<Option>
) : PanelListY(style) {

    val content = PanelListY(style)

    class Option private constructor(
        val title: String,
        val tooltipText: String,
        val value0: Inspectable?,
        val generator: () -> Inspectable
    ) {
        constructor(title: String, tooltipText: String, value0: Inspectable) : this(
            title, tooltipText, value0,
            { value0 }
        )

        constructor(title: String, tooltipText: String, generator: () -> Inspectable) : this(
            title, tooltipText, null, generator
        )
    }

    class OptionPanel(
        val sp: StackPanel,
        title: String,
        tooltipText: String,
        val value: Inspectable
    ) : SettingCategory(title, style) {

        init {
            setTooltip(tooltipText)
            PropertyInspector.createInspector(value, content, style)
        }

        override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
            when {
                button.isRight -> {
                    val index = indexInParent
                    openMenu(
                        sp.options.map { option ->
                            "Prepend ${option.title}" to {
                                sp.addComponent(option, index, true)
                            }
                        } + ("Remove Component" to {
                            sp.removeComponent(value)
                        })
                    )
                }
                else -> super.onMouseClicked(x, y, button, long)
            }
        }

        override fun onDeleteKey(x: Float, y: Float) {
            sp.removeComponent(value)
        }

    }

    val title = object: TextPanel(title, style){
        init {
            focusTextColor = textColor
        }
        override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
            if(button.isLeft && !long){
                val isHidden = this@StackPanel.children.getOrNull(1)?.visibility == Visibility.VISIBLE
                val visibility = if(isHidden) Visibility.VISIBLE else Visibility.GONE
                this@StackPanel.children.forEachIndexed { index, panel ->
                    if(index > 0){
                        panel.visibility = visibility
                    }
                }
            } else super.onMouseClicked(x, y, button, long)
        }
    }

    init {
        add(this.title)
        add(PanelContainer(content, Padding(10,0,0,0), style))
        values.forEachIndexed { index, it ->
            addComponent(it, index, false)
        }
        setTooltip(tooltipText)
    }

    fun showMenu() {
        openMenu(
            options.map { option ->
                "Append ${option.title}" to {
                    addComponent(option, content.children.size, true)
                }
            }
        )
    }

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isRight || long -> {
                showMenu()
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun addComponent(option: Option, index: Int, notify: Boolean) {
        val component = option.value0 ?: option.generator()
        content.children.add(index, OptionPanel(this, option.title, option.tooltipText, component))
        if (notify) onAddComponent(component, index)
    }

    abstract fun onAddComponent(component: Inspectable, index: Int)

    fun removeComponent(component: Inspectable) {
        content.children.removeIf { it is OptionPanel && it.value === component }
        onRemoveComponent(component)
    }

    abstract fun onRemoveComponent(component: Inspectable)

}