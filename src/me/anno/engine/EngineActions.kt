package me.anno.engine

import me.anno.Build
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.Events.addEvent
import me.anno.gpu.GFX
import me.anno.gpu.GFXState
import me.anno.gpu.debug.DebugGPUStorage
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.io.files.Reference.getReference
import me.anno.io.utils.StringMap
import me.anno.language.translation.Dict
import me.anno.ui.WindowStack.Companion.printLayout
import me.anno.ui.editor.code.CodeEditor
import org.apache.logging.log4j.LogManager

@Suppress("MemberVisibilityCanBePrivate")
object EngineActions {

    private val LOGGER = LogManager.getLogger(EngineActions::class)

    private fun warnNoPanelHovered() {
        LOGGER.warn("No panel was hovered for drop")
    }

    fun register() {

        val actions = listOf(
            "ToggleFullscreen" to { GFX.focusedWindow?.toggleFullscreen(); true },
            "PrintLayout" to { printLayout();true },
            "PrintDictDefaults" to { Dict.printDefaults();true },
            "DragEnd" to {

                val dragged = EngineBase.dragged

                // LOGGER.debug("Executing DragEnd, $dragged")

                if (dragged != null) {

                    val type = dragged.getContentType()
                    val data = dragged.getContent()

                    val window = GFX.focusedWindow
                    if (window != null) when (type) {
                        "File" -> {
                            val hp = EngineBase.instance?.hoveredPanel
                            if (hp != null) hp.onPasteFiles(
                                window.mouseX, window.mouseY,
                                data.split("\n").map { getReference(it) }
                            )
                            else warnNoPanelHovered()
                        }
                        else -> {
                            val hp = EngineBase.instance?.hoveredPanel
                            if (hp != null) hp.onPaste(window.mouseX, window.mouseY, data, type)
                            else warnNoPanelHovered()
                        }
                    } else LOGGER.warn("Could not drop, because no window was focussed")

                    EngineBase.dragged = null

                    true
                } else false
            },
            "ClearCache" to {
                EngineBase.instance?.clearAll()
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
                EngineBase.instance?.save()
                true
            },
            "Paste" to {
                Input.paste(GFX.someWindow)
                true
            },
            "Copy" to {
                Input.copy(GFX.someWindow)
                true
            },
            "Duplicate" to {
                val window = GFX.someWindow
                Input.copy(window)
                Input.paste(window)
                true
            },
            "Cut" to {
                val window = GFX.someWindow
                Input.copy(window)
                Input.empty(window)
                true
            },
            "Import" to {
                Input.import()
                true
            },
            "OpenHistory" to {
                EngineBase.instance?.openHistory()
                true
            },
            "SelectAll" to {
                val ws = GFX.someWindow.windowStack
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
                    addEvent { GFXState.newSession() }
                true
            }
        )

        for ((name, action) in actions) {
            ActionManager.registerGlobalAction(name, action)
        }

        registerCodeEditorActions()

        ActionManager.createDefaultKeymap = EngineActions::createKeymap
    }

