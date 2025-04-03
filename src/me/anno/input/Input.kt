package me.anno.input

import me.anno.Time
import me.anno.config.DefaultConfig
import me.anno.ecs.components.ui.UIEvent
import me.anno.ecs.components.ui.UIEventType
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.engine.EngineBase.Companion.instance
import me.anno.engine.Events.addEvent
import me.anno.gpu.OSWindow
import me.anno.input.Clipboard.copyFiles
import me.anno.input.Clipboard.getClipboardContent
import me.anno.input.Clipboard.setClipboardContent
import me.anno.input.controller.Controller
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.length
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.treeView.TreeViewEntryPanel
import me.anno.ui.input.InputPanel
import me.anno.utils.files.FileChooser
import me.anno.utils.structures.lists.Lists.mapFirstNotNull
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Strings
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * class for checking key pressed explicitly;
 * handles GLFW events, and passes them onto the event system, and then the UI system (in-focus panel from deepest to root)
 * */
object Input {

    private val LOGGER = LogManager.getLogger(Input::class)

    var keyUpCtr = 0

    var lastFile: FileReference = InvalidRef

    var mouseDownX = 0f
    var mouseDownY = 0f
    val mouseKeysDown = HashSet<Key>()

    // sum of mouse wheel movement
    // for components that have no access to events
    var mouseWheelSumX = 0f
    var mouseWheelSumY = 0f

    val keysDown = HashMap<Key, Long>()
    val keysWentDown = HashSet<Key>()
    val keysWentUp = HashSet<Key>()

    var lastShiftDown = 0L

    var lastClickX = 0f
    var lastClickY = 0f
    var lastClickTime = 0L
    var keyModState = 0
        set(value) {// check for shift...
            if (isShiftTrulyDown) lastShiftDown = Time.nanoTime
            field = value
        }

    var mouseMovementSinceMouseDown = 0f
    var maxClickDistance = 5f
    var minDragDistance = 20f
    var hadMouseMovement = false

    val mouseHasMoved get() = mouseMovementSinceMouseDown > maxClickDistance

    var isLeftDown = false
    var isMiddleDown = false
    var isRightDown = false

