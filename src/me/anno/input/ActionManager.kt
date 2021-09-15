package me.anno.input

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.GFX.inFocus
import me.anno.io.utils.StringMap
import me.anno.ui.base.Panel
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager

object ActionManager {

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
        for ((key, value) in createDefaultKeymap()) {
            if (key !in keyMap) {
                keyMap[key] = value
            }
        }
        parseConfig(keyMap)

    }

    var createDefaultKeymap = {
        LOGGER.warn("Using default keymap... this should not happen!")
        StringMap()
    }

    fun parseConfig(config: StringMap) {

        for ((key, value) in config.entries) {
            if (value == "") continue // skip it
            val keys = key.split('.')
            val namespace = keys[0]
            val button = keys.getOrNull(1)
            if (button == null) {
                LOGGER.warn("KeyCombination $key needs button!")
                continue
            }
            val buttonEvent = keys.getOrNull(2)
            if (buttonEvent == null) {
                LOGGER.warn("[WARN] KeyCombination $key needs type!")
                continue
            }
            val modifiers = keys.getOrElse(3) { "" }
            val keyComb = KeyCombination.parse(button, buttonEvent, modifiers)
            if (keyComb != null) {
                val values = value.toString().split('|')
                if (namespace.equals("global", true)) {
                    // LOGGER.debug("$value -> global $keyComb")
                    globalKeyCombinations[keyComb] = values
                } else {
                    // LOGGER.debug("$value -> $namespace $keyComb")
                    localActions[namespace, keyComb] = values
                }
            } else {
                LOGGER.warn("Could not parse combination $value")
            }
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
        Input.keysDown.forEach { (key, downTime) ->
            onKeyHoldDown(dx, dy, key, false)
            val deltaTime = (gameTime - downTime) * 1e-9f
            if (deltaTime >= keyDragDelay) {
                onKeyHoldDown(dx, dy, key, true)
            }
        }
    }

    fun onEvent(dx: Float, dy: Float, combination: KeyCombination, isContinuous: Boolean) {
        var panel = inFocus.firstOrNull()
        // filter action keys, if they are typing keys and a typing field is in focus
        val isWriting = combination.isWritingKey && (panel?.isKeyInput() == true)
        // LOGGER.debug("is writing: $isWriting, combination: $combination, has value? ${combination in globalKeyCombinations}")
        if (!isWriting) {
            executeGlobally(0f, 0f, false, globalKeyCombinations[combination])
        }
        val x = Input.mouseX
        val y = Input.mouseY
        val universally = localActions["*", combination]
        targetSearch@ while (panel != null) {
            val clazz = panel.className
            val actions = localActions[clazz, combination] ?: universally
            // LOGGER.debug("searching $clazz, found $actions")
            if (actions != null) {
                for (action in actions) {
                    if (panel.onGotAction(x, y, dx, dy, action, isContinuous)) {
                        break@targetSearch
                    }
                }
            }
            panel = panel.parent
        }
    }

    fun executeLocally(
        dx: Float, dy: Float, isContinuous: Boolean,
        panel: Panel, actions: List<String>?
    ) {
        if (actions == null) return
        for (action in actions) {
            if (panel.onGotAction(Input.mouseX, Input.mouseY, dx, dy, action, isContinuous)) {
                break
            }
        }
    }

    fun executeGlobally(dx: Float, dy: Float, isContinuous: Boolean, actions: List<String>?) {
        if (actions == null) return
        for (action in actions) {
            if (globalActions[action]?.invoke() == true) {
                return
            }
        }
        for (window in GFX.windowStack) {
            window.panel.forAll { panel ->
                executeLocally(dx, dy, isContinuous, panel, actions)
            }
        }
    }

    fun registerGlobalAction(name: String, action: () -> Boolean) {
        globalActions[name] = action
    }

}