    private fun registerCodeEditorActions() {
        try {
            CodeEditor.registerActions()
        } catch (ignored: NoClassDefFoundError) {
        }
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

        register["global.space.down", "Play|Pause"]
        register["global.space.down.s", "PlaySlow|Pause"]
        register["global.space.down.c", "PlayReversed|Pause"]
        register["global.space.down.cs", "PlayReversedSlow|Pause"]
        register["global.f11.down", "ToggleFullscreen"]
        register["global.print.down", "PrintLayout"]
        register["global.print.down.s", "PrintDictDefaults"]
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
        register["FileInput.left.drag", "DragStart"]
        register["FileEntry.left.drag", "DragStart"]
        register["FileExplorerEntry.left.drag", "DragStart"]
        register["ColorPaletteEntry.left.drag", "DragStart"]
        register["SceneTab.left.drag", "DragStart"]
        register["FileEntry.left.drag", "DragStart"]
        register["FileEntry.left.double", "Enter|Open"]
        register["FileEntry.f2.down", "Rename"]
        register["FileEntry.right.down", "OpenOptions"]
        register["FileExplorerEntry.left.double", "Enter|Open"]
        register["FileExplorerEntry.f2.down", "Rename"]
        register["FileExplorerEntry.right.down", "OpenOptions"]
        register["FileExplorer.right.down", "OpenOptions"]
        register["FileExplorer.mouseBackward.down", "Back"]
        register["FileExplorer.mouseForward.down", "Forward"]
        // todo why is inheritance broken?, also we sometimes need to press twice :(
        register["ECSFileExplorer.mouseBackward.down", "Back"]
        register["ECSFileExplorer.mouseForward.down", "Forward"]
        register["TreeViewEntryPanel.left.drag", "DragStart"]
        register["TreeViewEntryPanel.f2.down", "Rename"]
        register["StackPanel.left.drag", "DragStart"]

        register["HSVBox.left.down", "SelectColor"]
        register["HSVBox.left.press", "SelectColor"]
        register["AlphaBar.left.down", "SelectColor"]
        register["AlphaBar.left.press", "SelectColor"]
        register["HueBar.left.down", "SelectColor"]
        register["HueBar.left.press", "SelectColor"]
        register["HSVBoxMain.left.down", "SelectColor"]
        register["HSVBoxMain.left.press", "SelectColor"]

        for (i in 0 until 10) {
            fun registerForClass(clazz: String) {
                // not everyone has a numpad -> support normal number keys, too
                val action = "Cam$i"
                register["$clazz.$i.down", action]
                register["$clazz.$i.down.c", action]
                register["$clazz.numpad$i.down", action]
                register["$clazz.numpad$i.down.c", action]
            }
            registerForClass("SceneView")
            registerForClass("DraggingControls")
        }

        register["TextInput.backspace.typed", "DeleteBefore"]
        register["TextInputML.backspace.typed", "DeleteBefore"]

        // PureTextInputML
        register["PureTextInputML.delete.typed", "DeleteAfter"]
        register["PureTextInputML.backspace.typed", "DeleteBefore"]

        register["PureTextInputML.leftArrow.typed", "MoveLeft"]
        register["PureTextInputML.rightArrow.typed", "MoveRight"]
        register["PureTextInputML.upArrow.typed", "MoveUp"]
        register["PureTextInputML.downArrow.typed", "MoveDown"]
        register["PureTextInputML.leftArrow.typed.s", "MoveLeft"]
        register["PureTextInputML.rightArrow.typed.s", "MoveRight"]
        register["PureTextInputML.upArrow.typed.s", "MoveUp"]
        register["PureTextInputML.downArrow.typed.s", "MoveDown"]

        register["ConsoleInput.upArrow.typed", "MoveUp"]
        register["ConsoleInput.downArrow.typed", "MoveDown"]
        register["NumberInputComponent.leftArrow.typed", "MoveLeft"]
        register["NumberInputComponent.rightArrow.typed", "MoveRight"]

        register["PanelListX.leftArrow.typed", "Previous"]
        register["PanelListX.rightArrow.typed", "Next"]
        register["PanelListY.upArrow.typed", "Previous"]
        register["PanelListY.downArrow.typed", "Next"]

        register["PanelList2D.leftArrow.typed", "Left"]
        register["PanelList2D.rightArrow.typed", "Right"]
        register["PanelList2D.upArrow.typed", "Up"]
        register["PanelList2D.downArrow.typed", "Down"]

        register["FileExplorer.f5.typed", "Refresh"]
        register["FileExplorer.f.typed.c", "OpenSearchBar"]

        register["TreeView.delete.typed", "Delete"]
        register["SceneView.r.typed", "SetMode(MOVE)"]
        register["SceneView.t.typed", "SetMode(ROTATE)"]
        register["SceneView.y.typed", "SetMode(SCALE)"]
        register["SceneView.z.typed", "SetMode(SCALE)"]
    }
}