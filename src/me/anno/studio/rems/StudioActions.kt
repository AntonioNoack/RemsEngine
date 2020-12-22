package me.anno.studio.rems

import me.anno.gpu.GFX
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.Modifiers
import me.anno.io.utils.StringMap
import me.anno.cache.Cache
import me.anno.objects.modes.TransformVisibility
import me.anno.studio.StudioBase
import me.anno.ui.editor.TimelinePanel
import me.anno.ui.editor.UILayouts
import java.io.File
import kotlin.math.round

object StudioActions {
    fun register() {

        fun setEditorTimeDilation(dilation: Double): Boolean {
            return if (dilation == RemsStudio.editorTimeDilation || GFX.inFocus0?.isKeyInput() == true) false
            else {
                RemsStudio.editorTimeDilation = dilation
                true
            }
        }

        val actions = listOf(
            "Play" to { setEditorTimeDilation(1.0) },
            "Pause" to { setEditorTimeDilation(0.0) },
            "PlaySlow" to { setEditorTimeDilation(0.2) },
            "PlayReversed" to { setEditorTimeDilation(-1.0) },
            "PlayReversedSlow" to { setEditorTimeDilation(-0.2) },
            "ToggleFullscreen" to { GFX.toggleFullscreen(); true },
            "PrintLayout" to { UILayouts.printLayout();true },
            "NextFrame" to {
                RemsStudio.editorTime = (round(RemsStudio.editorTime * RemsStudio.targetFPS) + 1) / RemsStudio.targetFPS
                StudioBase.updateAudio()
                true
            },
            "PreviousFrame" to {
                RemsStudio.editorTime = (round(RemsStudio.editorTime * RemsStudio.targetFPS) - 1) / RemsStudio.targetFPS
                StudioBase.updateAudio()
                true
            },
            "NextStep" to {
                TimelinePanel.moveRight(1f)
                true
            },
            "PreviousStep" to {
                TimelinePanel.moveRight(-1f)
                true
            },
            "Jump2Start" to {
                RemsStudio.editorTime = 0.0
                StudioBase.updateAudio()
                true
            },
            "Jump2End" to {
                RemsStudio.editorTime = RemsStudio.project?.targetDuration ?: 10.0
                StudioBase.updateAudio()
                true
            },
            "DragEnd" to {
                val dragged = StudioBase.dragged
                if (dragged != null) {

                    val data = dragged.getContent()
                    val type = dragged.getContentType()

                    when (type) {
                        "File" -> {
                            GFX.hoveredPanel?.onPasteFiles(Input.mouseX, Input.mouseY, listOf(File(data)))
                        }
                        else -> {
                            GFX.hoveredPanel?.onPaste(Input.mouseX, Input.mouseY, data, type)
                        }
                    }

                    StudioBase.dragged = null

                    true
                } else false
            },
            "ClearCache" to {
                Cache.clear()
                // Video.clearCache()
                true
            },
            "Redo" to {
                RemsStudio.history?.redo()
                true
            },
            "Undo" to {
                RemsStudio.history?.undo()
                true
            },
            "ShowAllObjects" to {
                if (RemsStudio.root.listOfAll.any { it.visibility == TransformVisibility.VIDEO_ONLY }) {
                    RemsStudio.largeChange("Show all objects"){
                        RemsStudio.root.listOfAll.filter { it.visibility == TransformVisibility.VIDEO_ONLY }
                            .forEach { it.visibility = TransformVisibility.VISIBLE }
                    }
                    true
                } else false
            },
            "ToggleHideObject" to {
                val obj = RemsStudio.selectedTransform
                if (obj != null) {
                    RemsStudio.largeChange("Toggle Visibility"){
                        obj.visibility = when (obj.visibility) {
                            TransformVisibility.VISIBLE -> TransformVisibility.VIDEO_ONLY
                            else -> TransformVisibility.VISIBLE
                        }
                    }
                    true
                } else false
            }
        )
        actions.forEach { (name, action) ->
            ActionManager.registerGlobalAction(name, action)
        }

        ActionManager.createDefaultKeymap = StudioActions::createKeymap

    }

    fun createKeymap(): StringMap {

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
        keyMap["global.arrowLeft.t"] = "PreviousStep"
        keyMap["global.arrowRight.t"] = "NextStep"
        keyMap["global.arrowLeft.down.c"] = "Jump2Start"
        keyMap["global.arrowRight.down.c"] = "Jump2End"
        keyMap["global.comma.t"] = "PreviousFrame"
        keyMap["global.dot.t"] = "NextFrame"
        keyMap["global.z.t.${Modifiers[true, false]}"] = "Undo"
        keyMap["global.z.t.${Modifiers[true, true]}"] = "Redo"
        keyMap["global.y.t.${Modifiers[true, false]}"] = "Undo"
        keyMap["global.y.t.${Modifiers[true, true]}"] = "Redo"
        keyMap["global.h.t.${Modifiers[false, false, true]}"] = "ShowAllObjects"
        keyMap["global.h.t"] = "ToggleHideObject"

        // press instead of down for the delay
        keyMap["SceneTab.left.press"] = "DragStart"
        keyMap["FileEntry.left.press"] = "DragStart"
        keyMap["FileEntry.left.double"] = "Enter|Open"
        keyMap["FileEntry.f2.press"] = "Rename"
        keyMap["FileEntry.right.down"] = "OpenOptions"
        keyMap["FileExplorer.right.down"] = "OpenOptions"
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

        keyMap["PureTextInputML.delete.typed"] = "DeleteAfter"
        keyMap["PureTextInputML.backspace.typed"] = "DeleteBefore"
        keyMap["PureTextInputML.leftArrow.typed"] = "MoveLeft"
        keyMap["PureTextInputML.rightArrow.typed"] = "MoveRight"
        keyMap["PureTextInputML.upArrow.typed"] = "MoveUp"
        keyMap["PureTextInputML.downArrow.typed"] = "MoveDown"
        keyMap["PureTextInput.leftArrow.typed"] = "MoveLeft"
        keyMap["PureTextInput.rightArrow.typed"] = "MoveRight"

        return keyMap

    }

}