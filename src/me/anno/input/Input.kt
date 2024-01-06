package me.anno.input

import me.anno.Time.nanoTime
import me.anno.config.DefaultConfig
import me.anno.ecs.components.ui.UIEvent
import me.anno.ecs.components.ui.UIEventType
import me.anno.gpu.GFXBase
import me.anno.gpu.OSWindow
import me.anno.input.Touch.Companion.onTouchDown
import me.anno.input.Touch.Companion.onTouchMove
import me.anno.input.Touch.Companion.onTouchUp
import me.anno.io.ISaveable
import me.anno.io.SaveableArray
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileFileRef.Companion.copyHierarchy
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.length
import me.anno.studio.Events.addEvent
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.StudioBase.Companion.instance
import me.anno.ui.Panel
import me.anno.ui.Window
import me.anno.ui.base.menu.Menu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.editor.treeView.TreeViewEntryPanel
import me.anno.utils.Sleep
import me.anno.utils.files.FileChooser
import me.anno.utils.files.Files.findNextFile
import me.anno.utils.structures.maps.BiMap
import me.anno.utils.types.Strings.isArray
import me.anno.utils.types.Strings.isName
import me.anno.utils.types.Strings.isNumber
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWDropCallback
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor.*
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.RenderedImage
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
            if (isShiftTrulyDown) lastShiftDown = nanoTime
            field = value
        }

    var mouseMovementSinceMouseDown = 0f
    var maxClickDistance = 5f
    var hadMouseMovement = false

    val mouseHasMoved get() = mouseMovementSinceMouseDown > maxClickDistance

    var isLeftDown = false
    var isMiddleDown = false
    var isRightDown = false

    val isControlDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_CONTROL)

    // 30ms shift lag for numpad, because shift disables it on Windows
    val isShiftTrulyDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_SHIFT)
    val isShiftDown: Boolean get() = isShiftTrulyDown || (lastShiftDown != 0L && abs(lastShiftDown - nanoTime) < 30_000_000)

    @Suppress("unused")
    val isCapsLockDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_CAPS_LOCK)
    val isAltDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_ALT)
    val isSuperDown: Boolean get() = keyModState.hasFlag(GLFW.GLFW_MOD_SUPER)

    var mouseLockWindow: OSWindow? = null
    var mouseLockPanel: Panel? = null

    val isMouseLocked: Boolean
        get() = mouseLockWindow?.isInFocus == true && mouseLockPanel != null

    fun unlockMouse() {
        mouseLockWindow = null
        mouseLockPanel = null
    }

    var layoutFrameCount = 10
    fun needsLayoutUpdate(window: OSWindow) = window.framesSinceLastInteraction < layoutFrameCount

    fun initForGLFW(window: OSWindow) {

        GLFW.glfwSetDropCallback(window.pointer) { _: Long, count: Int, names: Long ->
            if (count > 0) {
                // it's important to be executed here, because the strings may be GCed otherwise
                val files = (0 until count).mapNotNull { nameIndex ->
                    try {
                        getReference(GLFWDropCallback.getName(names, nameIndex))
                    } catch (e: Exception) {
                        null
                    }
                }
                addEvent {
                    window.framesSinceLastInteraction = 0
                    val dws = window.windowStack
                    val mouseX = window.mouseX
                    val mouseY = window.mouseY
                    dws.requestFocus(dws.getPanelAt(mouseX, mouseY), true)
                    dws.inFocus0?.apply {
                        onPasteFiles(mouseX, mouseY, files)
                    }
                }
            }
        }

        // key typed callback
        GLFW.glfwSetCharModsCallback(window.pointer) { _, codepoint, mods ->
            addEvent { onCharTyped(window, codepoint, mods) }
        }

        GLFW.glfwSetCursorPosCallback(window.pointer) { _, xPosition, yPosition ->
            if (nanoTime > window.lastMouseTeleport) {
                addEvent { onMouseMove(window, xPosition.toFloat(), yPosition.toFloat()) }
            }
        }

        GLFW.glfwSetMouseButtonCallback(window.pointer) { _, button, action, mods ->
            addEvent {
                window.framesSinceLastInteraction = 0
                val button1 = Key.byId(button)
                when (action) {
                    GLFW.GLFW_PRESS -> onMousePress(window, button1)
                    GLFW.GLFW_RELEASE -> onMouseRelease(window, button1)
                }
                keyModState = mods
            }
        }

        GLFW.glfwSetScrollCallback(window.pointer) { _, xOffset, yOffset ->
            addEvent { onMouseWheel(window, xOffset.toFloat(), yOffset.toFloat(), true) }
        }

        GLFW.glfwSetKeyCallback(window.pointer) { window1, key, scancode, action, mods ->
            if (window1 != window.pointer) {
                // touch events are hacked into GLFW for Windows 7+
                window.framesSinceLastInteraction = 0
                // val pressure = max(1, mods)
                val x = scancode * 0.01f
                val y = action * 0.01f
                addEvent {
                    when (mods) {
                        -1 -> onTouchDown(key, x, y)
                        -2 -> onTouchUp(key, x, y)
                        else -> onTouchMove(key, x, y)
                    }
                }
            } else addEvent {
                val key1 = Key.byId(key)
                when (action) {
                    GLFW.GLFW_PRESS -> onKeyPressed(window, key1)
                    GLFW.GLFW_RELEASE -> onKeyReleased(window, key1)
                    GLFW.GLFW_REPEAT -> onKeyTyped(window, key1)
                }
                // LOGGER.info("event $key $scancode $action $mods")
                keyModState = mods
            }
        }
    }

    fun resetFrameSpecificKeyStates() {
        keysWentDown.clear()
        keysWentUp.clear()
    }

    fun onCharTyped(window: OSWindow, codepoint: Int, mods: Int) {
        window.framesSinceLastInteraction = 0
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

    fun onKeyPressed(window: OSWindow, key: Key) {
        window.framesSinceLastInteraction = 0
        keysDown[key] = nanoTime
        keysWentDown += key
        val event = UIEvent(
            window.currentWindow,
            window.mouseX,
            window.mouseY, key,
            UIEventType.KEY_DOWN
        ).call()
        if (!event.isCancelled) {
            window.windowStack.inFocus0
                ?.onKeyDown(window.mouseX, window.mouseY, key)
            ActionManager.onKeyDown(window, key)
            onKeyTyped(window, key)
        }
    }

    fun onKeyReleased(window: OSWindow, key: Key) {
        window.framesSinceLastInteraction = 0
        keyUpCtr++
        keysWentUp += key
        val event = UIEvent(
            window.currentWindow,
            window.mouseX,
            window.mouseY, key,
            UIEventType.KEY_UP
        ).call()
        if (!event.isCancelled) {
            window.windowStack.inFocus0?.onKeyUp(window.mouseX, window.mouseY, key)
            ActionManager.onKeyUp(window, key)
        }
        keysDown.remove(key)
    }

    fun onKeyTyped(window: OSWindow, key: Key) {

        window.framesSinceLastInteraction = 0

        val event = UIEvent(
            window.currentWindow,
            window.mouseX,
            window.mouseY, key,
            UIEventType.KEY_TYPED
        ).call()
        if (event.isCancelled) {
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
                    ) {
                        // switch between input elements
                        val root = inFocus0.rootPanel
                        // todo make groups, which are not empty, inputs?
                        val list = root.listOfAll.filter { it.canBeSeen && it.isKeyInput() }.toList()
                        val index = list.indexOf(inFocus0)
                        if (index > -1) {
                            var next = list
                                .subList(index + 1, list.size)
                                .firstOrNull()
                            if (next == null) {
                                // LOGGER.info("no more text input found, starting from top")
                                // restart from top
                                next = list.firstOrNull()
                            }// else LOGGER.info(next)
                            if (next != null) {
                                inFocus.clear()
                                inFocus += next
                            }
                        }// else error, child missing
                    } else inFocus0.onCharTyped(mouseX, mouseY, '\t'.code)
                }
            }
            Key.KEY_ESCAPE -> {
                if (ws.size > 1) {
                    val window2 = ws.peek()
                    if (window2.canBeClosedByUser) {
                        ws.pop().destroy()
                    } else inFocus0?.onEscapeKey(mouseX, mouseY)
                } else inFocus0?.onEscapeKey(mouseX, mouseY)
            }
            else -> {}
        }
        inFocus0?.onKeyTyped(mouseX, mouseY, key)
    }

    fun onMouseMove(window: OSWindow, newX: Float, newY: Float) {

        if (keysDown.isNotEmpty()) window.framesSinceLastInteraction = 0

        val dx: Float
        val dy: Float
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
        if (length(dx, dy) > 0f) {
            window.framesSinceLastInteraction = 0
        }
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

    val controllers = Array(15) { Controller(it) }
    fun pollControllers(window: OSWindow) {
        // controllers need to be pulled constantly
        synchronized(GFXBase.glfwLock) {
            var isFirst = true
            for (index in controllers.indices) {
                if (controllers[index].pollEvents(window, isFirst)) {
                    isFirst = false
                }
            }
        }
    }

    fun onClickIntoWindow(window: OSWindow, button: Key, panelWindow: Pair<Panel, Window>?) {
        if (panelWindow != null) {
            val ws = window.windowStack
            while (ws.isNotEmpty()) {
                val peek = ws.peek()
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
                        nextSelected.invalidateDrawing()
                    } else {
                        val index0 = prevSelected.indexInParent
                        val index1 = nextSelected.indexInParent
                        // todo we should use the last selected as reference point...
                        val minIndex = min(index0, index1)
                        val maxIndex = max(index0, index1)
                        for (index in minIndex..maxIndex) {
                            val child = commonParent.children[index]
                            child.requestFocus(false)
                            child.invalidateDrawing()
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
                                window.invalidateLayout()
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

        mouseStart = nanoTime
        mouseKeysDown.add(button)
        keysDown[button] = nanoTime
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
        val currentNanos = nanoTime
        val isClick = !mouseHasMoved && !windowWasClosed

        val event = UIEvent(
            window.currentWindow,
            window.mouseX,
            window.mouseY, 0f, 0f,
            button, -1, true,
            false, UIEventType.KEY_UP
        ).call()

        if (!event.isCancelled && isClick) {

            if (maySelectByClick && mouseLockPanel == null) {
                val dws = window.windowStack
                val panel = dws.getPanelAt(mouseX, mouseY)
                dws.requestFocus(panel, true)
            }

            val longClickNanos = 1_000_000 * longClickMillis
            val isDoubleClick = abs(lastClickTime - currentNanos) < longClickNanos &&
                    length(mouseX - lastClickX, mouseY - lastClickY) < maxClickDistance

            val inFocus0 = window.windowStack.inFocus0
            val clickEvent = UIEvent(
                window.currentWindow,
                window.mouseX,
                window.mouseY, 0f, 0f,
                button, -1, true,
                false, UIEventType.MOUSE_CLICK
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

    fun copy(window: OSWindow) {
        val mouseX = window.mouseX
        val mouseY = window.mouseY
        val dws = window.windowStack
        val inFocus = dws.inFocus
        val inFocus0 = dws.inFocus0 ?: return
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
                                isName(s) || isArray(s) || isNumber(s) -> s
                                else -> "\"${
                                    s.replace("\\", "\\\\").replace("\"", "\\\"")
                                }\""
                            }
                        }
                    setClipboardContent(data)
                }
                when (val first = inFocus0.onCopyRequested(mouseX, mouseY)) {
                    is ISaveable -> {
                        // create array
                        val array = SaveableArray()
                        array.add(first)
                        for (panel in inFocus) {
                            if (panel !== inFocus0) {
                                array.add(panel.onCopyRequested(mouseX, mouseY) as? ISaveable ?: continue)
                            }
                        }
                        setClipboardContent(JsonStringWriter.toText(listOf(array), InvalidRef))
                        // todo where necessary, support pasting an array of elements
                    }
                    is FileReference -> {
                        // when this is a list of files, invoke copyFiles instead
                        copyFiles(inFocus.mapNotNull { it.onCopyRequested(mouseX, mouseY) as? FileReference })
                    }
                    is List<*> -> {
                        if (first.isNotEmpty() && first.all { it is FileReference }) {
                            val fullFileList = inFocus.mapNotNull { it as? List<*> }
                                .map { it.filterIsInstance<FileReference>() }
                                .flatten()
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

    fun setClipboardContent(copied: String?) {
        copied ?: return
        val selection = StringSelection(copied)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }

    /**
     * @return null, String or List<FileReference>
     * */
    fun getClipboardContent(): Any? {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        try {
            val data = clipboard.getData(stringFlavor)
            if (data is String) {
                return data
            }
        } catch (_: UnsupportedFlavorException) {
        }
        try {
            val data = clipboard.getData(javaFileListFlavor) as? List<*>
            val data2 = data?.filterIsInstance<File>()
            if (!data2.isNullOrEmpty()) {
                return data2.map { getReference(it) }
            }
        } catch (_: UnsupportedFlavorException) {
        }
        try {
            val data = clipboard.getData(imageFlavor) as RenderedImage
            val folder = instance!!.getPersistentStorage()
            val file0 = folder.getChild("PastedImage.png")
            val file1 = findNextFile(file0, 3, '-', 1)
            file1.outputStream().use { out: OutputStream ->
                if (!ImageIO.write(data, "png", out)) {
                    LOGGER.warn("Couldn't find writer for PNG format")
                }
            }
            LOGGER.info("Pasted image of size ${data.width} x ${data.height}, placed into $file1")
            return listOf(file1)
        } catch (_: UnsupportedFlavorException) {

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun paste(window: OSWindow, panel: Panel? = window.windowStack.inFocus0) {
        if (panel == null) return
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        /*val flavors = clipboard.availableDataFlavors
        for(flavor in flavors){
            val charset = flavor.getParameter("charset")
            val repClass = flavor.representationClass

        }*/
        try {
            val data = clipboard.getData(stringFlavor)
            if (data is String) {
                // LOGGER.info(data)
                panel.onPaste(window.mouseX, window.mouseY, data, "")
                return
            }
        } catch (_: UnsupportedFlavorException) {
        }
        /*try {
            val data = clipboard.getData(getTextPlainUnicodeFlavor())
            LOGGER.info("plain text data: $data")
            if (data is String) inFocus0?.onPaste(mouseX, mouseY, data, "")
            // return
        } catch (e: UnsupportedFlavorException) {
            LOGGER.info("Plain text flavor is not supported")
        }
        try {
            val data = clipboard.getData(javaFileListFlavor)
            LOGGER.info("file data: $data")
            LOGGER.info((data as? List<*>)?.map { it?.javaClass })
            if (data is String) inFocus0?.onPaste(mouseX, mouseY, data, "")
            // return
        } catch (e: UnsupportedFlavorException) {
            LOGGER.info("File List flavor is not supported")
        }*/
        try {
            val data = clipboard.getData(javaFileListFlavor) as? List<*>
            val data2 = data?.filterIsInstance<File>()
            if (!data2.isNullOrEmpty()) {
                // LOGGER.info(data2)
                panel.onPasteFiles(
                    window.mouseX,
                    window.mouseY,
                    data2.map { copiedInternalFiles[it] ?: getReference(it) })
                return
                // return
            }
        } catch (_: UnsupportedFlavorException) {
        }
        try {
            val image = clipboard.getData(imageFlavor) as RenderedImage
            val folder = instance!!.getPersistentStorage()
            val file0 = folder.getChild("PastedImage.png")
            val file1 = findNextFile(file0, 3, '-', 1)
            file1.outputStream().use { out: OutputStream ->
                if (!ImageIO.write(image, "png", out)) {
                    LOGGER.warn("Couldn't find writer for PNG format")
                }
            }
            LOGGER.info("Pasted image of size ${image.width} x ${image.height}, placed into $file1")
            panel.onPasteFiles(window.mouseX, window.mouseY, listOf(file1))
            return
        } catch (_: UnsupportedFlavorException) {

        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        /*try {
            val data = clipboard.getData(DataFlavor.getTextPlainUnicodeFlavor())
            LOGGER.info("plain text data: $data")
        } catch (e: UnsupportedFlavorException) {
        }*/
        LOGGER.warn("Unsupported Data Flavor")
    }

    /**
     * is like calling "control-c" on those files
     * */
    fun copyFiles(files: List<FileReference>) {
        LOGGER.info("Copying $files")
        // we need this folder, when we have temporary copies,
        // because just FileFileRef.createTempFile() changes the name,
        // and we need the original file name
        val tmpFolder = lazy {
            val file = Files.createTempDirectory("tmp").toFile()
            file.deleteOnExit()
            file
        }
        val tmpFiles = files.map {
            if (it is FileFileRef) it.file
            else {
                // create a temporary copy, that the OS understands
                val tmp0 = copiedInternalFiles.reverse[it]
                val ctr = AtomicInteger()
                if (tmp0 != null) tmp0 else {
                    val tmp = File(tmpFolder.value, it.name)
                    copyHierarchy(it, tmp, { ctr.incrementAndGet() }, { ctr.decrementAndGet() })
                    Sleep.waitUntil(true) { ctr.get() == 0 } // wait for all copying to complete
                    copiedInternalFiles[tmp] = it
                    tmp
                }
            }
        }
        copyFiles2(tmpFiles)
    }

    private val copiedInternalFiles = BiMap<File, FileReference>()

    fun copyFiles2(files: List<File>) {
        LOGGER.info("Copying $files")
        Toolkit
            .getDefaultToolkit()
            .systemClipboard
            .setContents(FileTransferable(files), null)
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
}