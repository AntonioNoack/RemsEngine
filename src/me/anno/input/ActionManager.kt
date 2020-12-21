package me.anno.input

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX.gameTime
import me.anno.gpu.GFX.inFocus
import me.anno.io.utils.StringMap
import me.anno.studio.rems.RemsStudio
import me.anno.ui.base.Panel
import org.apache.logging.log4j.LogManager

object ActionManager {

    private val LOGGER = LogManager.getLogger(ActionManager::class)

    private val keyDragDelay = DefaultConfig["ui.keyDragDelay", 0.5f]

    private val localActions = HashMap<Pair<String, KeyCombination>, List<String>>()

    private val globalKeyCombinations = HashMap<KeyCombination, List<String>>()
    private val globalActions = HashMap<String, () -> Boolean>()

    private lateinit var keyMap: StringMap

    fun init(){

        keyMap = DefaultConfig["ui.keyMap", {
            createDefaultKeymap()
        }]
        parseConfig(keyMap)

    }

    var createDefaultKeymap = {
        LOGGER.warn("Using default keymap... this should not happen!")
        StringMap()
    }

    fun parseConfig(config: StringMap){

        for((key, value) in config.entries){
            val keys = key.split('.')
            val namespace = keys[0]
            val button = keys.getOrNull(1)
            if(button == null){
                LOGGER.warn("KeyCombination $key needs button!")
                continue
            }
            val buttonEvent = keys.getOrNull(2)
            if(buttonEvent == null){
                LOGGER.warn("[WARN] KeyCombination $key needs type!")
                continue
            }
            val modifiers = keys.getOrElse(3){ "" }
            val keyComb = KeyCombination.parse(button, buttonEvent, modifiers)
            if(keyComb != null){
                val values = value.toString().split('|')
                if(namespace.equals("global", true)){
                    globalKeyCombinations[keyComb] = values
                } else {
                    localActions[namespace to keyComb] = values
                }
            }
        }

    }

    fun onKeyTyped(key: Int){
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.TYPED), false)
    }

    fun onKeyUp(key: Int){
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.UP), false)
    }

    fun onKeyDown(key: Int){
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.DOWN), false)
    }

    fun onKeyDoubleClick(key: Int){
        onEvent(0f, 0f, KeyCombination(key, Input.keyModState, KeyCombination.Type.DOUBLE), false)
    }

    fun onKeyHoldDown(dx: Float, dy: Float, key: Int, save: Boolean){
        onEvent(dx, dy, KeyCombination(key, Input.keyModState, if(save) KeyCombination.Type.PRESS else KeyCombination.Type.PRESS_UNSAFE), true)
    }

    fun onMouseIdle() = onMouseMoved(0f, 0f)

    fun onMouseMoved(dx: Float, dy: Float){
        Input.keysDown.forEach { (key, downTime) ->
            onKeyHoldDown(dx, dy, key, false)
            val deltaTime = (gameTime - downTime) * 1e-9f
            if(deltaTime >= keyDragDelay){
                onKeyHoldDown(dx, dy, key, true)
            }
        }
    }

    fun onEvent(dx: Float, dy: Float, combination: KeyCombination, isContinuous: Boolean){
        var panel = inFocus.firstOrNull()
        // filter action keys, if they are typing keys and a typing field is in focus
        val isWriting = combination.isWritingKey && (panel?.isKeyInput() == true)
        if(!isWriting){
            executeGlobally(0f, 0f, false, globalKeyCombinations[combination])
        }
        val x = Input.mouseX
        val y = Input.mouseY
        targetSearch@ while(panel != null){
            val clazz = panel.getClassName()
            val actions = localActions[clazz to combination] ?: localActions["*" to combination]
            if(actions != null){
                for(action in actions){
                    if(panel.onGotAction(x, y, dx, dy, action, isContinuous)){
                        break@targetSearch
                    }
                }
            }
            panel = panel.parent
        }
    }

    fun executeLocally(dx: Float, dy: Float, isContinuous: Boolean,
                       panel: Panel, actions: List<String>?){
        if(actions == null) return
        for(action in actions){
            if(panel.onGotAction(Input.mouseX, Input.mouseY, dx, dy, action, isContinuous)){
                break
            }
        }
    }

    fun executeGlobally(dx: Float, dy: Float, isContinuous: Boolean, actions: List<String>?){
        if(actions == null) return
        for(action in actions){
            if(globalActions[action]?.invoke() == true){
                return
            }
        }
        for(window in RemsStudio.windowStack){
            for(panel in window.panel.listOfAll){
                executeLocally(dx, dy, isContinuous, panel, actions)
            }
        }
    }

    fun registerGlobalAction(name:String, action: () -> Boolean){
        globalActions[name] = action
    }

}