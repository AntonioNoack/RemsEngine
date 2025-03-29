package me.anno.ui.base.menu

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.engine.Events.addEvent
import me.anno.gpu.GFX
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.Window
import me.anno.ui.WindowStack
import me.anno.ui.base.Search
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.buttons.TextButton
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.ui.input.TextInput
import me.anno.ui.input.components.PureTextInput
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.algorithms.Recursion
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Strings.isNotBlank2
import me.anno.utils.types.Strings.levenshtein
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for opening menus, like asking the user questions, or him selecting values for an enum from a dropdown.
 * */
object Menu {

    private val LOGGER = LogManager.getLogger(Menu::class)

    const val MENU_SEPARATOR = "-----"

    var paddingX = 10
    var paddingY = 10

    private val buttonPadding = 4

    val menuSeparator1 = MenuOption(NameDesc(MENU_SEPARATOR, "", "")) {}

    // like in Android
    val shortDurationMillis get() = 2_000L
    val longDurationMillis get() = 3_500L

    /**
     * show an Android-toast-like message to the user
     * */
    fun msg(windowStack: WindowStack, nameDesc: NameDesc, showLong: Boolean = true) {
        val panel = TextPanel(nameDesc.name, style).setTooltip(nameDesc.desc)
        val window = openMenuByPanels(windowStack, NameDesc.EMPTY, listOf(panel))
        if (window != null) {
            // window should be at the bottom center like an Android toast
            val offsetForProgressBars = GFX.someWindow.progressbarHeightSum
            val paddingBottom = 10
            window.x = (windowStack.width - window.panel.width).shr(1)
            window.y = windowStack.height - (window.panel.height + paddingBottom + offsetForProgressBars)
            window.drawDirectly = true
            val duration = if (showLong) longDurationMillis else shortDurationMillis
            addEvent(duration) { window.close(false) }
        }
    }

    /**
     * show an Android-toast-like message to the user
     * */
    fun msg(nameDesc: NameDesc, showLong: Boolean = true) {
        msg(GFX.someWindow.windowStack, nameDesc, showLong)
    }

    /**
     * let the user confirm something (like JavaScript confirm())
     * */
    fun ask(windowStack: WindowStack, question: NameDesc, onYes: () -> Unit): Window? {
        val window = openMenu(
            windowStack, question, listOf(
            MenuOption(NameDesc("Yes", "", "ui.yes"), onYes),
            MenuOption(NameDesc("No", "", "ui.no")) {}
        ))
        window?.drawDirectly = true
        return window
    }

    /**
     * ask the user a yes/no-question; they click the menu away, and then it's neither yes nor no
     * */
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

    /**
     * ask the user for a name;
     * names can be given tints/colors to indicate invalidate or incomplete names
     * */
    fun askName(
        windowStack: WindowStack,
        nameDesc: NameDesc,
        value0: String,
        actionName: NameDesc,
        getColor: (String) -> Int,
        callback: (String) -> Unit
    ) = askName(
        windowStack,
        windowStack.mouseXi - paddingX,
        windowStack.mouseYi - paddingY,
        nameDesc, value0, actionName, getColor, callback
    )

    /**
     * ask the user for a name;
     * names can be given tints/colors to indicate invalidate or incomplete names
     * */
    fun askName(
        windowStack: WindowStack,
        x: Int, y: Int,
        nameDesc: NameDesc,
        value0: String,
        actionName: NameDesc,
        getColor: (String) -> Int,
        callback: (String) -> Unit
    ): Window {

        val style = DefaultConfig.style.getChild("menu")

        val textInput = PureTextInput(style)
        textInput.setText(value0, false)
        textInput.placeholder = nameDesc.name
        textInput.tooltip = nameDesc.desc
        textInput.setEnterListener {
            callback(textInput.value)
            close(textInput)
        }
        textInput.textColor = getColor(value0)
        textInput.addChangeListener {
            textInput.textColor = getColor(it)
        }

        val submit = TextButton(NameDesc(actionName.name), style)
        submit.weight = 1f
        submit.tooltip = actionName.desc
        submit.addLeftClickListener {
            callback(textInput.value)
            close(textInput)
        }

        val cancel = TextButton(NameDesc("Cancel"), style)
        cancel.weight = 1f
        cancel.addLeftClickListener { close(textInput) }

        val buttons = PanelListX(style)
        buttons += cancel
        buttons += submit

        val window = openMenuByPanels(windowStack, x, y, nameDesc, listOf(textInput, buttons))!!
        textInput.requestFocus()
        window.drawDirectly = true
        return window
    }

