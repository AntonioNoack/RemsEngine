package me.anno.ui.base.menu

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths
import me.anno.maths.Maths.mixARGB
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.Padding
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.base.constraints.WrapAlign
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.Search
import me.anno.ui.input.TextInput
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.utils.WindowStack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Menu {

    // todo sub menus

    var paddingX = 10
    var paddingY = 10

    const val menuSeparator = "-----"

    // used in Rem's Studio, maybe should be moved there
    @Suppress("unused")
    val menuSeparator1 = MenuOption(NameDesc(menuSeparator, "", "")) {}

    fun msg(windowStack: WindowStack, title: NameDesc) {
        openMenu(windowStack, listOf(MenuOption(title) {}))
    }

    fun ask(windowStack: WindowStack, question: NameDesc, onYes: () -> Unit) {
        openMenu(windowStack, question, listOf(
            MenuOption(NameDesc("Yes", "", "ui.yes"), onYes),
            MenuOption(NameDesc("No", "", "ui.no")) {}
        ))
    }

    fun ask(windowStack: WindowStack, question: NameDesc, onYes: () -> Unit, onNo: () -> Unit) {
        openMenu(
            windowStack, question, listOf(
                MenuOption(NameDesc("Yes", "", "ui.yes"), onYes),
                MenuOption(NameDesc("No", "", "ui.no"), onNo)
            )
        )
    }

    fun askName(
        windowStack: WindowStack,
        title: NameDesc,
        value0: String,
        actionName: NameDesc,
        getColor: (String) -> Int,
        callback: (String) -> Unit
    ) {
        // GFX.updateMousePosition()
        // windowStack.updateMousePosition()
        askName(
            windowStack,
            windowStack.mouseXi - paddingX,
            windowStack.mouseYi - paddingY,
            title, value0, actionName, getColor, callback
        )
    }

    fun askName(
        windowStack: WindowStack,
        x: Int, y: Int,
        title: NameDesc,
        value0: String,
        actionName: NameDesc,
        getColor: (String) -> Int,
        callback: (String) -> Unit
    ): Window {

        val style = DefaultConfig.style.getChild("menu")
        val panel = PureTextInput(style)
        panel.setText(value0, false)
        panel.placeholder = title.name
        panel.tooltip = title.desc
        panel.setEnterListener {
            callback(panel.text)
            close(panel)
        }
        panel.addChangeListener {
            panel.textColor = getColor(it)
        }
        val submit = TextButton(actionName.name, false, style)
        submit.tooltip = actionName.desc
        submit.addLeftClickListener {
            callback(panel.text)
            close(panel)
        }

        val cancel = TextButton("Cancel", false, style)
        cancel.addLeftClickListener { close(panel) }

        val buttons = PanelListX(style)
        buttons += cancel
        buttons += submit

        val window = openMenuByPanels(windowStack, x, y, title, listOf(panel, buttons))!!
        panel.requestFocus()
        return window

    }

    fun close(panel: Panel) {
        val window = panel.window!!
        window.windowStack.remove(window)
        window.destroy()
    }

    fun openMenuComplex(
        windowStack: WindowStack,
        x: Int,
        y: Int,
        title: NameDesc,
        options: List<ComplexMenuOption>
    ): Window? {

        if (options.isEmpty()) return null
        val style = DefaultConfig.style.getChild("menu")

        val list = ArrayList<Panel>()

        val padding = 4
        for (index in options.indices) {
            val option = options[index]
            val name = option.title
            val action = option.action
            when {
                name == menuSeparator -> {
                    if (index != 0) {
                        list += SpacerPanel(0, 1, style)
                    }
                }
                option.isEnabled -> {
                    val button = TextPanel(name, style)
                    button.addOnClickListener { _, _, mouseButton, long ->
                        if (action(mouseButton, long)) {
                            close(button)
                            true
                        } else false
                    }
                    button.tooltip = option.description
                    button.enableHoverColor = true
                    button.padding.left = padding
                    button.padding.right = padding
                    list += button
                }
                else -> {
                    val button = TextPanel(name, style)
                    button.textColor = mixARGB(button.textColor, 0x77777777, 0.5f)
                    button.focusTextColor = button.textColor
                    button.tooltip = option.description
                    button.padding.left = padding
                    button.padding.right = padding
                    list += button
                }
            }
        }

        return openMenuByPanels(windowStack, x, y, title, list)!!

    }

    @Suppress("unused")
    fun openMenuByPanels(windowStack: WindowStack, title: NameDesc, panels: List<Panel>): Window? {
        // GFX.updateMousePosition()
        // windowStack.updateMousePosition(window)
        return openMenuByPanels(
            windowStack,
            windowStack.mouseXi - paddingX,
            windowStack.mouseYi - paddingY,
            title, panels
        )
    }

    fun openMenuByPanels(
        windowStack: WindowStack,
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

        val window = Window(container, isTransparent = false, isFullscreen = false, windowStack, 1, 1)

        val padding = 4
        val titleValue = title.name
        if (titleValue.isNotEmpty()) {
            // make this window draggable
            // todo make it resizable somehow...
            val titlePanel = object : TextPanel(titleValue, style) {
                var leftDown = false
                var rightDown = false
                override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
                    if (button.isLeft) leftDown = true
                    if (button.isRight) rightDown = true
                }

                override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
                    if (button.isLeft) leftDown = false
                    if (button.isRight) rightDown = false
                }

                override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                    if (leftDown) {
                        // move the window
                        window.x += dx.roundToInt()
                        window.y += dy.roundToInt()
                        invalidateLayout()
                        return
                    } else if (rightDown) {
                        // todo scale somehow...
                    }
                    super.onMouseMoved(x, y, dx, dy)
                }
            }
            titlePanel.tooltip = title.desc
            titlePanel.padding.left = padding
            titlePanel.padding.right = padding
            list += titlePanel
            list += SpacerPanel(0, 1, style)
        }

        // search panel
        if (panels.size >= DefaultConfig["ui.search.minItems", 5]) {
            val startIndex = list.children.size + 1
            val suggestions = DefaultConfig["ui.search.spellcheck", true]
            val searchPanel = TextInput("Search", "", suggestions, style)
            searchPanel.addChangeListener { searchTerm ->
                val search = Search(searchTerm)
                val children = list.children
                for (child in children.subList(startIndex, children.size)) {
                    // check all text elements inside this panel for matches
                    child.visibility = Visibility[child.listOfAll.any {
                        it is TextPanel && (search.matches(it.text))
                    }]
                }
            }
            searchPanel.setEnterListener {
                val children = list.children
                // find the first element, and click it
                val chosen = children.subList(startIndex, children.size)
                    .firstOrNull { it.visibility == Visibility.VISIBLE && it.canBeSeen }
                if (chosen != null) {
                    val deepest = chosen.getPanelAt(chosen.x, chosen.y)!!
                    deepest.requestFocus()
                    deepest.onMouseClicked(chosen.x.toFloat(), chosen.y.toFloat(), MouseButton.LEFT, false)
                }
            }
            list += searchPanel
            searchPanel.requestFocus()
        }

        // todo in the future, we also could create/allow groups for faster access

        for (panel in panels) {
            list += panel
        }

        val maxWidth = max(300, windowStack.width)
        val maxHeight = max(300, windowStack.height)

        // could we do the calculation on another thread?
        container.calculateSize(maxWidth, maxHeight)
        container.setSize(min(container.minW, maxWidth), min(container.minH, maxHeight))

        window.x = Maths.clamp(x, 0, max(windowStack.width - container.w, 0))
        window.y = Maths.clamp(y, 0, max(windowStack.height - container.h, 0))

        container.forAllPanels { it.window = window }

        windowStack.add(window)
        GFX.loadTexturesSync.pop()

        return window

    }

    @Suppress("unused")
    fun openMenuComplex(
        windowStack: WindowStack,
        x: Float,
        y: Float,
        title: NameDesc,
        options: List<ComplexMenuOption>
    ) = openMenuComplex(windowStack, x.roundToInt(), y.roundToInt(), title, options)

    fun openMenu(windowStack: WindowStack, options: List<MenuOption>) =
        openMenu(windowStack, NameDesc(), options)

    fun openMenu(windowStack: WindowStack, title: NameDesc, options: List<MenuOption>): Window? {
        // GFX.updateMousePosition() // just in case; this method should only be called from the GFX thread
        // windowStack.updateMousePosition()
        return openMenu(windowStack, windowStack.mouseX - paddingX, windowStack.mouseY - paddingY, title, options)
    }

    fun openMenu(
        windowStack: WindowStack,
        x: Int, y: Int, title: NameDesc, options: List<MenuOption>, delta: Int = 10
    ) = openMenu(windowStack, x.toFloat(), y.toFloat(), title, options, delta)

    fun openMenu(
        windowStack: WindowStack,
        x: Float, y: Float, title: NameDesc, options: List<MenuOption>, delta: Int = 10
    ) = openMenuComplex(windowStack, x.roundToInt() - delta, y.roundToInt() - delta, title,
        options.map { option -> option.toComplex() })

}