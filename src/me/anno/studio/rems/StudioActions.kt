package me.anno.studio.rems

import me.anno.cache.CacheSection
import me.anno.gpu.GFX
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.Modifiers
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.utils.StringMap
import me.anno.remsstudio.objects.modes.TransformVisibility
import me.anno.remsstudio.RemsStudio
import me.anno.remsstudio.Selection
import me.anno.studio.StudioBase
import me.anno.remsstudio.ui.editor.TimelinePanel
import me.anno.remsstudio.RemsStudioUILayouts
import me.anno.ui.utils.WindowStack.Companion.printLayout
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
            "PrintLayout" to { printLayout();true },
            "NextFrame" to {
                RemsStudio.editorTime = (round(RemsStudio.editorTime * RemsStudio.targetFPS) + 1) / RemsStudio.targetFPS
                RemsStudio.updateAudio()
                true
            },
            "PreviousFrame" to {
                RemsStudio.editorTime = (round(RemsStudio.editorTime * RemsStudio.targetFPS) - 1) / RemsStudio.targetFPS
                RemsStudio.updateAudio()
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
                RemsStudio.updateAudio()
                true
            },
            "Jump2End" to {
                RemsStudio.editorTime = RemsStudio.project?.targetDuration ?: 10.0
                RemsStudio.updateAudio()
                true
            },
            "DragEnd" to {
                val dragged = StudioBase.dragged
                if (dragged != null) {

                    val type = dragged.getContentType()
                    val data = dragged.getContent()

                    when (type) {
                        "File" -> {
                            GFX.hoveredPanel?.onPasteFiles(
                                Input.mouseX, Input.mouseY,
                                data.split("\n").map { getReference(it) }
                            )
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
                CacheSection.clearAll()
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
                    RemsStudio.largeChange("Show all objects") {
                        RemsStudio.root.listOfAll.filter { it.visibility == TransformVisibility.VIDEO_ONLY }
                            .forEach { it.visibility = TransformVisibility.VISIBLE }
                    }
                    true
                } else false
            },
            "ToggleHideObject" to {
                val obj = Selection.selectedTransform
                if (obj != null) {
                    RemsStudio.largeChange("Toggle Visibility") {
                        obj.visibility = when (obj.visibility) {
                            TransformVisibility.VISIBLE -> TransformVisibility.VIDEO_ONLY
                            else -> TransformVisibility.VISIBLE
                        }
                    }
                    true
                } else false
            }
        )

        for ((name, action) in actions) {
            ActionManager.registerGlobalAction(name, action)
        }

        ActionManager.createDefaultKeymap = StudioActions::createKeymap

    }

    fun createKeymap(keyMap: StringMap) {

        /**
         * types:
         * - typed -> typed
         * - down -> down
         * - while down -> press
         * - up -> up
         * */

        keyMap["global.space.down.${Modifiers[false, false]}", "Play|Pause"]
        keyMap["global.space.down.${Modifiers[false, true]}", "PlaySlow|Pause"]
        keyMap["global.space.down.${Modifiers[true, false]}", "PlayReversed|Pause"]
        keyMap["global.space.down.${Modifiers[true, true]}", "PlayReversedSlow|Pause"]
        keyMap["global.f11.down", "ToggleFullscreen"]
        keyMap["global.print.down", "PrintLayout"]
        keyMap["global.left.up", "DragEnd"]
        keyMap["global.f5.down.${Modifiers[true, false]}", "ClearCache"]
        keyMap["global.arrowLeft.t", "PreviousStep"]
        keyMap["global.arrowRight.t", "NextStep"]
        keyMap["global.arrowLeft.down.c", "Jump2Start"]
        keyMap["global.arrowRight.down.c", "Jump2End"]
        keyMap["global.comma.t", "PreviousFrame"]
        keyMap["global.dot.t", "NextFrame"]
        keyMap["global.z.t.${Modifiers[true, false]}", "Undo"]
        keyMap["global.z.t.${Modifiers[true, true]}", "Redo"]
        keyMap["global.y.t.${Modifiers[true, false]}", "Undo"]
        keyMap["global.y.t.${Modifiers[true, true]}", "Redo"]
        keyMap["global.h.t.${Modifiers[false, false, true]}", "ShowAllObjects"]
        keyMap["global.h.t", "ToggleHideObject"]

        // press instead of down for the delay
        keyMap["ColorPaletteEntry.left.press", "DragStart"]
        keyMap["SceneTab.left.press", "DragStart"]
        keyMap["FileEntry.left.press", "DragStart"]
        keyMap["FileEntry.left.double", "Enter|Open"]
        keyMap["FileEntry.f2.down", "Rename"]
        // todo only when clicked...
        keyMap["FileEntry.right.down", "OpenOptions"]
        keyMap["FileExplorer.right.down", "OpenOptions"]
        keyMap["FileExplorer.mouseBackward.down", "Back"]
        keyMap["FileExplorer.mouseForward.down", "Forward"]
        keyMap["FileExplorerEntry.left.double", "Enter"]
        keyMap["TreeViewPanel.left.press", "DragStart"]
        keyMap["TreeViewPanel.f2.down", "Rename"]
        keyMap["StackPanel.left.press", "DragStart"]

        keyMap["HSVBox.left.down", "SelectColor"]
        keyMap["HSVBox.left.press-unsafe", "SelectColor"]
        keyMap["AlphaBar.left.down", "SelectColor"]
        keyMap["AlphaBar.left.press-unsafe", "SelectColor"]
        keyMap["HueBar.left.down", "SelectColor"]
        keyMap["HueBar.left.press-unsafe", "SelectColor"]
        keyMap["HSVBoxMain.left.down", "SelectColor"]
        keyMap["HSVBoxMain.left.press-unsafe", "SelectColor"]

        keyMap["StudioSceneView.right.p", "Turn"]
        keyMap["StudioSceneView.left.p", "MoveObject"]
        keyMap["StudioSceneView.left.p.${Modifiers[false, true]}", "MoveObjectAlternate"]

        for (i in 0 until 10) {
            // keyMap["SceneView.$i.down", "Cam$i"]
            keyMap["SceneView.numpad$i.down", "Cam$i"]
            // keyMap["SceneView.$i.down.${Modifiers[true, false]}", "Cam$i"]
            keyMap["SceneView.numpad$i.down.${Modifiers[true, false]}", "Cam$i"]
        }

        keyMap["StudioSceneView.w.p", "MoveForward"]
        keyMap["StudioSceneView.a.p", "MoveLeft"]
        keyMap["StudioSceneView.s.p", "MoveBackward"]
        keyMap["StudioSceneView.d.p", "MoveRight"]
        keyMap["StudioSceneView.q.p", "MoveDown"]
        keyMap["StudioSceneView.e.p", "MoveUp"]
        keyMap["StudioSceneView.r.p", "SetMode(MOVE)"]
        keyMap["StudioSceneView.t.p", "SetMode(ROTATE)"]
        keyMap["StudioSceneView.z.p", "SetMode(SCALE)"]
        keyMap["StudioSceneView.y.p", "SetMode(SCALE)"]

        keyMap["PureTextInputML.delete.typed", "DeleteAfter"]
        keyMap["PureTextInputML.backspace.typed", "DeleteBefore"]
        keyMap["PureTextInputML.leftArrow.typed", "MoveLeft"]
        keyMap["PureTextInputML.rightArrow.typed", "MoveRight"]
        keyMap["PureTextInputML.upArrow.typed", "MoveUp"]
        keyMap["PureTextInputML.downArrow.typed", "MoveDown"]
        keyMap["PureTextInput.leftArrow.typed", "MoveLeft"]
        keyMap["PureTextInput.rightArrow.typed", "MoveRight"]
        keyMap["ConsoleInput.upArrow.typed", "MoveUp"]
        keyMap["ConsoleInput.downArrow.typed", "MoveDown"]

        keyMap["PanelListX.leftArrow.typed", "Previous"]
        keyMap["PanelListX.rightArray.typed", "Next"]
        keyMap["PanelListY.upArrow.typed", "Previous"]
        keyMap["PanelListY.downArrow.typed", "Next"]

        keyMap["FileExplorer.f5.typed", "Refresh"]

    }

}