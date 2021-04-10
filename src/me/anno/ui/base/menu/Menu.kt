package me.anno.ui.base.menu

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.Window
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.ui.base.Panel
import me.anno.ui.base.SpacePanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.components.PureTextInput
import me.anno.utils.Maths
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Menu {

    const val menuSeparator = "-----"
    val menuSeparator1 = MenuOption(NameDesc(menuSeparator, "", "")) {}

    fun msg(title: NameDesc) {
        openMenu(listOf(MenuOption(title) {}))
    }

    fun ask(question: NameDesc, onYes: () -> Unit) {
        openMenu(
            Input.mouseX, Input.mouseY, question, listOf(
                MenuOption(NameDesc("Yes", "", "ui.yes"), onYes),
                MenuOption(NameDesc("No", "", "ui.no")) {}
            ))
    }

    fun ask(question: NameDesc, onYes: () -> Unit, onNo: () -> Unit) {
        openMenu(
            Input.mouseX, Input.mouseY, question, listOf(
                MenuOption(NameDesc("Yes", "", "ui.yes"), onYes),
                MenuOption(NameDesc("No", "", "ui.no"), onNo)
            )
        )
    }

    fun askName(
        x: Int, y: Int,
        title: NameDesc,
        value0: String,
        actionName: NameDesc,
        getColor: (String) -> Int,
        callback: (String) -> Unit
    ) {

        lateinit var window: Window
        fun close() {
            GFX.windowStack.remove(window)
            window.destroy()
        }

        val style = DefaultConfig.style.getChild("menu")
        val panel = PureTextInput(style)
        panel.text = value0
        panel.updateChars(false)
        panel.placeholder = title.name
        panel.setTooltip(title.desc)
        panel.setEnterListener {
            callback(panel.text)
            close()
        }
        panel.setChangeListener {
            panel.textColor = getColor(it)
        }

        val submit = TextButton(actionName.name, false, style)
            .setTooltip(actionName.desc)
            .setSimpleClickListener {
                callback(panel.text)
                close()
            }

        val cancel = TextButton("Cancel", false, style)
            .setSimpleClickListener { close() }

        val buttons = PanelListX(style)
        buttons += cancel
        buttons += submit

        window = openMenuComplex2(x, y, title, listOf(panel, buttons))!!

    }

    fun openMenuComplex(
        x: Int,
        y: Int,
        title: NameDesc,
        options: List<ComplexMenuOption>
    ) {

        if (options.isEmpty()) return
        val style = DefaultConfig.style.getChild("menu")

        lateinit var window: Window
        fun close() {
            GFX.windowStack.remove(window)
            window.destroy()
        }

        val list = ArrayList<Panel>()

        val padding = 4
        for ((index, element) in options.withIndex()) {
            val name = element.title
            val action = element.action
            if (name == menuSeparator) {
                if (index != 0) {
                    list += SpacePanel(0, 1, style)
                }
            } else {
                val buttonView = TextPanel(name, style)
                buttonView.setOnClickListener { _, _, button, long ->
                    if (action(button, long)) {
                        close()
                    }
                }
                buttonView.setTooltip(element.description)
                buttonView.enableHoverColor = true
                buttonView.padding.left = padding
                buttonView.padding.right = padding
                list += buttonView
            }
        }

        window = openMenuComplex2(x, y, title, list)!!

    }

    fun openMenuComplex2(title: NameDesc, panels: List<Panel>) =
        openMenuComplex2(Input.mouseX.toInt() - 10, Input.mouseY.toInt() - 10, title, panels)

    fun openMenuComplex2(
        x: Int,
        y: Int,
        title: NameDesc,
        panels: List<Panel>
    ): Window? {

        GFX.loadTexturesSync.push(true) // to calculate the correct size, which is needed for correct placement
        if (panels.isEmpty()) return null
        val style = DefaultConfig.style.getChild("menu")
        val list = PanelListY(style)
        list += WrapAlign.LeftTop
        val container = ScrollPanelY(list, Padding(1), style, AxisAlignment.MIN)
        container += WrapAlign.LeftTop

        val window = Window(container, false, 1, 1)

        val padding = 4
        val titleValue = title.name
        if (titleValue.isNotEmpty()) {
            // make this window draggable
            // todo make it resizable somehow...
            val titlePanel = object: TextPanel(titleValue, style){
                var leftDown = false
                var rightDown = false
                override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
                    if(button.isLeft) leftDown = true
                    if(button.isRight) rightDown = true
                }

                override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
                    if(button.isLeft) leftDown = false
                    if(button.isRight) rightDown = false
                }

                override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                    if(leftDown){
                        // move the window
                        window.x += dx.roundToInt()
                        window.y += dy.roundToInt()
                        invalidateLayout()
                    } else if(rightDown){
                        // todo scale somehow...

                    }
                }
            }
            titlePanel.setTooltip(title.desc)
            titlePanel.padding.left = padding
            titlePanel.padding.right = padding
            list += titlePanel
            list += SpacePanel(0, 1, style)
        }

        for (panel in panels) {
            list += panel
        }

        val maxWidth = max(300, GFX.width)
        val maxHeight = max(300, GFX.height)
        container.calculateSize(maxWidth, maxHeight)
        container.applyPlacement(min(container.minW, maxWidth), min(container.minH, maxHeight))

        window.x = Maths.clamp(x, 0, max(GFX.width - container.w, 0))
        window.y = Maths.clamp(y, 0, max(GFX.height - container.h, 0))

        container.listOfAll { it.window = window }

        GFX.windowStack.add(window)
        GFX.loadTexturesSync.pop()

        return window

    }

    fun openMenuComplex(
        x: Float,
        y: Float,
        title: NameDesc,
        options: List<ComplexMenuOption>,
        delta: Int = 10
    ) {
        openMenuComplex(x.roundToInt() - delta, y.roundToInt() - delta, title, options)
    }

    fun openMenu(options: List<MenuOption>) {
        openMenu(Input.mouseX, Input.mouseY, NameDesc(), options)
    }

    fun openMenu(title: NameDesc, options: List<MenuOption>) {
        openMenu(Input.mouseX, Input.mouseY, title, options)
    }

    fun openMenu(x: Int, y: Int, title: NameDesc, options: List<MenuOption>, delta: Int = 10) {
        return openMenu(x.toFloat(), y.toFloat(), title, options, delta)
    }

    fun openMenu(
        x: Float, y: Float,
        title: NameDesc,
        options: List<MenuOption>,
        delta: Int = 10
    ) {
        openMenuComplex(
            x.roundToInt() - delta,
            y.roundToInt() - delta,
            title,
            options.map { option -> option.toComplex() })
    }

}