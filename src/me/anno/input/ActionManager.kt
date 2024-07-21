package me.anno.input

import me.anno.Time
import me.anno.config.ConfigRef
import me.anno.gpu.OSWindow
import me.anno.io.config.ConfigBasics.loadConfig
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.ui.Panel
import me.anno.ui.input.components.TitlePanel
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object ActionManager : StringMap() {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(ActionManager::class)

    @JvmStatic
    var keyDragDelay by ConfigRef("ui.keyDragDelay", 0.5f)

    @JvmStatic
    var enableQuickDragging by ConfigRef("ui.mouse.enableQuickDragging", true)

    @JvmStatic
    private val localActions = KeyPairMap<String, KeyCombination, List<String>>(512)

    @JvmStatic
    private val globalKeyCombinations = HashMap<KeyCombination, List<String>>()

    @JvmStatic
    private val globalActions = HashMap<String, () -> Boolean>()

    @JvmStatic
    fun init() {
        try {
            putAll(loadConfig("keymap.config", InvalidRef, this, true))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // this should be fine
        // if an action is supposed to do nothing, then it should be set to ""
        createDefaultKeymap(this)
        parseConfig()
    }

    @JvmField
    var createDefaultKeymap: (StringMap) -> Unit = {
        LOGGER.warn("Using default keymap... this should not happen!")
    }

    @JvmStatic
    fun parseConfig() {
        for ((key, value) in this) {
            value as? String ?: continue
            register(key, value)
        }
    }

    /**
     * @param direction 0 = override, -1 = prepend, +1 = append
     * */
    @JvmStatic
    fun register(key: String, value: String, direction: Int = 0) {
        if (value == "") return
        val keys = key.split('.')
        val namespace = keys[0]
        val button = keys.getOrNull(1)
        if (button == null) {
            LOGGER.warn("KeyCombination $key needs button!")
            return
        }
        val buttonEvent = keys.getOrNull(2)
        if (buttonEvent == null) {
            LOGGER.warn("[WARN] KeyCombination $key needs type!")
            return
        }
        val modifiers = keys.getOrElse(3) { "" }
        val keyComb = KeyCombination.parse(button, buttonEvent, modifiers)
        if (keyComb != null) {
            val values = value.split('|')
            register(namespace, keyComb, values, direction)
        } else {
            LOGGER.warn("Could not parse combination $value")
        }
    }

    /**
     * @param direction 0 = override, -1 = prepend, +1 = append
     * */
    @JvmStatic
    fun register(namespace: String, keyCombination: KeyCombination, actions: List<String>, direction: Int = 0) {
        if (namespace.equals("global", true)) {
            globalKeyCombinations[keyCombination] =
                combine(globalKeyCombinations[keyCombination], actions, direction)
        } else {
            localActions[namespace, keyCombination] =
                combine(localActions[namespace, keyCombination], actions, direction)
        }
    }

    /**
     * @param direction 0 = override, -1 = prepend, +1 = append
     * */
    @JvmStatic
    fun register(namespace: String, keyCombination: KeyCombination, action: String, direction: Int = 0) {
        register(namespace, keyCombination, listOf(action), direction)
    }

    @JvmStatic
    fun combine(oldValues: List<String>?, actions: List<String>, direction: Int): List<String> {
        return when {
            direction == 0 || oldValues == null -> actions
            direction < 0 -> actions + oldValues
            else -> oldValues + actions
        }
    }

    @JvmStatic
    private fun onKeyXXX(window: OSWindow, key: Key, type: KeyCombination.Type) {
        if (key == Key.KEY_UNKNOWN) return
        onEvent(window, 0f, 0f, KeyCombination(key, Input.keyModState, type), false)
    }

    @JvmStatic
    fun onKeyTyped(window: OSWindow, key: Key) {
        onKeyXXX(window, key, KeyCombination.Type.TYPED)
    }

    @JvmStatic
    fun onKeyUp(window: OSWindow, key: Key) {
        onKeyXXX(window, key, KeyCombination.Type.UP)
    }

    @JvmStatic
    fun onKeyDown(window: OSWindow, key: Key) {
        onKeyXXX(window, key, KeyCombination.Type.DOWN)
    }

    @JvmStatic
    fun onKeyDoubleClick(window: OSWindow, key: Key) {
        onKeyXXX(window, key, KeyCombination.Type.DOUBLE)
    }

    @JvmStatic
    fun onKeyHoldDown(window: OSWindow, dx: Float, dy: Float, key: Key, type: KeyCombination.Type) {
        if (key == Key.KEY_UNKNOWN) return
        onEvent(window, dx, dy, KeyCombination(key, Input.keyModState, type), true)
    }

    @JvmStatic
    fun onMouseIdle(window: OSWindow) = onMouseMoved(window, 0f, 0f)

    @JvmStatic
    fun onMouseMoved(window: OSWindow, dx: Float, dy: Float) {
        for ((key, downTime) in Input.keysDown) {
            onKeyHoldDown(window, dx, dy, key, KeyCombination.Type.PRESSING)
            val deltaTime = (Time.nanoTime - downTime) * 1e-9f
            val wasWaiting = deltaTime >= keyDragDelay
            val mouseMoved = Input.mouseHasMoved
            val isDragging = if (enableQuickDragging) {
                wasWaiting || mouseMoved
            } else {
                wasWaiting && !mouseMoved
            }
            if (isDragging) {
                onKeyHoldDown(window, dx, dy, key, KeyCombination.Type.DRAGGING)
            }
        }
    }

    /**
     * returns whether the event was consumed
     * */
    @JvmStatic
    fun onEvent(window: OSWindow, dx: Float, dy: Float, combination: KeyCombination, isContinuous: Boolean): Boolean {
        val stack = window.windowStack
        var panel = stack.inFocus0
        if (stack.isEmpty() || stack.peek() != panel?.window) panel = null
        // filter action keys, if they are typing keys, and a typing field is in focus
        val isWriting = panel != null && panel.isKeyInput() && when {
            combination.isControl || combination.isAlt || combination.isSuper -> false
            combination.key == Key.KEY_SPACE -> !Input.isShiftDown && panel.acceptsChar('\t'.code)
            else -> true
        }
        // LOGGER.info("is writing: $isWriting, combination: $combination, has value? ${combination in globalKeyCombinations}")
        if (!isWriting) {
            executeGlobally(window, 0f, 0f, false, globalKeyCombinations[combination])
        }
        val x = window.mouseX
        val y = window.mouseY
        val localActions = localActions
        val globalActions = localActions["*", combination]
        val print = lastComb != combination
        // if (print) LOGGER.info("-- processing $universally, $combination")
        lastComb = combination
        val lastParentClass = getParentClass(Panel::class)
        targetSearch@ while (panel != null) {
            val actions = localActions[panel.className, combination]
            if (processActions(panel, x, y, dx, dy, isContinuous, actions, print)) {
                return true
            }
            // also check parent classes
            var clazz: KClass<*>? = getParentClass(panel::class)
            while (clazz != null && clazz != lastParentClass) {
                val entry = getByClass(clazz)
                val className = entry?.sampleInstance?.className ?: clazz.simpleName
                if (className != null) {
                    val actions1 = localActions[className, combination]
                    if (processActions(panel, x, y, dx, dy, isContinuous, actions1, print)) {
                        return true
                    }
                }
                clazz = getParentClass(clazz)
            }
            // and if nothing is found at all, check the universal list
            if (processActions(panel, x, y, dx, dy, isContinuous, globalActions, print)) {
                return true
            }
            panel = panel.uiParent
        }
        return false
    }

    private fun getParentClass(clazz: KClass<*>): KClass<*>? {
        return clazz.superclasses.firstOrNull()
    }

    @JvmField
    var lastComb: KeyCombination? = null

    @JvmStatic
    fun processActions(
        panel: Panel,
        x: Float, y: Float, dx: Float, dy: Float,
        isContinuous: Boolean,
        actions: List<String>?,
        @Suppress("unused_parameter") print: Boolean,
    ): Boolean {
        // if (print) LOGGER.info("${panel::class.simpleName}/${panel.className}, ${actions?.size}")
        if (actions == null) return false
        for (i in actions.indices) {
            val action = actions[i]
            if (panel.onGotAction(x, y, dx, dy, action, isContinuous)) {
                // if (print) LOGGER.info("consumed action $action by ${panel::class}")
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun executeLocally(
        window: OSWindow,
        dx: Float, dy: Float, isContinuous: Boolean,
        panel: Panel, actions: List<String>?
    ): Boolean {
        // LOGGER.info("el ${panel::class.simpleName}/${panel.className}, ${actions?.size}")
        if (actions == null) return false
        for (actionIndex in actions.indices) {
            val action = actions[actionIndex]
            if (panel.onGotAction(window.mouseX, window.mouseY, dx, dy, action, isContinuous)) {
                // LOGGER.info("el consumed action $action by ${panel::class}")
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun executeGlobally(window: OSWindow, dx: Float, dy: Float, isContinuous: Boolean, actions: List<String>?) {
        if (actions == null) return
        for (index in actions.indices) {
            val action = actions[index]
            if (globalActions[action]?.invoke() == true) {
                LOGGER.debug("Consumed Global Event $action")
                return
            }
        }
        val ws = window.windowStack
        // LOGGER.info("Executing $actions on all panels")
        for (index in ws.indices) {
            ws[index].panel.forAllPanels { panel ->
                executeLocally(window, dx, dy, isContinuous, panel, actions)
            }
        }
    }

    @JvmStatic
    fun registerGlobalAction(name: String, action: () -> Boolean) {
        globalActions[name] = action
    }
}