    val isControlDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_CONTROL)

    // 30ms shift lag for numpad, because shift disables it on Windows
    val isShiftTrulyDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_SHIFT)
    val isShiftDown: Boolean get() = isShiftTrulyDown || (lastShiftDown != 0L && abs(lastShiftDown - Time.nanoTime) < 30_000_000)

    @Suppress("unused")
    val isCapsLockDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_CAPS_LOCK)
    val isAltDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_ALT)
    val isSuperDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_SUPER)

    var mouseLockWindow: OSWindow? = null
    var mouseLockPanel: Panel? = null

    val isMouseLocked: Boolean
        get() = mouseLockWindow?.isInFocus == true && mouseLockPanel != null

    fun skipCharTyped(codepoint: Int): Boolean {
        return isKeyDown(Key.KEY_LEFT_ALT) &&
                codepoint < 128 && codepoint.toChar().lowercaseChar() in 'a'..'z'
    }

    fun unlockMouse() {
        mouseLockWindow = null
        mouseLockPanel = null
    }

    val shiftSlowdown get() = if (isAltDown) 5f else if (isShiftDown) 0.2f else 1f

    fun resetFrameSpecificKeyStates() {
        keysWentDown.clear()
        keysWentUp.clear()
    }

    fun onCharTyped(window: OSWindow, codepoint: Int, mods: Int) {
        KeyNames.onCharTyped(codepoint)
        val event = UIEvent(
            window.currentWindow,
            window.mouseX, window.mouseY, 0f, 0f,
            Key.KEY_UNKNOWN, codepoint,
            byMouse = false, isLong = false,
            UIEventType.CHAR_TYPED,
        ).call()
        if (!event.isCancelled) {
            window.windowStack.inFocus0
                ?.onCharTyped(window.mouseX, window.mouseY, codepoint)
        }
        keyModState = mods
    }

    private fun callKeyEventIsCancelled(window: OSWindow, key: Key, type: UIEventType): Boolean {
        val event = UIEvent(
            window.currentWindow,
            window.mouseX, window.mouseY,
            key, type
        ).call()
        return event.isCancelled
    }

    fun onKeyPressed(window: OSWindow, key: Key, nanoTime: Long) {
        keysDown[key] = nanoTime
        keysWentDown += key
        if (!callKeyEventIsCancelled(window, key, UIEventType.KEY_DOWN)) {
            window.windowStack.inFocus0
                ?.onKeyDown(window.mouseX, window.mouseY, key)
            ActionManager.onKeyDown(window, key)
            onKeyTyped(window, key)
        }
    }

    fun onKeyReleased(window: OSWindow, key: Key) {
        keyUpCtr++
        keysWentUp += key
        if (!callKeyEventIsCancelled(window, key, UIEventType.KEY_UP)) {
            window.windowStack.inFocus0?.onKeyUp(window.mouseX, window.mouseY, key)
            ActionManager.onKeyUp(window, key)
        }
        keysDown.remove(key)
    }

    fun onKeyTyped(window: OSWindow, key: Key) {

        if (callKeyEventIsCancelled(window, key, UIEventType.KEY_TYPED)) {
            return
        }

        ActionManager.onKeyTyped(window, key)

        // just related to the top window-stack
        val ws = window.windowStack
        val inFocus = ws.inFocus
        val inFocus0 = ws.inFocus0
        val mouseX = window.mouseX
        val mouseY = window.mouseY

        when (key) {
            Key.KEY_ENTER, Key.KEY_KP_ENTER -> {
                if (inFocus0 != null) {
                    if (isShiftDown || isControlDown) {
                        inFocus0.onCharTyped(mouseX, mouseY, '\n'.code)
                    } else {
                        inFocus0.onEnterKey(mouseX, mouseY)
                    }
                }
            }
            Key.KEY_DELETE -> {
                // tree view selections need to be removed, because they would be illogical to keep
                // (because the underlying Transform changes)
                val inFocusTreeViews = inFocus.filterIsInstance<TreeViewEntryPanel<*>>()
                for (it in inFocus) it.onDeleteKey(mouseX, mouseY)
                inFocus.removeAll(inFocusTreeViews.toSet())
            }
            Key.KEY_BACKSPACE -> inFocus0?.onBackSpaceKey(mouseX, mouseY)
            Key.KEY_TAB -> {
                if (inFocus0 != null) {
                    if (isShiftDown || isControlDown
                        || !inFocus0.isKeyInput()
                        || !inFocus0.acceptsChar('\t'.code)
                    ) tabToNextInput(inFocus0, isAltDown)
                    else inFocus0.onCharTyped(mouseX, mouseY, '\t'.code)
                }
            }
            Key.KEY_ESCAPE -> {
                if (ws.size > 1) {
                    val window2 = ws.last()
                    if (window2.canBeClosedByUser) {
                        ws.pop().destroy()
                    } else inFocus0?.onEscapeKey(mouseX, mouseY)
                } else inFocus0?.onEscapeKey(mouseX, mouseY)
            }
            else -> {}
        }
        inFocus0?.onKeyTyped(mouseX, mouseY, key)
    }

    private fun isTabNavigationInput(it: Panel): Boolean {
        return it is InputPanel<*> && it.isEnabled &&
                it.isKeyInput() && !it.acceptsChar('\t'.code)
    }

    fun tabToNextInput(inFocus0: Panel, backwards: Boolean) {

        // switch between input elements
        val root = inFocus0.rootPanel
        val inputPanels = root.listOfVisible.filter(::isTabNavigationInput)

        val index = inFocus0.listOfHierarchy.mapFirstNotNull {
            val idx = inputPanels.indexOf(it)
            if (idx >= 0) idx else null
        }

        var next = inputPanels.firstOrNull()
        if (index != null) {
            val delta = if (backwards) -1 else +1 // alt+tab goes backwards :3
            next = inputPanels.getOrNull(index + delta)
            if (next == null) {
                // restart from other end
                next = if (backwards) inputPanels.lastOrNull() else inputPanels.firstOrNull()
            }
            if (next != null) {
                next.scrollTo() // in case it would be off-screen
                next.requestFocus()
            }
        }
        if (next != null) {
            next.scrollTo() // in case it would be off-screen
            next.requestFocus()
        }
    }

    fun onMouseMove(window: OSWindow, newX: Float, newY: Float) {

        var dx: Float
        var dy: Float
        synchronized(window) {

            dx = newX - window.mouseX
            dy = newY - window.mouseY
            val length = length(dx, dy)
            mouseMovementSinceMouseDown += length
            if (length > 0f) hadMouseMovement = true

            window.mouseX = newX
            window.mouseY = newY
        }

        val event = UIEvent(
            window.currentWindow,
            window.mouseX, window.mouseY, dx, dy,
            Key.KEY_UNKNOWN, -1,
            byMouse = true, isLong = false,
            UIEventType.MOUSE_MOVE
        ).call()

        if (!event.isCancelled) {
            for (panel in window.windowStack.inFocus) {
                panel.onMouseMoved(newX, newY, dx, dy)
            }
            ActionManager.onMouseMoved(window, dx, dy)
        }
    }

    var userCanScrollX = false
    fun onMouseWheel(window: OSWindow, dx: Float, dy: Float, byMouse: Boolean) {
        mouseWheelSumX += dx
        mouseWheelSumY += dy
        addEvent {
            val mouseX = window.mouseX
            val mouseY = window.mouseY
            val panelUnderCursor = window.windowStack.getPanelAt(mouseX, mouseY)
            if (!byMouse && abs(dx) > abs(dy)) userCanScrollX = true // e.g., by touchpad: use can scroll x
            if (panelUnderCursor != null) {
                if (!userCanScrollX && byMouse && (isShiftDown || isControlDown)) {
                    panelUnderCursor.onMouseWheel(mouseX, mouseY, -dy, dx, byMouse = true)
                } else {
                    panelUnderCursor.onMouseWheel(mouseX, mouseY, dx, dy, byMouse)
                }
            }
        }
    }

    val controllers = ArrayList<Controller>()

    fun onClickIntoWindow(window: OSWindow, button: Key, panelWindow: Pair<Panel, Window>?) {
        if (panelWindow != null) {
            val ws = window.windowStack
            while (true) {
                val peek = ws.peek() ?: break
                if (panelWindow.second == peek || !peek.acceptsClickAway(button)) break
                ws.pop().destroy()
                windowWasClosed = true
            }
        }
    }

    var mouseStart = 0L
    var windowWasClosed = false
    var maySelectByClick = false

    fun onMousePress(window: OSWindow, button: Key) {

        val mouseX = window.mouseX
        val mouseY = window.mouseY
        // find the clicked element
        mouseDownX = mouseX
        mouseDownY = mouseY
        mouseMovementSinceMouseDown = 0f
        keysWentDown += button

        when (button) {
            Key.BUTTON_LEFT -> isLeftDown = true
            Key.BUTTON_RIGHT -> isRightDown = true
            Key.BUTTON_MIDDLE -> isMiddleDown = true
            else -> {}
        }

        windowWasClosed = false
        val windowStack = window.windowStack
        val panelWindow = windowStack.getPanelAndWindowAt(mouseX, mouseY)
        onClickIntoWindow(window, button, panelWindow)
        if (windowWasClosed) return

        // todo the selection order for multiselect not always makes sense (e.g. not for graph panels) ->
        //  - sort the list or
        //  - disable multiselect

        val event = UIEvent(
            window.currentWindow,
            mouseX, mouseY, 0f, 0f,
            button, -1,
            byMouse = true, isLong = false,
            UIEventType.KEY_DOWN
        ).call()

        if (!event.isCancelled) {

            val toggleSinglePanel = isControlDown
            val selectPanelRange = isShiftDown
            val inFocus0 = windowStack.inFocus0

            var issuedMouseDown = false
            val mouseTarget =
                if (window == mouseLockWindow && mouseLockPanel != null) mouseLockPanel
                else windowStack.getPanelAt(mouseX, mouseY)
            maySelectByClick = if (toggleSinglePanel || selectPanelRange) {
                val nextSelected = mouseTarget?.getMultiSelectablePanel()
                val prevSelected = inFocus0?.getMultiSelectablePanel()
                val commonParent = prevSelected?.uiParent
                if (nextSelected != null && commonParent != null && commonParent === nextSelected.uiParent) {
                    issuedMouseDown = true
                    if (inFocus0 !== prevSelected) windowStack.requestFocus(prevSelected, true)
                    if (toggleSinglePanel) {
                        if (nextSelected.isInFocus) {
                            windowStack.inFocus -= nextSelected
                        } else {
                            nextSelected.requestFocus(false)
                        }
                    } else {
                        val index0 = prevSelected.indexInParent
                        val index1 = nextSelected.indexInParent
                        // todo we should use the last selected as reference point...
                        val minIndex = min(index0, index1)
                        val maxIndex = max(index0, index1)
                        for (index in minIndex..maxIndex) {
                            commonParent.children[index].requestFocus(false)
                        }
                    }
                    false
                } else {
                    if (mouseTarget != null && mouseTarget.isInFocus) {
                        true
                    } else {
                        windowStack.requestFocus(mouseTarget, true)
                        false
                    }
                }
            } else {
                if (mouseTarget != null && mouseTarget.isInFocus) {
                    true
                } else {
                    windowStack.requestFocus(mouseTarget, true)
                    false
                }
            }

            val mouseYi = mouseY.toInt()
            if (button == Key.BUTTON_RIGHT && mouseYi in 0 until window.progressbarHeightSum) {
                val idx = mouseYi / window.progressbarHeight
                val progressBar = window.progressBars.getOrNull(idx)
                if (progressBar != null) {
                    val notifyWhenFinishedTitle = NameDesc(
                        if (progressBar.notifyWhenFinished) "Don't notify when finished"
                        else "Notify when finished"
                    )
                    Menu.openMenu(
                        windowStack,
                        NameDesc(progressBar.name),
                        listOf(
                            MenuOption(notifyWhenFinishedTitle) {
                                progressBar.notifyWhenFinished = !progressBar.notifyWhenFinished
                            },
                            MenuOption(NameDesc("Hide")) {
                                window.progressBars.remove(progressBar)
                            },
                            MenuOption(NameDesc("Cancel")) {
                                progressBar.cancel(false)
                                // the user interacted, and knows what he was doing,
                                // so close much quicker than usual
                                progressBar.endShowDuration = 300 * MILLIS_TO_NANOS
                            }
                        )
                    )
                    issuedMouseDown = true
                }
            }

            for (panel in windowStack.inFocus) {
                panel.onKeyDown(mouseX, mouseY, button)
                issuedMouseDown = true
            }

            if (!issuedMouseDown) {
                val inFocus = window.currentWindow?.panel?.getPanelAt(mouseX.toInt(), mouseY.toInt())
                inFocus?.requestFocus()
                inFocus?.onKeyDown(mouseX, mouseY, button)
            }

            ActionManager.onKeyDown(window, button)
        }

        mouseStart = Time.nanoTime
        mouseKeysDown.add(button)
        keysDown[button] = Time.nanoTime
    }

    fun onMouseRelease(window: OSWindow, button: Key) {

        keyUpCtr++
        keysWentUp += button

        when (button) {
            Key.BUTTON_LEFT -> isLeftDown = false
            Key.BUTTON_RIGHT -> isRightDown = false
            Key.BUTTON_MIDDLE -> isMiddleDown = false
            else -> {}
        }

        val mouseX = window.mouseX
        val mouseY = window.mouseY
        val inFocus = window.windowStack.inFocus
        for (i in inFocus.indices) {
            inFocus.getOrNull(i)?.onKeyUp(mouseX, mouseY, button) ?: break
        }

        ActionManager.onKeyUp(window, button)
        ActionManager.onKeyTyped(window, button)

        val longClickMillis = DefaultConfig["longClick", 300]
        val currentNanos = Time.nanoTime
        val isClick = !mouseHasMoved && !windowWasClosed

        val event = UIEvent(
            window.currentWindow,
            window.mouseX, window.mouseY, 0f, 0f,
            button, -1, byMouse = true,
            isLong = false, UIEventType.KEY_UP
        ).call()

        if (!event.isCancelled && isClick) {

            if (maySelectByClick && mouseLockPanel == null) {
                val ws = window.windowStack
                val panel = ws.getPanelAt(mouseX, mouseY)
                ws.requestFocus(panel, true)
            }

            val longClickNanos = 1_000_000 * longClickMillis
            val isDoubleClick = abs(lastClickTime - currentNanos) < longClickNanos &&
                    length(mouseX - lastClickX, mouseY - lastClickY) < maxClickDistance

            val inFocus0 = window.windowStack.inFocus0
            val clickEvent = UIEvent(
                window.currentWindow,
                window.mouseX,
                window.mouseY, 0f, 0f,
                button, -1, byMouse = true,
                isLong = false, UIEventType.MOUSE_CLICK
            ).call()

            if (!clickEvent.isCancelled) {
                if (isDoubleClick) {
                    ActionManager.onKeyDoubleClick(window, button)
                    inFocus0?.onDoubleClick(mouseX, mouseY, button)
                } else {
                    val mouseDuration = currentNanos - mouseStart
                    val isLongClick = mouseDuration / 1_000_000 >= longClickMillis
                    inFocus0?.onMouseClicked(mouseX, mouseY, button, isLongClick)
                }
            }

            lastClickX = mouseX
            lastClickY = mouseY
            lastClickTime = currentNanos
        }

        mouseKeysDown.remove(button)
        keysDown.remove(button)

        // normally, this should be consumed
        // but there might be situations, where it isn't, so
        // then set it to null just in case
        dragged = null
    }

    fun copy(window: OSWindow) {
        val mouseX = window.mouseX
        val mouseY = window.mouseY
        val ws = window.windowStack
        val inFocus = ws.inFocus
        val inFocus0 = ws.inFocus0 ?: return
        when (inFocus.size) {
            0 -> return // should not happen
            1 -> {
                val value = inFocus0.onCopyRequested(mouseX, mouseY)
                if (value is List<*> && value.isNotEmpty() && value.all { it is FileReference }) {
                    copyFiles(value.filterIsInstance<FileReference>())
                } else setClipboardContent(value?.toString())
            }
            else -> {
                val parentValue = inFocus0.getMultiSelectablePanel()?.onCopyRequested(mouseX, mouseY)
                if (parentValue != null) return setClipboardContent(parentValue.toString())
                // combine them into an array
                fun defaultCopy() {
                    // create very simple, stupid array of values as strings
                    val data = inFocus
                        .mapNotNull { it.onCopyRequested(mouseX, mouseY) }
                        .joinToString(",", "[", "]") {
                            val s = it.toString()
                            when {
                                s.isEmpty() -> "\"\""
                                Strings.isName(s) || Strings.isArray(s) || Strings.isNumber(s) -> s
                                else -> "\"${
                                    s.replace("\\", "\\\\").replace("\"", "\\\"")
                                }\""
                            }
                        }
                    setClipboardContent(data)
                }
                when (val first = inFocus0.onCopyRequested(mouseX, mouseY)) {
                    is Saveable -> {
                        val saveables = ArrayList<Saveable>()
                        saveables.add(first)
                        for (panel in inFocus) {
                            if (panel !== inFocus0) {
                                val ith = panel.onCopyRequested(mouseX, mouseY) as? Saveable ?: continue
                                saveables.add(ith)
                            }
                        }
                        setClipboardContent(JsonStringWriter.toText(saveables, InvalidRef))
                        // todo where necessary, support pasting an array of elements
                    }
                    is FileReference -> {
                        // when this is a list of files, invoke copyFiles instead
                        copyFiles(inFocus.mapNotNull { it.onCopyRequested(mouseX, mouseY) as? FileReference })
                    }
                    is List<*> -> {
                        if (first.isNotEmpty() && first.all { it is FileReference }) {
                            val fullFileList = inFocus.mapNotNull { it as? List<*> }
                                .flatMap { it.filterIsInstance<FileReference>() }
                            copyFiles(fullFileList)
                        } else defaultCopy()
                    }
                    else -> defaultCopy()
                }
            }
        }
    }

    fun copy(window: OSWindow, panel: Panel) {
        val mouseX = window.mouseX
        val mouseY = window.mouseY
        setClipboardContent(panel.onCopyRequested(mouseX, mouseY)?.toString())
    }

    fun paste(window: OSWindow, panel: Panel? = window.windowStack.inFocus0) {
        if (panel == null) return
        val data = getClipboardContent() ?: return
        when {
            data is String -> panel.onPaste(window.mouseX, window.mouseY, data, "")
            data is List<*> && data.firstOrNull() is FileReference -> {
                panel.onPasteFiles(window.mouseX, window.mouseY, data.filterIsInstance<FileReference>())
            }
            else -> LOGGER.warn("Unsupported paste-type: ${data::class}")
        }
    }

    fun empty(window: OSWindow) {
        // LOGGER.info("[Input] emptying, $inFocus0, ${inFocus0?.javaClass}")
        window.windowStack.inFocus0?.onEmpty(window.mouseX, window.mouseY)
    }

    fun import() {
        if (lastFile == InvalidRef) lastFile = instance!!.getDefaultFileLocation()
        FileChooser.selectFiles(
            NameDesc("Import Files"), allowFiles = true,
            allowFolders = false, allowMultiples = false, startFolder = lastFile,
            toSave = false, filters = emptyList()
        ) { files ->
            val fileRef = files.firstOrNull()
            if (fileRef != null) {
                lastFile = fileRef
                instance?.importFile(fileRef)
            }
        }
    }

    fun isKeyDown(key: Key): Boolean {
        return key in keysDown
    }

    fun isKeyDown(key: String): Boolean {
        val keyCode = KeyCombination.keyMapping[key] ?: return false
        return isKeyDown(keyCode)
    }

    fun isKeyDown(key: Char): Boolean {
        val key1 = Key.byId(key.uppercaseChar().code)
        if (key1 == Key.KEY_UNKNOWN) return false
        return isKeyDown(key1)
    }

    fun wasKeyPressed(key: Key): Boolean {
        return key in keysWentDown
    }

    fun wasKeyPressed(key: Char): Boolean {
        val key1 = Key.byId(key.uppercaseChar().code)
        if (key1 == Key.KEY_UNKNOWN) return false
        return wasKeyPressed(key1)
    }

    @Suppress("unused")
    fun wasKeyPressed(key: String): Boolean {
        val keyCode = KeyCombination.keyMapping[key] ?: return false
        return wasKeyPressed(keyCode)
    }

    fun wasKeyReleased(key: Key): Boolean {
        return key in keysWentUp
    }

    @Suppress("unused")
    fun wasKeyReleased(key: Char): Boolean {
        val key1 = Key.byId(key.uppercaseChar().code)
        if (key1 == Key.KEY_UNKNOWN) return false
        return wasKeyReleased(key1)
    }

    @Suppress("unused")
    fun wasKeyReleased(key: String): Boolean {
        val keyCode = KeyCombination.keyMapping[key] ?: return false
        return wasKeyReleased(keyCode)
    }

    fun getDownTimeNanos(key: Key): Long {
        val downTimeNanos = keysDown[key] ?: return -1L
        return Time.nanoTime - downTimeNanos
    }
}