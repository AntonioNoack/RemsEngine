package me.anno.engine

import me.anno.Build
import me.anno.ecs.prefab.PrefabInspector
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.Modifiers
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.utils.StringMap
import me.anno.studio.StudioBase
import me.anno.ui.editor.code.CodeEditor
import me.anno.ui.utils.WindowStack.Companion.printLayout
import me.anno.utils.LOGGER

@Suppress("MemberVisibilityCanBePrivate")
object EngineActions {

    fun register() {

        val actions = listOf(
            "ToggleFullscreen" to { GFX.focusedWindow?.toggleFullscreen(); true },
            "PrintLayout" to { printLayout();true },
            "DragEnd" to {

                val dragged = StudioBase.dragged

                // LOGGER.debug("Executing DragEnd, $dragged")

                if (dragged != null) {

                    val type = dragged.getContentType()
                    val data = dragged.getContent()

                    val window = GFX.focusedWindow
                    if (window != null) when (type) {
                        "File" -> {
                            val hp = StudioBase.instance?.hoveredPanel
                            if (hp != null) hp.onPasteFiles(
                                window.mouseX, window.mouseY,
                                data.split("\n").map { getReference(it) }
                            )
                            else LOGGER.warn("No panel was hovered for drop")
                        }
                        else -> {
                            val hp = StudioBase.instance?.hoveredPanel
                            if (hp != null) hp.onPaste(window.mouseX, window.mouseY, data, type)
                            else LOGGER.warn("No panel was hovered for drop")
                        }
                    } else LOGGER.warn("Could not drop, because no window was focussed")

                    StudioBase.dragged = null

                    true
                } else false
            },
            "ClearCache" to {
                StudioBase.instance?.clearAll()
                true
            },
            "Redo" to { PrefabInspector.currentInspector?.history?.redo() ?: false },
            "Undo" to { PrefabInspector.currentInspector?.history?.undo() ?: false },
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
            },
            "Save" to {
                StudioBase.instance?.save()
                true
            },
            "Paste" to {
                Input.paste(GFX.someWindow!!)
                true
            },
            "Copy" to {
                Input.copy(GFX.someWindow!!)
                true
            },
            "Duplicate" to {
                val window = GFX.someWindow!!
                Input.copy(window)
                Input.paste(window)
                true
            },
            "Cut" to {
                val window = GFX.someWindow!!
                Input.copy(window)
                Input.empty(window)
                true
            },
            "Import" to {
                Input.import()
                true
            },
            "OpenHistory" to {
                StudioBase.instance?.openHistory()
                true
            },
            "SelectAll" to {
                val ws = GFX.someWindow!!.windowStack
                val inFocus0 = ws.inFocus0
                inFocus0?.onSelectAll(ws.mouseX, ws.mouseY)
                true
            },
            "DebugGPUStorage" to {
                DebugGPUStorage.openMenu()
                true
            },
            "ResetOpenGLSession" to {
                if (GFX.canLooseContext)
                    StudioBase.addEvent { GFXState.newSession() }
                true
            }
        )

        for ((name, action) in actions) {
            ActionManager.registerGlobalAction(name, action)
        }

        CodeEditor.registerActions()

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

        register["global.s.t.c", "Save"]
        register["global.c.t.c", "Copy"]
        register["global.v.t.c", "Paste"]
        register["global.x.t.c", "Cut"]
        register["global.d.t.c", "Duplicate"]
        register["global.i.t.c", "Import"]
        register["global.h.t.c", "OpenHistory"]
        register["global.a.t.c", "SelectAll"]

        if (Build.isDebug) {
            register["global.m.t.c", "DebugGPUStorage"]
            register["global.l.t.c", "ResetOpenGLSession"]
            register["global.f5.down.c", "ClearCache"]
        }

        register["global.space.down.${Modifiers[false, false]}", "Play|Pause"]
        register["global.space.down.${Modifiers[false, true]}", "PlaySlow|Pause"]
        register["global.space.down.${Modifiers[true, false]}", "PlayReversed|Pause"]
        register["global.space.down.${Modifiers[true, true]}", "PlayReversedSlow|Pause"]
        register["global.f11.down", "ToggleFullscreen"]
        register["global.print.down", "PrintLayout"]
        register["global.left.up", "DragEnd"]
        register["global.arrowLeft.t", "PreviousStep"]
        register["global.arrowRight.t", "NextStep"]
        register["global.arrowLeft.down.c", "Jump2Start"]
        register["global.arrowRight.down.c", "Jump2End"]
        register["global.comma.t", "PreviousFrame"]
        register["global.dot.t", "NextFrame"]
        register["global.z.t.c", "Undo"]
        register["global.z.t.cs", "Redo"]
        register["global.y.t.c", "Undo"]
        register["global.y.t.cs", "Redo"]
        register["global.h.t.a", "ShowAllObjects"]
        register["global.h.t", "ToggleHideObject"]

        // press instead of down for the delay
        register["FileInput.left.press", "DragStart"]
        register["FileEntry.left.press", "DragStart"]
        register["FileExplorerEntry.left.press", "DragStart"]
        register["ColorPaletteEntry.left.press", "DragStart"]
        register["SceneTab.left.press", "DragStart"]
        register["FileEntry.left.press", "DragStart"]
        register["FileEntry.left.double", "Enter|Open"]
        register["FileEntry.f2.down", "Rename"]
        register["FileEntry.right.down", "OpenOptions"]
        register["FileExplorerEntry.left.double", "Enter|Open"]
        register["FileExplorerEntry.f2.down", "Rename"]
        register["FileExplorerEntry.right.down", "OpenOptions"]
        register["FileExplorer.right.down", "OpenOptions"]
        register["FileExplorer.mouseBackward.down", "Back"]
        register["FileExplorer.mouseForward.down", "Forward"]
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
            register["SceneView.numpad$i.down", "Cam$i"]
            register["SceneView.numpad$i.down.${Modifiers[true, false]}", "Cam$i"]
        }

        register["TextInput.backspace.typed", "DeleteBefore"]
        register["TextInputML.backspace.typed", "DeleteBefore"]
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
        register["NumberInputComponent.leftArrow.typed", "MoveLeft"]
        register["NumberInputComponent.rightArrow.typed", "MoveRight"]

        register["PanelListX.leftArrow.typed", "Previous"]
        register["PanelListX.rightArrow.typed", "Next"]
        register["PanelListY.upArrow.typed", "Previous"]
        register["PanelListY.downArrow.typed", "Next"]

        register["FileExplorer.f5.typed", "Refresh"]

        register["ECSTreeView.delete.typed", "Delete"]
        register["SceneView.r.typed"] = "SetMode(MOVE)"
        register["SceneView.t.typed"] = "SetMode(ROTATE)"
        register["SceneView.y.typed"] = "SetMode(SCALE)"
        register["SceneView.z.typed"] = "SetMode(SCALE)"

    }

}