    /**
     * ask the user to rename a file; shows green/yellow/red tint depending on
     * whether the new name is valid and a file with that name already exists
     * */
    fun askRename(
        windowStack: WindowStack,
        nameDesc: NameDesc,
        value0: String,
        actionName: NameDesc,
        folder: FileReference,
        callback: (FileReference) -> Unit
    ) = askRename(
        windowStack,
        windowStack.mouseXi - paddingX,
        windowStack.mouseYi - paddingY,
        nameDesc, value0, actionName, folder, callback
    )

    /**
     * ask the user to rename a file; shows green/yellow/red tint depending on
     * whether the new name is valid and a file with that name already exists
     * */
    fun askRename(
        windowStack: WindowStack,
        x: Int, y: Int,
        nameDesc: NameDesc,
        value0: String,
        actionName: NameDesc,
        folder: FileReference,
        callback: (FileReference) -> Unit
    ): Window {
        return askName(windowStack, x, y, nameDesc, value0, actionName, { name ->
            val validName = name.toAllowedFilename()
            if (validName != name) {
                0xff0000
            } else {
                val sibling = folder.getChildUnsafe(name, false)
                if (sibling.exists) {
                    0xffff00
                } else {
                    0x00ff00
                }
            }.withAlpha(255)
        }, { name ->
            val validName = name.toAllowedFilename()
            val result = if (validName == name) {
                folder.getChildUnsafe(name, false)
            } else {
                LOGGER.warn("Ignoring invalid file name $validName")
                InvalidRef
            }
            callback(result)
        })
    }

    /**
     * close the window containing the panel;
     * be careful with this method, because if your panel is just embedded somewhere,
     * you might close important menus or similar
     * */
    fun close(panel: Panel) {
        panel.window?.close(false)
    }

    private fun styleComplexEntry(button: TextPanel, option: ComplexMenuEntry, hover: Boolean) {
        button.tooltip = option.nameDesc.desc
        button.enableHoverColor = hover
        button.padding.left = buttonPadding
        button.padding.right = buttonPadding
    }

    fun openComplexMenu(
        windowStack: WindowStack, nameDesc: NameDesc,
        options: List<ComplexMenuEntry>
    ): Window? {
        return openComplexMenu(
            windowStack,
            windowStack.mouseXi - paddingX,
            windowStack.mouseYi - paddingY,
            nameDesc, options
        )
    }

    fun openComplexMenu(
        windowStack: WindowStack,
        x: Int, y: Int, nameDesc: NameDesc,
        options: List<ComplexMenuEntry>
    ): Window? {

        if (options.isEmpty()) return null
        val optionsI = if (options.any2 { it is ComplexMenuGroup }) {
            options + ComplexMenuOption(NameDesc("Search All")) {
                val allOptions = ArrayList<ComplexMenuOption>()
                Recursion.processRecursive(options) { list, remaining ->
                    for (i in list.indices) {
                        when (val it = list[i]) {
                            is ComplexMenuGroup -> remaining.add(it.children)
                            is ComplexMenuOption -> allOptions.add(it)
                        }
                    }
                }
                openComplexMenu(windowStack, x, y, nameDesc, allOptions)
            }
        } else options

        val style = DefaultConfig.style.getChild("menu")

        val list = ArrayList<Panel>()
        val keyListeners = ExtraKeyListeners()

        for (index in optionsI.indices) {
            val option = optionsI[index]
            val optionI = option.nameDesc
            val name = optionI.name
            when {
                optionI.englishName == MENU_SEPARATOR -> {
                    if (index != 0) {
                        list += SpacerPanel(0, 1, style)
                    }
                }
                option.isEnabled && option is ComplexMenuOption -> {
                    val magicIndex = keyListeners.findNextFreeIndex(name)
                    val button = UnderlinedTextPanel(optionI, magicIndex, style)
                    if (magicIndex in name.indices) {
                        keyListeners.bind(name[magicIndex].lowercaseChar()) {
                            option.action()
                            close(button)
                            true
                        }
                    }
                    defineActionListener(button, option.action)
                    styleComplexEntry(button, option, true)
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
                    styleComplexEntry(button, option, true)
                    list += button
                }
                else -> list += createDisabledButton(option)
            }
        }

        return openMenuByPanels(windowStack, x, y, nameDesc, list, keyListeners.listeners)!!
    }

