package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.engine.projects.GameEngineProject.Companion.currentProject
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.language.translation.Dict
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.utils.Logging.hash32
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.structures.lists.Lists.getOrPrevious
import org.apache.logging.log4j.LogManager

object ECSSceneTabs : ScrollPanelX(style) {

    private val LOGGER = LogManager.getLogger(ECSSceneTabs::class)

    val content = child as PanelList
    val children2 = content.children
    val children3 get() = children2.filterIsInstance<ECSSceneTab>()

    var currentTab: ECSSceneTab? = null
        set(value) {
            if (field != value) {
                field?.onStop()
                field?.needsStart = false
                value?.needsStart = true
                field = value
            }
        }

    init {
        content.spacing = 4
    }

    fun open(reference: FileReference, playMode: PlayMode, setActive: Boolean): ECSSceneTab {
        val opened = children3.firstOrNull { it.file == reference }
        return if (opened != null) {
            open(opened, setActive)
            opened
        } else {
            val tab = ECSSceneTab(reference, playMode, findName(reference))
            content += tab
            open(tab, setActive)
            tab
        }
    }

    fun add(file: FileReference, playMode: PlayMode): ECSSceneTab {
        val opened = children3.firstOrNull { it.file == file }
        return if (opened == null) {
            val tab = ECSSceneTab(file, playMode, findName(file))
            content += tab
            tab
        } else opened
    }

    fun findName(file: FileReference): String {
        var name = file.nameWithoutExtension
        for (tab in children3) {
            if (tab.file != file) {
                var file1 = file
                // if a name is already in use, try a different one
                while (tab.text.equals(name, true) && file1 != InvalidRef) {
                    val parent = file1.getParent()
                    name = parent.nameWithoutExtension
                    file1 = parent
                }
            }
        }
        return name
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "SceneTab" -> {
                // todo bug: dragging is broken (if it ever worked)
                val tab = dragged!!.getOriginal() as ECSSceneTab
                if (!tab.contains(x, y)) {
                    val oldIndex = tab.indexInParent
                    val newIndex = children2.map { it.x + it.width / 2 }.count2 { it < x }
                    // LOGGER.info("$oldIndex -> $newIndex, $x ${children2.map { it.x + it.w/2 }}")
                    if (oldIndex < newIndex) {
                        children2.add(newIndex, tab)
                        children2.removeAt(oldIndex)
                    } else if (oldIndex > newIndex) {
                        children2.removeAt(oldIndex)
                        children2.add(newIndex, tab)
                    }
                    invalidateLayout()
                }// else done
                dragged = null
            }
            else -> super.onPaste(x, y, data, type)
        }
    }

    fun refocus() {
        PrefabInspector.currentInspector = currentTab?.inspector
    }

    fun focus(tab: ECSSceneTab) {

        synchronized(this) {
            currentTab = tab
            PrefabInspector.currentInspector = tab.inspector
            EditorState.select(null)
            if (tab !in children3) content += tab
            val ws = window?.windowStack
            if (ws != null) for (window in ws) {
                window.panel.forAllPanels {
                    if (it is RenderView) {
                        it.playMode = tab.playMode
                    }
                }
            }
        }

        (uiParent ?: this).invalidateDrawing()
    }

    val project get() = currentProject

    fun open(tab: ECSSceneTab, setActive: Boolean) {

        if (tab.file.nullIfUndefined() == null) {
            throw RuntimeException("Cannot open InvalidRef as tab on prefab ${hash32(tab.prefab)}")
        }

        // add tab to project
        val project = project
        if (project != null) {
            val tabLocal = tab.file.toLocalPath(project.location)
            if (project.openTabs.add(tabLocal) || project.lastScene != tabLocal) {
                project.lastScene = tabLocal
                project.saveMaybe()
            }
        }

        if (tab.parent == null) tab.parent = this

        if (setActive) {
            if (!tab.isHovered) tab.scrollTo()

            val prefab = tab.inspector.prefab
            updatePrefab(prefab)

            focus(tab)
        }
    }

    fun updatePrefab(prefab: Prefab, major: Boolean = true) {
        currentTab?.inspector?.onChange(major) // probably correct ^^
        EditorState.prefabSource = prefab.source
    }

    fun close(sceneTab: ECSSceneTab, setNextActive: Boolean) {
        if (currentTab === sceneTab) {
            if (children2.size == 1) {
                LOGGER.warn(Dict["Cannot close last element", "ui.sceneTabs.cannotCloseLast"])
                return
            } else {
                val index = sceneTab.indexInParent
                sceneTab.removeFromParent()
                open(children2.getOrPrevious(index) as ECSSceneTab, setNextActive)
            }
        } else sceneTab.removeFromParent()
        val project = project
        if (project != null && setNextActive) {
            val failed = project.openTabs.remove(sceneTab.file.toLocalPath(project.location))
            if (failed) LOGGER.warn("Failed to close ${sceneTab.file}!!")
            project.saveMaybe()
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        try {
            open(files.first(), PlayMode.EDITING, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}