package me.anno.ui.base.menu

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.WindowStack
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.Search
import me.anno.ui.input.TextInput
import me.anno.ui.input.components.PureTextInput
import me.anno.utils.Color.mixARGB
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.levenshtein
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Utility for opening menus, like asking the user questions, or him selecting values for an enum from a dropdown.
 * */
object Menu {

    var paddingX = 10
    var paddingY = 10

    const val menuSeparator = "-----"

    @Suppress("unused")
    val menuSeparator1 = MenuOption(NameDesc(menuSeparator, "", "")) {}

    fun msg(windowStack: WindowStack, title: NameDesc) {
        val panel = TextPanel(title.name, style).setTooltip(title.desc)
        val window = openMenuByPanels(windowStack, NameDesc(), listOf(panel))
        if (window != null) {
            // window should be at the bottom center like an Android toast
            window.x = (windowStack.width - window.panel.width).shr(1)
            window.y = windowStack.height - window.panel.height - 10
            window.drawDirectly = true
        }
    }

    fun msg(title: NameDesc) {
        msg(GFX.someWindow.windowStack, title)
    }

    fun ask(windowStack: WindowStack, question: NameDesc, onYes: () -> Unit): Window? {
        val window = openMenu(windowStack, question, listOf(
            MenuOption(NameDesc("Yes", "", "ui.yes"), onYes),
            MenuOption(NameDesc("No", "", "ui.no")) {}
        ))
        window?.drawDirectly = true
        return window
    }

    fun ask(windowStack: WindowStack, question: NameDesc, onYes: () -> Unit, onNo: () -> Unit): Window? {
        val window = openMenu(
            windowStack, question, listOf(
                MenuOption(NameDesc("Yes", "", "ui.yes"), onYes),
                MenuOption(NameDesc("No", "", "ui.no"), onNo)
            )
        )
        window?.drawDirectly = true
        return window
    }

    fun askName(
        windowStack: WindowStack,
        title: NameDesc,
        value0: String,
        actionName: NameDesc,
        getColor: (String) -> Int,
        callback: (String) -> Unit
    ) = askName(
        windowStack,
        windowStack.mouseXi - paddingX,
        windowStack.mouseYi - paddingY,
        title, value0, actionName, getColor, callback
    )

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

        val textInput = PureTextInput(style)
        textInput.setText(value0, false)
        textInput.placeholder = title.name
        textInput.tooltip = title.desc
        textInput.setEnterListener {
            callback(textInput.value)
            close(textInput)
        }
        textInput.addChangeListener {
            textInput.textColor = getColor(it)
        }

        val submit = TextButton(actionName.name, style)
        submit.weight = 1f
        submit.tooltip = actionName.desc
        submit.addLeftClickListener {
            callback(textInput.value)
            close(textInput)
        }

        val cancel = TextButton("Cancel", style)
        cancel.weight = 1f
        cancel.addLeftClickListener { close(textInput) }

        val buttons = PanelListX(style)
        buttons += cancel
        buttons += submit

