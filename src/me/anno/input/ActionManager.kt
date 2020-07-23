package me.anno.input

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.hoveredPanel
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.GFX.inFocus0
import me.anno.io.utils.StringMap
import me.anno.objects.Video
import me.anno.objects.cache.Cache
import me.anno.studio.UILayouts
import me.anno.studio.RemsStudio
import me.anno.studio.Studio
import me.anno.studio.Studio.dragged
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.editorTimeDilation
import me.anno.studio.Studio.targetFPS
import me.anno.studio.Studio.updateAudio
import me.anno.ui.base.Panel
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.math.abs
import kotlin.math.round

object ActionManager {

    private val LOGGER = LogManager.getLogger(ActionManager::class)

    val keyDragDelay = DefaultConfig["keyDragDelay", 0.5f]

    val localActions = HashMap<Pair<String, KeyCombination>, List<String>>()

    val globalActions = HashMap<KeyCombination, List<String>>()

    fun init(){

        /**
         * types:
         * - typed -> typed
         * - down -> down
         * - while down -> press
         * - up -> up
         * */

        val defaultValue = StringMap()
        defaultValue["global.space.down.${Modifiers[false, false]}"] = "Play|Pause"
        defaultValue["global.space.down.${Modifiers[false, true]}"] = "PlaySlow|Pause"
        defaultValue["global.space.down.${Modifiers[true, false]}"] = "PlayReversed|Pause"
        defaultValue["global.space.down.${Modifiers[true, true]}"] = "PlayReversedSlow|Pause"
        defaultValue["global.f11.down"] = "ToggleFullscreen"
        defaultValue["global.print.down"] = "PrintLayout"
        defaultValue["global.left.up"] = "DragEnd"
        defaultValue["global.f5.down.${Modifiers[true, false]}"] = "ClearCache"
        defaultValue["global.comma.down"] = "PreviousFrame"
        defaultValue["global.dot.down"] = "NextFrame"

        // press instead of down for the delay
        defaultValue["FileEntry.left.press"] = "DragStart"
        defaultValue["FileEntry.left.double"] = "Enter|Open"
        defaultValue["FileExplorer.right.down"] = "Back"
        defaultValue["TreeViewPanel.left.press"] = "DragStart"

        defaultValue["HSVBox.left.down"] = "selectColor"
        defaultValue["HSVBox.left.press-unsafe"] = "selectColor"

        defaultValue["SceneView.right.p"] = "Turn"
        defaultValue["SceneView.left.p"] = "MoveObject"
        defaultValue["SceneView.left.p.${Modifiers[false, true]}"] = "MoveObjectAlternate"
        defaultValue["SceneView.numpad0.down"] = "ResetCamera"
        defaultValue["SceneView.w.p"] = "MoveForward"
        defaultValue["SceneView.a.p"] = "MoveLeft"
        defaultValue["SceneView.s.p"] = "MoveBackward"
        defaultValue["SceneView.d.p"] = "MoveRight"
        defaultValue["SceneView.q.p"] = "MoveDown"
        defaultValue["SceneView.e.p"] = "MoveUp"
        defaultValue["SceneView.r.p"] = "SetMode(MOVE)"
        defaultValue["SceneView.t.p"] = "SetMode(SCALE)"
        defaultValue["SceneView.z.p"] = "SetMode(ROTATE)"
        defaultValue["SceneView.y.p"] = "SetMode(ROTATE)"


        // todo somehow not working
        defaultValue["GraphEditorBody.arrowLeft.press"] = "MoveLeft"
        defaultValue["GraphEditorBody.arrowRight.press"] = "MoveRight"

        defaultValue["PureTextInputML.delete.typed"] = "DeleteAfter"
        defaultValue["PureTextInputML.backspace.typed"] = "DeleteBefore"
        defaultValue["PureTextInputML.leftArrow.typed"] = "MoveLeft"
        defaultValue["PureTextInputML.rightArrow.typed"] = "MoveRight"
        defaultValue["PureTextInputML.upArrow.typed"] = "MoveUp"
        defaultValue["PureTextInputML.downArrow.typed"] = "MoveDown"
        defaultValue["PureTextInput.leftArrow.typed"] = "MoveLeft"
        defaultValue["PureTextInput.rightArrow.typed"] = "MoveRight"

        parseConfig(defaultValue)

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
                    globalActions[keyComb] = values
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
            val deltaTime = abs(downTime - GFX.lastTime) * 1e-9f
            if(deltaTime >= keyDragDelay){
                onKeyHoldDown(dx, dy, key, true)
            }
        }
    }

    fun onEvent(dx: Float, dy: Float, combination: KeyCombination, isContinuous: Boolean){
        executeGlobally(0f, 0f, false, globalActions[combination])
        val x = Input.mouseX
        val y = Input.mouseY
        var panel = inFocus.firstOrNull()
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

    fun executeGlobally(dx: Float, dy: Float, isContinuous: Boolean,
                        actions: List<String>?){
        if(actions == null) return
        for(action in actions){
            fun setEditorTimeDilation(dilation: Double): Boolean {
                return if(dilation == editorTimeDilation || inFocus0?.isKeyInput() == true) false
                else {
                    editorTimeDilation = dilation
                    true
                }
            }
            if(when(action){
                    "Play" -> setEditorTimeDilation(1.0)
                    "Pause" -> setEditorTimeDilation(0.0)
                    "PlaySlow" -> setEditorTimeDilation(0.2)
                    "PlayReversed" -> setEditorTimeDilation(-1.0)
                    "PlayReversedSlow" -> setEditorTimeDilation(-0.2)
                    "ToggleFullscreen" -> { GFX.toggleFullscreen(); true }
                    "PrintLayout" -> { UILayouts.printLayout();true }
                    "NextFrame" -> {
                        editorTime = (round(editorTime*targetFPS) + 1) / targetFPS
                        updateAudio()
                        true
                    }
                    "PreviousFrame" -> {
                        editorTime = (round(editorTime*targetFPS) - 1) / targetFPS
                        updateAudio()
                        true
                    }
                    "DragEnd" -> {
                        val dragged = dragged
                        if(dragged != null){

                            val data = dragged.getContent()
                            val type = dragged.getContentType()

                            when(type){
                                "File" -> {
                                    hoveredPanel?.onPasteFiles(Input.mouseX, Input.mouseY, listOf(File(data)))
                                }
                                else -> {
                                    hoveredPanel?.onPaste(Input.mouseX, Input.mouseY, data, type)
                                }
                            }

                            Studio.dragged = null

                            true
                        } else false
                    }
                    "ClearCache" -> {
                        Cache.clear()
                        // Video.clearCache()
                        true
                    }
                    else -> false
                }) return
        }
        for(window in RemsStudio.windowStack){
            for(panel in window.panel.listOfAll){
                executeLocally(dx, dy, isContinuous, panel, actions)
            }
        }
    }

}