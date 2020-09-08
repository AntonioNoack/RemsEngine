package me.anno.input

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFX.hoveredPanel
import me.anno.gpu.GFX.inFocus
import me.anno.gpu.GFX.inFocus0
import me.anno.io.utils.StringMap
import me.anno.objects.cache.Cache
import me.anno.studio.RemsStudio
import me.anno.studio.Studio
import me.anno.studio.Studio.dragged
import me.anno.studio.Studio.editorTime
import me.anno.studio.Studio.editorTimeDilation
import me.anno.studio.Studio.targetFPS
import me.anno.studio.Studio.updateAudio
import me.anno.studio.UILayouts
import me.anno.studio.history.History
import me.anno.ui.base.Panel
import me.anno.ui.editor.TimelinePanel.Companion.moveRight
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

        val keyMap = StringMap()
        keyMap["global.space.down.${Modifiers[false, false]}"] = "Play|Pause"
        keyMap["global.space.down.${Modifiers[false, true]}"] = "PlaySlow|Pause"
        keyMap["global.space.down.${Modifiers[true, false]}"] = "PlayReversed|Pause"
        keyMap["global.space.down.${Modifiers[true, true]}"] = "PlayReversedSlow|Pause"
        keyMap["global.f11.down"] = "ToggleFullscreen"
        keyMap["global.print.down"] = "PrintLayout"
        keyMap["global.left.up"] = "DragEnd"
        keyMap["global.f5.down.${Modifiers[true, false]}"] = "ClearCache"
        keyMap["global.arrowLeft.down"] = "PreviousStep"
        keyMap["global.arrowRight.down"] = "NextStep"
        keyMap["global.arrowLeft.down.c"] = "Jump2Start"
        keyMap["global.arrowRight.down.c"] = "Jump2End"
        keyMap["global.comma.down"] = "PreviousFrame"
        keyMap["global.dot.down"] = "NextFrame"
        keyMap["global.z.down.${Modifiers[true, false]}"] = "Undo"
        keyMap["global.z.down.${Modifiers[true, true]}"] = "Redo"
        keyMap["global.y.down.${Modifiers[true, false]}"] = "Undo"
        keyMap["global.y.down.${Modifiers[true, true]}"] = "Redo"

        // press instead of down for the delay
        keyMap["FileEntry.left.press"] = "DragStart"
        keyMap["FileEntry.left.double"] = "Enter|Open"
        keyMap["FileExplorer.right.down"] = "Back"
        keyMap["FileExplorer.mousebackward.down"] = "Back"
        keyMap["TreeViewPanel.left.press"] = "DragStart"

        keyMap["HSVBox.left.down"] = "selectColor"
        keyMap["HSVBox.left.press-unsafe"] = "selectColor"

        keyMap["SceneView.right.p"] = "Turn"
        keyMap["SceneView.left.p"] = "MoveObject"
        keyMap["SceneView.left.p.${Modifiers[false, true]}"] = "MoveObjectAlternate"
        keyMap["SceneView.numpad0.down"] = "ResetCamera"
        keyMap["SceneView.w.p"] = "MoveForward"
        keyMap["SceneView.a.p"] = "MoveLeft"
        keyMap["SceneView.s.p"] = "MoveBackward"
        keyMap["SceneView.d.p"] = "MoveRight"
        keyMap["SceneView.q.p"] = "MoveDown"
        keyMap["SceneView.e.p"] = "MoveUp"
        keyMap["SceneView.r.p"] = "SetMode(MOVE)"
        keyMap["SceneView.t.p"] = "SetMode(SCALE)"
        keyMap["SceneView.z.p"] = "SetMode(ROTATE)"
        keyMap["SceneView.y.p"] = "SetMode(ROTATE)"

        keyMap["GraphEditorBody.arrowLeft.typed"] = "MoveLeft"
        keyMap["GraphEditorBody.arrowRight.typed"] = "MoveRight"

        keyMap["PureTextInputML.delete.typed"] = "DeleteAfter"
        keyMap["PureTextInputML.backspace.typed"] = "DeleteBefore"
        keyMap["PureTextInputML.leftArrow.typed"] = "MoveLeft"
        keyMap["PureTextInputML.rightArrow.typed"] = "MoveRight"
        keyMap["PureTextInputML.upArrow.typed"] = "MoveUp"
        keyMap["PureTextInputML.downArrow.typed"] = "MoveDown"
        keyMap["PureTextInput.leftArrow.typed"] = "MoveLeft"
        keyMap["PureTextInput.rightArrow.typed"] = "MoveRight"

        parseConfig(keyMap)

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
        var panel = inFocus.firstOrNull()
        // filter action keys, if they are typing keys and a typing field is in focus
        val isWriting = combination.isWritingKey && (panel?.isKeyInput() == true)
        if(!isWriting){
            executeGlobally(0f, 0f, false, globalActions[combination])
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
                    "NextStep" -> {
                        moveRight(1f)
                        true
                    }
                    "PreviousStep" -> {
                        moveRight(-1f)
                        true
                    }
                    "Jump2Start" -> {
                        editorTime = 0.0
                        updateAudio()
                        true
                    }
                    "Jump2End" -> {
                        editorTime = Studio.project?.targetDuration ?: 10.0
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
                    "Redo" -> {
                        History.redo()
                        true }
                    "Undo" -> {
                        History.undo()
                        true }
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