    class UnderlinedTextPanel(optionI: NameDesc, val magicIndex: Int, style: Style) : TextPanel(optionI, style) {
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            super.draw(x0, y0, x1, y1)
            if (magicIndex in text.indices) {
                underline(magicIndex, magicIndex + 1)
            }
        }
    }

    private fun createDisabledButton(option: ComplexMenuEntry): Panel {
        val optionI = option.nameDesc
        val name = optionI.name
        // disabled -> show it grayed-out
        // if action is a group, add a small arrow
        val name1 = if (option is ComplexMenuGroup)
            NameDesc("$name →", optionI.desc, "") else optionI
        val button = TextPanel(name1, style)
        button.textColor = mixARGB(button.textColor, 0x77777777, 0.5f)
        button.focusTextColor = button.textColor
        styleComplexEntry(button, option, false)
        return button
    }

    private fun defineActionListener(button: TextPanel, action: () -> Unit) {
        button.addOnClickListener { _, _, _, key, long ->
            if (key == Key.BUTTON_LEFT && !long) {
                action()
                close(button)
                true
            } else false
        }
    }

    fun openMenuByPanels(
        windowStack: WindowStack,
        nameDesc: NameDesc,
        panels: List<Panel>,
        extraKeyListeners: Map<Char, () -> Boolean> = emptyMap(),
    ): Window? {
        return openMenuByPanels(
            windowStack,
            windowStack.mouseXi - paddingX,
            windowStack.mouseYi - paddingY,
            nameDesc, panels, extraKeyListeners
        )
    }

    fun needsSearch(count: Int): Boolean {
        return count >= DefaultConfig["ui.search.minItems", 5]
    }

    fun openMenuByPanels(
        windowStack: WindowStack,
        x: Int, y: Int,
        nameDesc: NameDesc,
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
        val titleValue = nameDesc.name
        if (titleValue.isNotEmpty()) {
            // make this window draggable
            val titlePanel = MoveableTitlePanel(nameDesc, style)
            titlePanel.padding.left = padding
            titlePanel.padding.right = padding
            list += titlePanel
            list += SpacerPanel(0, 1, style)
        }

        // search panel
        var searchPanel: TextInput? = null

        // while useful for keyboard-only controls, it looks quite stupid to have a searchbar for only two items
        val originalOrder = HashMap<Panel, Int>()
        if (needsSearch(panels.size)) {
            val startIndex = list.children.size + 1
            val suggestions = DefaultConfig["ui.search.spellcheck", true]
            searchPanel = TextInput(NameDesc("Search", "", "ui.general.search"), "", suggestions, style)
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
                            it is TextPanel && search.matches(listOf(it.text, it.tooltip))
                        }
                    }
                    // sort results by relevance
                    children.sortBy {
                        val id = originalOrder[it] ?: -1
                        if (id >= startIndex && it is TextPanel && it.isVisible) {
                            // find best match using levenshtein distance (text similarity)
                            val dist0 = searchTerm.levenshtein(it.text, true)
                            val tt = it.tooltip
                            val minDist = if (tt.isNotBlank2()) {
                                val dist1 = searchTerm.levenshtein(tt, true)
                                min(dist0, dist1)
                            } else dist0
                            children.size + minDist
                        } else id // invisible things can be put to the end
                    }
                }
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

        windowStack.push(window)
        (searchPanel ?: container).requestFocus()

        GFX.loadTexturesSync.pop()
        return window
    }

    @Suppress("unused")
    fun openComplexMenu(
        windowStack: WindowStack,
        x: Float, y: Float,
        nameDesc: NameDesc, options: List<ComplexMenuEntry>
    ) = openComplexMenu(windowStack, x.roundToIntOr(), y.roundToIntOr(), nameDesc, options)

    fun openMenu(windowStack: WindowStack, options: List<MenuOption>) =
        openMenu(windowStack, NameDesc.EMPTY, options)

    fun openMenu(windowStack: WindowStack, nameDesc: NameDesc, options: List<MenuOption>): Window? =
        openMenu(windowStack, windowStack.mouseX - paddingX, windowStack.mouseY - paddingY, nameDesc, options)

    fun openMenu(
        windowStack: WindowStack,
        x: Int, y: Int, nameDesc: NameDesc, options: List<MenuOption>, delta: Int = 10
    ): Window? = openMenu(windowStack, x.toFloat(), y.toFloat(), nameDesc, options, delta)

    fun openMenu(
        windowStack: WindowStack,
        x: Float, y: Float, nameDesc: NameDesc, options: List<MenuOption>, delta: Int = 10
    ): Window? = openComplexMenu(
        windowStack, x.roundToIntOr() - delta, y.roundToIntOr() - delta, nameDesc,
        options.map { option -> option.toComplex() })
}