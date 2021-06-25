package me.anno.input

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.GFX.getPanelAndWindowAt
import me.anno.gpu.GFX.getPanelAt
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.GFX.inFocus0
import me.anno.gpu.GFX.requestFocus
import me.anno.gpu.GFX.window
import me.anno.gpu.GFX.windowStack
import me.anno.gpu.Window
import me.anno.input.Input.toMouseButton
import me.anno.input.Touch.Companion.onTouchDown
import me.anno.input.Touch.Companion.onTouchMove
import me.anno.input.Touch.Companion.onTouchUp
import me.anno.io.FileReference
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.studio.rems.RemsStudio.history
import me.anno.studio.rems.RemsStudio.project
import me.anno.studio.rems.RemsStudio.root
import me.anno.ui.base.Panel
import me.anno.ui.editor.files.ImportFromFile.addChildFromFile
import me.anno.ui.editor.treeView.TreeViewPanel
import me.anno.utils.Maths.length
import me.anno.utils.Threads.threadWithName
import me.anno.utils.files.FileExplorerSelectWrapper
import org.apache.logging.log4j.LogManager
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWDropCallback
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor.javaFileListFlavor
import java.awt.datatransfer.DataFlavor.stringFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Input {

    private val LOGGER = LogManager.getLogger(Input::class)

    var keyUpCtr = 0

    var mouseX = 0f
    var mouseY = 0f

    var lastFile: FileReference? = null

    var mouseDownX = 0f
    var mouseDownY = 0f
    var mouseKeysDown = HashSet<Int>()

    val keysDown = HashMap<Int, Long>()

    var lastShiftDown = 0L

    var lastClickX = 0f
    var lastClickY = 0f
    var lastClickTime = 0L
    var keyModState = 0
        set(value) {// check for shift...
            if (isShiftTrulyDown) lastShiftDown = gameTime
            field = value
        }

    var mouseMovementSinceMouseDown = 0f
    val maxClickDistance = 5f

    val isControlDown get() = (keyModState and GLFW.GLFW_MOD_CONTROL) != 0

    // 30ms shift lag for numpad, because shift disables it on Windows
    val isShiftTrulyDown get() = (keyModState and GLFW.GLFW_MOD_SHIFT) != 0
    val isShiftDown get() = isShiftTrulyDown || abs(lastShiftDown - gameTime) < 30_000_000

    val isCapsLockDown get() = (keyModState and GLFW.GLFW_MOD_CAPS_LOCK) != 0
    val isAltDown get() = (keyModState and GLFW.GLFW_MOD_ALT) != 0
    val isSuperDown get() = (keyModState and GLFW.GLFW_MOD_SUPER) != 0

    var framesSinceLastInteraction = 0
    val layoutFrameCount = 10

    fun needsLayoutUpdate() = framesSinceLastInteraction < layoutFrameCount

    fun invalidateLayout() {
        framesSinceLastInteraction = 0
    }

    fun Int.toMouseButton() = when (this) {
        0 -> MouseButton.LEFT
        1 -> MouseButton.RIGHT
        2 -> MouseButton.MIDDLE
        else -> MouseButton.UNKNOWN
    }

    fun initForGLFW() {

        GLFW.glfwSetDropCallback(window) { _: Long, count: Int, names: Long ->
            if (count > 0) {
                // it's important to be executed here, because the strings may be GCed otherwise
                val files = Array(count) { nameIndex ->
                    try {
                        File(GLFWDropCallback.getName(names, nameIndex))
                    } catch (e: Exception) {
                        null
                    }
                }
                addEvent {
                    framesSinceLastInteraction = 0
                    requestFocus(getPanelAt(mouseX, mouseY), true)
                    inFocus0?.apply {
                        onPasteFiles(mouseX, mouseY, files.filterNotNull().map { FileReference(it) })
                    }
                }
            }
        }

        /*GLFW.glfwSetCharCallback(window) { _, _ ->
            addEvent {
                // LOGGER.info("char event $codepoint")
            }
        } */

        // key typed callback
        GLFW.glfwSetCharModsCallback(window) { _, codepoint, mods ->
            addEvent {
                framesSinceLastInteraction = 0
                inFocus0?.onCharTyped(mouseX, mouseY, codepoint)
                keyModState = mods
            }
        }

        GLFW.glfwSetCursorPosCallback(window) { _, xPosition, yPosition ->
            addEvent {

                if (keysDown.isNotEmpty()) framesSinceLastInteraction = 0

                val newX = xPosition.toFloat()
                val newY = yPosition.toFloat()

                val dx = newX - mouseX
                val dy = newY - mouseY

                mouseMovementSinceMouseDown += length(dx, dy)

                mouseX = newX
                mouseY = newY

                inFocus0?.onMouseMoved(mouseX, mouseY, dx, dy)
                ActionManager.onMouseMoved(dx, dy)

            }
        }

        GLFW.glfwSetMouseButtonCallback(window) { _, button, action, mods ->
            addEvent {
                framesSinceLastInteraction = 0
                when (action) {
                    GLFW.GLFW_PRESS -> onMousePress(button)
                    GLFW.GLFW_RELEASE -> onMouseRelease(button)
                }
                keyModState = mods
            }
        }

        GLFW.glfwSetScrollCallback(window) { _, xOffset, yOffset ->
            addEvent {
                framesSinceLastInteraction = 0
                val clicked = getPanelAt(mouseX, mouseY)
                clicked?.onMouseWheel(mouseX, mouseY, xOffset.toFloat(), yOffset.toFloat())
            }
        }

        GLFW.glfwSetKeyCallback(window) { window, key, scancode, action, mods ->
            if (window != GFX.window) {
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
            } else
                addEvent {
                    framesSinceLastInteraction = 0
                    fun keyTyped(key: Int) {

                        ActionManager.onKeyTyped(key)

                        when (key) {
                            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                                if (isShiftDown || isControlDown) {
                                    inFocus0?.onCharTyped(mouseX, mouseY, '\n'.code)
                                } else {
                                    inFocus0?.onEnterKey(mouseX, mouseY)
                                }
                            }
                            GLFW.GLFW_KEY_DELETE -> {
                                // tree view selections need to be removed, because they would be illogical to keep
                                // (because the underlying Transform changes)
                                val inFocusTreeViews = inFocus.filterIsInstance<TreeViewPanel>()
                                inFocus.forEach { it.onDeleteKey(mouseX, mouseY) }
                                inFocus.removeAll(inFocusTreeViews)
                            }
                            GLFW.GLFW_KEY_BACKSPACE -> {
                                inFocus0?.onBackSpaceKey(mouseX, mouseY)
                            }
                            GLFW.GLFW_KEY_TAB -> {
                                val inFocus0 = inFocus0
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
                                                // println("no more text input found, starting from top")
                                                // restart from top
                                                next = list.firstOrNull()
                                            }// else println(next)
                                            if (next != null) {
                                                inFocus.clear()
                                                inFocus += next
                                            }
                                        }// else error, child missing
                                    } else inFocus0.onCharTyped(mouseX, mouseY, '\t'.code)
                                }
                            }
                            GLFW.GLFW_KEY_ESCAPE -> {
                                if (windowStack.size > 1) {
                                    val window2 = windowStack.peek()
                                    if (window2.canBeClosedByUser) {
                                        windowStack.pop().destroy()
                                    } else inFocus0?.onEscapeKey(mouseX, mouseY)
                                } else inFocus0?.onEscapeKey(mouseX, mouseY)
                            }
                            else -> {
                                if (isControlDown) {
                                    if (action == GLFW.GLFW_PRESS) {
                                        when (key) {
                                            GLFW.GLFW_KEY_S -> save()
                                            GLFW.GLFW_KEY_V -> paste()
                                            GLFW.GLFW_KEY_C -> copy()
                                            GLFW.GLFW_KEY_D -> {// duplicate selection
                                                copy()
                                                paste()
                                            }
                                            GLFW.GLFW_KEY_X -> {// cut
                                                copy()
                                                empty()
                                            }
                                            GLFW.GLFW_KEY_I -> {// import
                                                threadWithName("Ctrl+I") {
                                                    if (lastFile == null) lastFile = project?.file
                                                    FileExplorerSelectWrapper.selectFile(lastFile?.file) { file ->
                                                        if (file != null) {
                                                            lastFile = FileReference(file)
                                                            addEvent { addChildFromFile(root, file, null, true) {} }
                                                        }
                                                    }
                                                }
                                            }
                                            GLFW.GLFW_KEY_H -> history?.display()
                                            GLFW.GLFW_KEY_A -> inFocus0?.onSelectAll(mouseX, mouseY)
                                        }
                                    }
                                }
                                // LOGGER.info("typed by $action")
                                inFocus0?.onKeyTyped(mouseX, mouseY, key)
                                // inFocus?.onCharTyped(mx,my,key)
                            }
                        }
                    }
                    when (action) {
                        GLFW.GLFW_PRESS -> {
                            keysDown[key] = gameTime
                            inFocus0?.onKeyDown(mouseX, mouseY, key) // 264
                            ActionManager.onKeyDown(key)
                            keyTyped(key)
                        }
                        GLFW.GLFW_RELEASE -> {
                            keyUpCtr++
                            inFocus0?.onKeyUp(mouseX, mouseY, key)
                            ActionManager.onKeyUp(key)
                            keysDown.remove(key)
                        } // 265
                        GLFW.GLFW_REPEAT -> keyTyped(key)
                    }
                    // LOGGER.info("event $key $scancode $action $mods")
                    keyModState = mods
                }
        }

    }

    fun onClickIntoWindow(button: Int, panelWindow: Pair<Panel, Window>?){
        if (panelWindow != null) {
            val mouseButton = button.toMouseButton()
            while (windowStack.isNotEmpty()) {
                val peek = windowStack.peek()
                if (panelWindow.second == peek || !peek.acceptsClickAway(mouseButton)) break
                windowStack.pop().destroy()
                windowWasClosed = true
            }
        }
    }

    var mouseStart = 0L
    var windowWasClosed = false
    var maySelectByClick = false

    fun onMousePress(button: Int){

        // find the clicked element
        mouseDownX = mouseX
        mouseDownY = mouseY
        mouseMovementSinceMouseDown = 0f

        windowWasClosed = false
        val panelWindow = getPanelAndWindowAt(mouseX, mouseY)
        onClickIntoWindow(button, panelWindow)

        val singleSelect = isControlDown
        val multiSelect = isShiftDown

        val mouseTarget = getPanelAt(mouseX, mouseY)
        val selectionTarget = mouseTarget?.getMultiSelectablePanel()
        val inFocusTarget = inFocus0?.getMultiSelectablePanel()
        val joinedParent = inFocusTarget?.parent

        maySelectByClick = if ((singleSelect || multiSelect) && selectionTarget != null && joinedParent == selectionTarget.parent) {
            if (inFocus0 != inFocusTarget) requestFocus(inFocusTarget, true)
            if (singleSelect) {
                if (selectionTarget in inFocus) inFocus -= selectionTarget
                else inFocus += selectionTarget
            } else {
                val index0 = inFocusTarget!!.indexInParent
                val index1 = selectionTarget.indexInParent
                // todo we should use the last selected as reference point...
                val minIndex = min(index0, index1)
                val maxIndex = max(index0, index1)
                for (index in minIndex..maxIndex) {
                    inFocus += joinedParent!!.children[index]
                }
            }
            // LOGGER.info(inFocus)
            false
        } else {
            if(mouseTarget in inFocus){
                true
            } else {
                requestFocus(mouseTarget, true)
                false
            }
        }

        if (!windowWasClosed) {

            inFocus0?.onMouseDown(mouseX, mouseY, button.toMouseButton())
            ActionManager.onKeyDown(button)
            mouseStart = System.nanoTime()
            mouseKeysDown.add(button)
            keysDown[button] = gameTime

        }

    }

    fun onMouseRelease(button: Int){

        keyUpCtr++

        inFocus0?.onMouseUp(mouseX, mouseY, button.toMouseButton())

        ActionManager.onKeyUp(button)
        ActionManager.onKeyTyped(button)

        val longClickMillis = DefaultConfig["longClick", 300]
        val currentNanos = System.nanoTime()
        val isClick = mouseMovementSinceMouseDown < maxClickDistance && !windowWasClosed

        if (isClick) {

            if(maySelectByClick){
                val panelWindow = getPanelAndWindowAt(mouseX, mouseY)
                requestFocus(panelWindow?.first, true)
            }

            val isDoubleClick = abs(lastClickTime - currentNanos) / 1_000_000 < longClickMillis &&
                    length(mouseX - lastClickX, mouseY - lastClickY) < maxClickDistance

            if (isDoubleClick) {

                ActionManager.onKeyDoubleClick(button)
                inFocus0?.onDoubleClick(mouseX, mouseY, button.toMouseButton())

            } else {

                val mouseDuration = currentNanos - mouseStart
                val isLongClick = mouseDuration / 1_000_000 >= longClickMillis
                inFocus0?.onMouseClicked(mouseX, mouseY, button.toMouseButton(), isLongClick)

            }

            lastClickX = mouseX
            lastClickY = mouseY
            lastClickTime = currentNanos

        }

        mouseKeysDown.remove(button)
        keysDown.remove(button)

    }

    fun empty() {
        inFocus0?.onEmpty(mouseX, mouseY)
    }

    fun copy() {
        // todo combine all selected values into an array?
        val copied = inFocus0?.onCopyRequested(mouseX, mouseY)
        if (copied != null) {
            val selection = StringSelection(copied)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        }
    }

    fun copy(panel: Panel) {
        val copied = panel.onCopyRequested(mouseX, mouseY)
        if (copied != null) {
            val selection = StringSelection(copied)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        }
    }

    fun paste(panel: Panel? = inFocus0) {
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
                // println(data)
                panel.onPaste(mouseX, mouseY, data, "")
                return
            }
        } catch (e: UnsupportedFlavorException) {
        }
        /*try {
            val data = clipboard.getData(getTextPlainUnicodeFlavor())
            println("plain text data: $data")
            if (data is String) inFocus0?.onPaste(mouseX, mouseY, data, "")
            // return
        } catch (e: UnsupportedFlavorException) {
            println("Plain text flavor is not supported")
        }
        try {
            val data = clipboard.getData(javaFileListFlavor)
            println("file data: $data")
            println((data as? List<*>)?.map { it?.javaClass })
            if (data is String) inFocus0?.onPaste(mouseX, mouseY, data, "")
            // return
        } catch (e: UnsupportedFlavorException) {
            println("File List flavor is not supported")
        }*/
        try {
            val data = clipboard.getData(javaFileListFlavor) as? List<*>
            val data2 = data?.filterIsInstance<File>()
            if (data2 != null && data2.isNotEmpty()) {
                // println(data2)
                panel.onPasteFiles(mouseX, mouseY, data2.map { FileReference(it) })
                // return
            }
        } catch (e: UnsupportedFlavorException) {
        }
        /*try {
            val data = clipboard.getData(DataFlavor.getTextPlainUnicodeFlavor())
            println("plain text data: $data")
        } catch (e: UnsupportedFlavorException) {
        }*/
        LOGGER.warn("Unsupported Data Flavor")
    }

    fun copyFiles(files: List<File>) {
        Toolkit
            .getDefaultToolkit()
            .systemClipboard
            .setContents(FileTransferable(files), null)
    }

    fun save() {
        project?.save()
    }

    fun isKeyDown(key: Char): Boolean {
        return key.uppercaseChar().code in keysDown
    }

}