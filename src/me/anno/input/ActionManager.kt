package me.anno.input

import me.anno.Engine.gameTime
import me.anno.config.DefaultConfig
import me.anno.gpu.WindowX
import me.anno.io.ISaveable
import me.anno.io.utils.StringMap
import me.anno.ui.Panel
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager
import java.util.function.BiConsumer
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object ActionManager : StringMap() {

    private val LOGGER = LogManager.getLogger(ActionManager::class)

    private val keyDragDelay = DefaultConfig["ui.keyDragDelay", 0.5f]

    private val localActions = KeyPairMap<String, KeyCombination, List<String>>(512)

    private val globalKeyCombinations = HashMap<KeyCombination, List<String>>()
    private val globalActions = HashMap<String, () -> Boolean>()

    private lateinit var keyMap: StringMap

    fun init() {

        keyMap = DefaultConfig["ui.keyMap", { StringMap() }]
        // this should be fine
        // if an action is supposed to do nothing, then it should be set to ""
        createDefaultKeymap(keyMap)
        parseConfig(keyMap)

    }

    var createDefaultKeymap: (StringMap) -> Unit = {
        LOGGER.warn("Using default keymap... this should not happen!")
    }

    fun parseConfig(config: StringMap) {
        for ((key, value) in config) {
            value as? String ?: continue
            register(key, value)
        }
    }

    /**
     * @param direction 0 = override, -1 = prepend, +1 = append
     * */
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
    fun register(namespace: String, keyCombination: KeyCombination, action: String, direction: Int = 0) {
        register(namespace, keyCombination, listOf(action), direction)
    }

    fun combine(oldValues: List<String>?, actions: List<String>, direction: Int): List<String> {
        return when {
            direction == 0 || oldValues == null -> actions
            direction < 0 -> actions + oldValues
            else -> oldValues + actions
        }
    }

    fun onKeyTyped(window: WindowX, key: Int) {
        onEvent(window, 0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.TYPED), false)
    }

    fun onKeyUp(window: WindowX, key: Int) {
        onEvent(window, 0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.UP), false)
    }

    fun onKeyDown(window: WindowX, key: Int) {
        onEvent(window, 0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.DOWN), false)
    }

    fun onKeyDoubleClick(window: WindowX, key: Int) {
        onEvent(window, 0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.DOUBLE), false)
    }

    fun onKeyHoldDown(window: WindowX, dx: Float, dy: Float, key: Int, isSafe: Boolean) {
        val type = if (isSafe) KeyCombination.Type.PRESS else KeyCombination.Type.PRESS_UNSAFE
        onEvent(window, dx, dy, KeyCombination(key, Input.keyModState, type), true)
    }

    fun onMouseIdle(window: WindowX) = onMouseMoved(window, 0f, 0f)

    fun onMouseMoved(window: WindowX, dx: Float, dy: Float) {
        if (Input.keysDown.isEmpty()) return
        val mouseMoveConsumer = BiConsumer<Int, Long> { key, downTime ->
            onKeyHoldDown(window, dx, dy, key, false)
            val deltaTime = (gameTime - downTime) * 1e-9f
            val mouseStill = Input.mouseMovementSinceMouseDown < Input.maxClickDistance
            if (deltaTime >= keyDragDelay && mouseStill) {
                onKeyHoldDown(window, dx, dy, key, true)
            }
        }
        Input.keysDown.forEach(mouseMoveConsumer)
    }

    fun onEvent(window: WindowX, dx: Float, dy: Float, combination: KeyCombination, isContinuous: Boolean) {
        val stack = window.windowStack
        var panel = stack.inFocus0
        if (stack.peek() != panel?.window) panel = null
        // filter action keys, if they are typing keys, and a typing field is in focus
        val isWriting = combination.isWritingKey && (panel?.isKeyInput() == true)
        // LOGGER.debug("is writing: $isWriting, combination: $combination, has value? ${combination in globalKeyCombinations}")
        if (!isWriting) {
            executeGlobally(window, 0f, 0f, false, globalKeyCombinations[combination])
        }
        val x = window.mouseX
        val y = window.mouseY
        val la = localActions
        val universally = la["*", combination]
        targetSearch@ while (panel != null) {
            if (processActions(panel, x, y, dx, dy, isContinuous, la[panel.className, combination])) return
            // also check parent classes
            var clazz: KClass<*> = panel::class
            while (true) {
                val entry = ISaveable.getByClass(clazz)
                if (entry != null) {
                    val cnI = entry.sampleInstance.className
                    if (processActions(panel, x, y, dx, dy, isContinuous, la[cnI, combination])) return
                }
                clazz = clazz.superclasses.getOrNull(0) ?: break
            }
            // and if nothing is found at all, check the universal list
            if (processActions(panel, x, y, dx, dy, isContinuous, universally)) return
            panel = panel.uiParent
        }
    }

    fun processActions(
        panel: Panel,
        x: Float, y: Float, dx: Float, dy: Float,
        isContinuous: Boolean,
        actions: List<String>?
    ): Boolean {
        // LOGGER.info("pa ${panel::class.simpleName}/${panel.className}, ${actions?.size}")
        if (actions == null) return false
        for (actionIndex in actions.indices) {
            val action = actions[actionIndex]
            if (panel.onGotAction(x, y, dx, dy, action, isContinuous)) {
                // LOGGER.info("pa consumed action $action by ${panel::class}")
                return true
            }
        }
        return false
    }

    fun executeLocally(
        window: WindowX,
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

    fun executeGlobally(window: WindowX, dx: Float, dy: Float, isContinuous: Boolean, actions: List<String>?) {
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

    fun registerGlobalAction(name: String, action: () -> Boolean) {
        globalActions[name] = action
    }

}