package me.anno.input

import me.anno.Engine.gameTime
import me.anno.config.DefaultConfig
import me.anno.io.ISaveable
import me.anno.io.utils.StringMap
import me.anno.studio.StudioBase.Companion.defaultWindowStack
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
        for ((key, value) in config.entries) {
            register(key, value.toString())
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

    fun onKeyTyped(key: Int) {
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.TYPED), false)
    }

    fun onKeyUp(key: Int) {
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.UP), false)
    }

    fun onKeyDown(key: Int) {
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.DOWN), false)
    }

    fun onKeyDoubleClick(key: Int) {
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.DOUBLE), false)
    }

    fun onKeyHoldDown(dx: Float, dy: Float, key: Int, save: Boolean) {
        val type = if (save) KeyCombination.Type.PRESS else KeyCombination.Type.PRESS_UNSAFE
        onEvent(dx, dy, KeyCombination(key, Input.keyModState, type), true)
    }

    fun onMouseIdle() = onMouseMoved(0f, 0f)

    fun onMouseMoved(dx: Float, dy: Float) {
        if (Input.keysDown.isEmpty()) return
        val mouseMoveConsumer = BiConsumer<Int, Long> { key, downTime ->
            onKeyHoldDown(dx, dy, key, false)
            val deltaTime = (gameTime - downTime) * 1e-9f
            if (deltaTime >= keyDragDelay) {
                onKeyHoldDown(dx, dy, key, true)
            }
        }
        Input.keysDown.forEach(mouseMoveConsumer)
    }

    // todo this maybe should exist on a per-windowStack basis,
    // todo so all actions are redirected through a game-window
    fun onEvent(dx: Float, dy: Float, combination: KeyCombination, isContinuous: Boolean) {
        var panel = defaultWindowStack?.inFocus0
        // filter action keys, if they are typing keys and a typing field is in focus
        val isWriting = combination.isWritingKey && (panel?.isKeyInput() == true)
        // LOGGER.debug("is writing: $isWriting, combination: $combination, has value? ${combination in globalKeyCombinations}")
        if (!isWriting) {
            executeGlobally(0f, 0f, false, globalKeyCombinations[combination])
        }
        val x = Input.mouseX
        val y = Input.mouseY
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
        // println("pa ${panel::class.simpleName}/${panel.className}, ${actions?.size}")
        if (actions == null) return false
        for (actionIndex in actions.indices) {
            val action = actions[actionIndex]
            if (panel.onGotAction(x, y, dx, dy, action, isContinuous)) {
                // println("pa consumed action $action by ${panel::class}")
                return true
            }
        }
        return false
    }

    fun executeLocally(
        dx: Float, dy: Float, isContinuous: Boolean,
        panel: Panel, actions: List<String>?
    ): Boolean {
        // println("el ${panel::class.simpleName}/${panel.className}, ${actions?.size}")
        if (actions == null) return false
        for (actionIndex in actions.indices) {
            val action = actions[actionIndex]
            if (panel.onGotAction(Input.mouseX, Input.mouseY, dx, dy, action, isContinuous)) {
                // println("el consumed action $action by ${panel::class}")
                return true
            }
        }
        return false
    }

    fun executeGlobally(dx: Float, dy: Float, isContinuous: Boolean, actions: List<String>?) {
        if (actions == null) return
        for (actionIndex in actions.indices) {
            val action = actions[actionIndex]
            if (globalActions[action]?.invoke() == true) {
                // println("eg, consumed $action")
                return
            }
        }
        val ws = defaultWindowStack
        if (ws == null) {
            LOGGER.warn("WindowStack is null")
            return
        }
        // LOGGER.info("Executing $actions on all panels")
        for (index in ws.indices) {
            ws[index].panel.forAllPanels { panel ->
                executeLocally(dx, dy, isContinuous, panel, actions)
            }
        }
    }

    fun registerGlobalAction(name: String, action: () -> Boolean) {
        globalActions[name] = action
    }

}