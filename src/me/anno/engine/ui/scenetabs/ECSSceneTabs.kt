package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.engine.projects.GameEngineProject.Companion.currentProject
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.types.Booleans.toInt
import org.apache.logging.log4j.LogManager

object ECSSceneTabs : ScrollPanelX(style) {

    private val LOGGER = LogManager.getLogger(ECSSceneTabs::class)

    val content = child as PanelList
    val ecsTabsRaw = content.children
    val ecsTabs get() = ecsTabsRaw.filterIsInstance<ECSSceneTab>()

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
        val opened = ecsTabs.firstOrNull { it.file == reference }
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
        val opened = ecsTabs.firstOrNull { it.file == file }
        return if (opened == null) {
            val tab = ECSSceneTab(file, playMode, findName(file))
            content += tab
            tab
        } else opened
    }

    fun findName(file: FileReference): String {
        var name = file.nameWithoutExtension
        for (tab in ecsTabs) {
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

    fun refocus() {
        PrefabInspector.currentInspector = currentTab?.inspector
    }

    fun focus(tab: ECSSceneTab) {

        synchronized(this) {
            currentTab = tab
            PrefabInspector.currentInspector = tab.inspector
            EditorState.select(null)
            if (tab !in ecsTabs) content += tab
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
            val index = sceneTab.indexInParent
            sceneTab.removeFromParent()
            val indices = (0 until index).reversed() +
                    (index until ecsTabs.size)
            var done = false
            for (idx in indices) {
                try {
                    open(ecsTabs[idx], setNextActive)
                    done = true
                    break
                } catch (e: Exception) {
                    LOGGER.warn("$e")
                }
            }
            if (!done) {
                open(flatCube.ref, PlayMode.EDITING, setNextActive)
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
            var lastFile: FileReference = InvalidRef
            for (fi in files.indices) {
                val file = files[fi]
                val playMode = (dragged as? ECSSceneTab.ECSTabDraggable)?.playMode
                val tab = ecsTabs.firstOrNull2 {
                    it.file == file && (playMode == null || it.playMode == playMode)
                }
                if (tab != null && !tab.contains(x, y)) { // swap two tabs
                    val oldIndex = tab.indexInParent
                    val newIndex = ecsTabsRaw.count2 { it.x + it.width.shr(1) < x }
                    if (oldIndex != newIndex) {
                        ecsTabsRaw.removeAt(oldIndex)
                        val removedLeft = oldIndex < newIndex
                        ecsTabsRaw.add(newIndex - removedLeft.toInt(), tab)
                        invalidateLayout()
                    }
                } else {
                    open(file, PlayMode.EDITING, false)
                    lastFile = file
                }
            }
            if (lastFile != InvalidRef) {
                open(lastFile, PlayMode.EDITING, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}