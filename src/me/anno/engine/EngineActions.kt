package me.anno.engine

import me.anno.gpu.GFX
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.Modifiers
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.utils.StringMap
import me.anno.studio.StudioBase
import me.anno.ui.utils.WindowStack.Companion.printLayout

object EngineActions {

    fun register() {

        val actions = listOf(
            "ToggleFullscreen" to { GFX.toggleFullscreen(); true },
            "PrintLayout" to { printLayout();true },
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
                StudioBase.instance?.clearAll()
                true
            },
            "Redo" to {
                // RemsStudio.history?.redo()
                true
            },
            "Undo" to {
                // RemsStudio.history?.undo()
                true
            },
            "ShowAllObjects" to {
                /*if (RemsStudio.root.listOfAll.any { it.visibility == TransformVisibility.VIDEO_ONLY }) {
                    RemsStudio.largeChange("Show all objects") {
                        RemsStudio.root.listOfAll.filter { it.visibility == TransformVisibility.VIDEO_ONLY }
                            .forEach { it.visibility = TransformVisibility.VISIBLE }
                    }
                    true
                } else */false
            },
            "ToggleHideObject" to {
                /*val obj = Selection.selectedTransform
                if (obj != null) {
                    RemsStudio.largeChange("Toggle Visibility") {
                        obj.visibility = when (obj.visibility) {
                            TransformVisibility.VISIBLE -> TransformVisibility.VIDEO_ONLY
                            else -> TransformVisibility.VISIBLE
                        }
                    }
                    true
                } else */false
            }
        )

        for ((name, action) in actions) {
            ActionManager.registerGlobalAction(name, action)
        }

        ActionManager.createDefaultKeymap = EngineActions::createKeymap

    }

    fun createKeymap(register: StringMap) {

        /**
         * types:
         * - typed -> typed
         * - down -> down
         * - while down -> press
         * - up -> up
         * */

        register["global.space.down.${Modifiers[false, false]}", "Play|Pause"]
        register["global.space.down.${Modifiers[false, true]}", "PlaySlow|Pause"]
        register["global.space.down.${Modifiers[true, false]}", "PlayReversed|Pause"]
        register["global.space.down.${Modifiers[true, true]}", "PlayReversedSlow|Pause"]
        register["global.f11.down", "ToggleFullscreen"]
        register["global.print.down", "PrintLayout"]
        register["global.left.up", "DragEnd"]
        register["global.f5.down.${Modifiers[true, false]}", "ClearCache"]
        register["global.arrowLeft.t", "PreviousStep"]
        register["global.arrowRight.t", "NextStep"]
        register["global.arrowLeft.down.c", "Jump2Start"]
        register["global.arrowRight.down.c", "Jump2End"]
        register["global.comma.t", "PreviousFrame"]
        register["global.dot.t", "NextFrame"]
        register["global.z.t.${Modifiers[true, false]}", "Undo"]
        register["global.z.t.${Modifiers[true, true]}", "Redo"]
        register["global.y.t.${Modifiers[true, false]}", "Undo"]
        register["global.y.t.${Modifiers[true, true]}", "Redo"]
        register["global.h.t.${Modifiers[false, false, true]}", "ShowAllObjects"]
        register["global.h.t", "ToggleHideObject"]

        // press instead of down for the delay
        register["ColorPaletteEntry.left.press", "DragStart"]
        register["SceneTab.left.press", "DragStart"]
        register["FileEntry.left.press", "DragStart"]
        register["FileEntry.left.double", "Enter|Open"]
        register["FileEntry.f2.down", "Rename"]
        // todo only when clicked...
        register["FileEntry.right.down", "OpenOptions"]
        register["FileExplorer.right.down", "OpenOptions"]
        register["FileExplorer.mouseBackward.down", "Back"]
        register["FileExplorer.mouseForward.down", "Forward"]
        register["FileExplorerEntry.left.double", "Enter"]
        register["TreeViewPanel.left.press", "DragStart"]
        register["TreeViewPanel.f2.down", "Rename"]
        register["StackPanel.left.press", "DragStart"]

        register["HSVBox.left.down", "SelectColor"]
        register["HSVBox.left.press-unsafe", "SelectColor"]
        register["AlphaBar.left.down", "SelectColor"]
        register["AlphaBar.left.press-unsafe", "SelectColor"]
        register["HueBar.left.down", "SelectColor"]
        register["HueBar.left.press-unsafe", "SelectColor"]
        register["HSVBoxMain.left.down", "SelectColor"]
        register["HSVBoxMain.left.press-unsafe", "SelectColor"]

        for (i in 0 until 10) {
            // keyMap["SceneView.$i.down", "Cam$i"]
            register["SceneView.numpad$i.down", "Cam$i"]
            // keyMap["SceneView.$i.down.${Modifiers[true, false]}", "Cam$i"]
            register["SceneView.numpad$i.down.${Modifiers[true, false]}", "Cam$i"]
        }

        register["PureTextInputML.delete.typed", "DeleteAfter"]
        register["PureTextInputML.backspace.typed", "DeleteBefore"]
        register["PureTextInputML.leftArrow.typed", "MoveLeft"]
        register["PureTextInputML.rightArrow.typed", "MoveRight"]
        register["PureTextInputML.upArrow.typed", "MoveUp"]
        register["PureTextInputML.downArrow.typed", "MoveDown"]
        register["PureTextInput.leftArrow.typed", "MoveLeft"]
        register["PureTextInput.rightArrow.typed", "MoveRight"]
        register["ConsoleInput.upArrow.typed", "MoveUp"]
        register["ConsoleInput.downArrow.typed", "MoveDown"]

        register["PanelListX.leftArrow.typed", "Previous"]
        register["PanelListX.rightArray.typed", "Next"]
        register["PanelListY.upArrow.typed", "Previous"]
        register["PanelListY.downArrow.typed", "Next"]

        register["FileExplorer.f5.typed", "Refresh"]

        register["ECSTreeView.delete.typed", "Delete"]

    }

}