        val window = openMenuByPanels(windowStack, x, y, title, listOf(textInput, buttons))!!
        textInput.requestFocus()
        window.drawDirectly = true
        return window
    }

    fun close(panel: Panel) {
        val window = panel.window!!
        window.windowStack.remove(window)
        window.destroy()
        if (window.windowStack.isEmpty()) {
            window.windowStack.osWindow?.requestClose()
        }
    }

    private fun styleComplexEntry(button: TextPanel, option: ComplexMenuEntry, padding: Int, hover: Boolean) {
        button.tooltip = option.description
        button.enableHoverColor = hover
        button.padding.left = padding
        button.padding.right = padding
    }

    @Suppress("unused")
    fun openComplexMenu(
        windowStack: WindowStack, title: NameDesc,
        options: List<ComplexMenuEntry>
    ): Window? {
        return openComplexMenu(
            windowStack,
            windowStack.mouseXi - paddingX,
            windowStack.mouseYi - paddingY,
            title, options
        )
    }

    fun openComplexMenu(
        windowStack: WindowStack,
        x: Int, y: Int, title: NameDesc,
        options: List<ComplexMenuEntry>
    ): Window? {

        if (options.isEmpty()) return null
        val style = DefaultConfig.style.getChild("menu")

        val list = ArrayList<Panel>()
        val keyListeners = ExtraKeyListeners()

        val padding = 4
        for (index in options.indices) {
            val option = options[index]
            val name = option.title
            val action = (option as? ComplexMenuOption)?.action
            when {
                name == menuSeparator -> {
                    if (index != 0) {
                        list += SpacerPanel(0, 1, style)
                    }
                }
                option.isEnabled && action != null -> {
                    val magicIndex = keyListeners.findNextFreeIndex(name)
                    val button = object : TextPanel(name, style) {
                        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                            super.onDraw(x0, y0, x1, y1)
                            if (magicIndex in text.indices) {
                                underline(magicIndex, magicIndex + 1)
                            }
                        }
                    }
                    if (magicIndex in name.indices) {
                        keyListeners.bind(name[magicIndex].lowercaseChar()) {
                            action()
                            close(button)
                            true
                        }
                    }
                    button.addOnClickListener { _, _, _, key, long ->
                        if (key == Key.BUTTON_LEFT && !long) {
                            action()
                            close(button)
                            true
                        } else false
                    }
                    styleComplexEntry(button, option, padding, true)
                    list += button
                }
                option.isEnabled && option is ComplexMenuGroup -> {
                    lateinit var button: ComplexMenuGroupPanel
                    val magicIndex = keyListeners.findNextFreeIndex(name)
                    button = ComplexMenuGroupPanel(option, magicIndex, { close(button) }, style)
                    if (!list.size.hasFlag(1)) { // add soft stripes
                        button.backgroundColor = mixARGB(button.backgroundColor, button.textColor, 0.07f)
                    }
                    if (magicIndex in name.indices) {
                        val char = name[magicIndex].lowercaseChar()
                        keyListeners.bind(char) {
                            button.openMenu()
                            false
                        }
                    }
                    styleComplexEntry(button, option, padding, true)
                    list += button
                }
                else -> {
                    // disabled -> show it grayed-out
                    // if action is a group, add a small arrow
                    val name1 = if (option is ComplexMenuGroup) "$name →" else name
                    val button = TextPanel(name1, style)
                    button.textColor = mixARGB(button.textColor, 0x77777777, 0.5f)
                    button.focusTextColor = button.textColor
                    styleComplexEntry(button, option, padding, false)
                    list += button
                }
            }
        }

        return openMenuByPanels(windowStack, x, y, title, list, keyListeners.listeners)!!
    }

    @Suppress("unused")
    fun openMenuByPanels(
        windowStack: WindowStack,
        title: NameDesc,
        panels: List<Panel>,
        extraKeyListeners: Map<Char, () -> Boolean> = emptyMap(),
    ): Window? {
        return openMenuByPanels(
            windowStack,
            windowStack.mouseXi - paddingX,
            windowStack.mouseYi - paddingY,
            title, panels, extraKeyListeners
        )
    }

    fun openMenuByPanels(
        windowStack: WindowStack,
        x: Int, y: Int,
        title: NameDesc,
        panels: List<Panel>,
        extraKeyListeners: Map<Char, () -> Boolean> = emptyMap(),
    ): Window? {

        GFX.loadTexturesSync.push(true) // to calculate the correct size, which is needed for correct placement

        if (panels.isEmpty()) return null

        val style = DefaultConfig.style.getChild("menu")
        val list = PanelListY(style)

        val container = object : ScrollPanelY(list, Padding(1), style) {
            override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
                val window = window
                val char = codepoint.toChar()
                val entry = extraKeyListeners[char.lowercaseChar()]
                if (window in windowStack && entry?.invoke() == true) {
                    close(this)
                } else super.onCharTyped(x, y, codepoint)
            }
        }
        container.alignmentX = AxisAlignment.MIN
        container.alignmentY = AxisAlignment.MIN

        val window = Window(container, isTransparent = false, isFullscreen = false, windowStack, 1, 1)

        val padding = 4
        val titleValue = title.name
        if (titleValue.isNotEmpty()) {
            // make this window draggable
            val titlePanel = object : TextPanel(titleValue, style) {
                override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                    if (Input.isLeftDown) {
                        // move the window
                        window.x = clamp(
                            window.x + dx.roundToIntOr(),
                            0, windowStack.width - window.panel.width
                        )
                        window.y = clamp(
                            // we only can control the window at the top -> top needs to stay visible
                            window.y + dy.roundToIntOr(), 0,
                            windowStack.height - window.panel.height
                        )
                        window.panel.invalidateLayout()
                    } else super.onMouseMoved(x, y, dx, dy)
                }
            }
            titlePanel.tooltip = title.desc
            titlePanel.padding.left = padding
            titlePanel.padding.right = padding
            list += titlePanel
            list += SpacerPanel(0, 1, style)
        }

        // search panel
        var searchPanel: TextInput? = null

        // while useful for keyboard-only controls, it looks quite stupid to have a searchbar for only two items
        // todo when searching, look into ComplexMenuGroups, too
        val needsSearch = panels.size >= DefaultConfig["ui.search.minItems", 5]
        val originalOrder = HashMap<Panel, Int>()
        if (needsSearch) {
            val startIndex = list.children.size + 1
            val suggestions = DefaultConfig["ui.search.spellcheck", true]
            searchPanel = TextInput(Dict["Search", "ui.general.search"], "", suggestions, style)
            searchPanel.addChangeListener { searchTerm ->
                val search = Search(searchTerm)
                val children = list.children
                if (search.matchesEverything()) {
                    // make everything visible
                    for (i in startIndex until children.size) {
                        children[i].isVisible = true
                    }
                    // restore original order
                    children.sortBy { originalOrder[it] ?: -1 }
                } else {
                    for (i in startIndex until children.size) {
                        val child = children[i]
                        // check all text elements inside this panel for matches
                        child.isVisible = child.any {
                            it is TextPanel && (search.matches(it.text) || search.matches(it.tooltip))
                        }
                    }
                    // sort results by relevance
                    children.sortBy {
                        val id = originalOrder[it] ?: -1
                        if (id >= startIndex && it is TextPanel && it.isVisible) {
                            // find best match using levenshtein distance (text similarity)
                            val dist0 = searchTerm.levenshtein(it.text, true)
                            val tt = it.tooltip
                            val minDist = if (tt != null) {
                                val dist1 = searchTerm.levenshtein(tt, true)
                                min(dist0, dist1)
                            } else dist0
                            children.size + minDist
                        } else id // invisible things can be put to the end
                    }
                }
                list.invalidateLayout()
            }
            searchPanel.setEnterListener {
                val children = list.children
                // find the first element, and click it
                val chosen = children.subList(startIndex, children.size)
                    .firstOrNull { it.isVisible && it.canBeSeen }
                if (chosen != null) {
                    val deepest = chosen.getPanelAt(chosen.x, chosen.y)!!
                    deepest.requestFocus()
                    deepest.onMouseClicked(chosen.x.toFloat(), chosen.y.toFloat(), Key.BUTTON_LEFT, false)
                }
            }
            list += searchPanel
        }

        for (panel in panels) {
            list += panel
        }

        val children = list.children
        for (i in children.indices) {
            originalOrder[children[i]] = i
        }

        val maxWidth = max(300, windowStack.width)
        val maxHeight = max(300, windowStack.height)

        // could we do the calculation on another thread?
        container.calculateSize(maxWidth, maxHeight)
        container.setSize(min(container.minW, maxWidth), min(container.minH, maxHeight))

        window.x = clamp(x, 0, max(windowStack.width - container.width, 0))
        window.y = clamp(y, 0, max(windowStack.height - container.height, 0))

        container.forAllPanels { it.window = window }

        windowStack.add(window)
        (searchPanel ?: container).requestFocus()

        GFX.loadTexturesSync.pop()
        return window
    }

    @Suppress("unused")
    fun openComplexMenu(
        windowStack: WindowStack,
        x: Float, y: Float,
        title: NameDesc, options: List<ComplexMenuEntry>
    ) = openComplexMenu(windowStack, x.roundToIntOr(), y.roundToIntOr(), title, options)

    fun openMenu(windowStack: WindowStack, options: List<MenuOption>) =
        openMenu(windowStack, NameDesc(), options)

    fun openMenu(windowStack: WindowStack, title: NameDesc, options: List<MenuOption>): Window? =
        openMenu(windowStack, windowStack.mouseX - paddingX, windowStack.mouseY - paddingY, title, options)

    fun openMenu(
        windowStack: WindowStack,
        x: Int, y: Int, title: NameDesc, options: List<MenuOption>, delta: Int = 10
    ): Window? = openMenu(windowStack, x.toFloat(), y.toFloat(), title, options, delta)

    fun openMenu(
        windowStack: WindowStack,
        x: Float, y: Float, title: NameDesc, options: List<MenuOption>, delta: Int = 10
    ): Window? = openComplexMenu(windowStack, x.roundToIntOr() - delta, y.roundToIntOr() - delta, title,
        options.map { option -> option.toComplex() })
}