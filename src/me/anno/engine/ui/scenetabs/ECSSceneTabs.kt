package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig.style
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.RemsEngine
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.studio.StudioBase
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.utils.structures.lists.Lists.getOrPrevious
import org.apache.logging.log4j.LogManager

object ECSSceneTabs : ScrollPanelX(style) {

    // todo if a name is already in use, try a different one

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

    fun open(reference: FileReference, playMode: PlayMode): ECSSceneTab {
        val opened = children3.firstOrNull { it.file == reference }
        return if (opened != null) {
            open(opened)
            opened
        } else {
            val tab = ECSSceneTab(reference, playMode, findName(reference))
            content += tab
            open(tab)
            tab
        }
    }

    fun add(file: FileReference, classNameIfNull: String, playMode: PlayMode): ECSSceneTab {
        val opened = children3.firstOrNull { it.file == file }
        return if (opened == null) {
            val tab = ECSSceneTab(file, classNameIfNull, playMode, findName(file))
            content += tab
            tab
        } else opened
    }

    fun findName(file: FileReference): String {
        var name = file.nameWithoutExtension
        for (tab in children3) {
            if (tab.file != file) {
                var file1 = file
                while (tab.text.equals(name, true)) {
                    val parent = file1.getParent() ?: break
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
                val tab = dragged!!.getOriginal() as ECSSceneTab
                if (!tab.contains(x, y)) {
                    val oldIndex = tab.indexInParent
                    val newIndex = children2.map { it.x + it.w / 2 }.count { it < x }
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

    fun open(file: FileReference, classNameIfNull: String = "Entity", playMode: PlayMode) {
        for (tab in children3) {
            if (tab.file == file && tab.playMode == playMode) {
                focus(tab)
                return
            }
        }
        open(ECSSceneTab(file, classNameIfNull, playMode))
    }

    fun focus(tab: ECSSceneTab) {

        synchronized(this) {
            currentTab = tab
            PrefabInspector.currentInspector = tab.inspector
            // root = sceneTab.root
            // val instance = prefab.getSampleInstance()
            // EditorState.select(instance, null)
            EditorState.select(null)
            if (tab !in children3) content += tab
            for (window in windowStack) {
                window.panel.forAllPanels {
                    if (it is RenderView) {
                        it.playMode = tab.playMode
                    }
                }
            }
        }

        (uiParent ?: this).invalidateDrawing()

    }

    val project get() = (StudioBase.instance as? RemsEngine)?.currentProject

    fun open(tab: ECSSceneTab) {

        if (tab.file.nullIfUndefined() == null) {
            throw RuntimeException("Cannot open InvalidRef as tab on prefab ${System.identityHashCode(tab.prefab)}")
        }

        // add tab to project
        val project = project
        if (project != null) {
            if (project.openTabs.add(tab.file) || project.lastScene != tab.file) {
                project.lastScene = tab.file
                project.invalidate()
            }
        }

        if (tab.parent == null) tab.parent = this
        if (!tab.isHovered) tab.scrollTo()

        val prefab = tab.inspector.prefab
        updatePrefab(prefab)

        focus(tab)

    }

    fun updatePrefab(prefab: Prefab, major: Boolean = true) {
        currentTab?.inspector?.onChange(major) // probably correct ^^
        EditorState.prefabSource = prefab.source
    }

    /*fun createWorld(item: ISaveable, src: FileReference): Entity {
        // todo if there is no lights at all, we should all them "virtually", temporarily
        return when (item) {
            is Entity -> item
            is Mesh -> {
                val entity = Entity()
                entity.add(MeshComponent(src))
                entity
            }
            is Material -> {
                val entity = Entity()
                val mesh = Thumbs.sphereMesh.clone()
                mesh.materials = listOf(src)
                entity.add(mesh)
                entity
            }
            // todo if light, also add some objects for visualization
            is Component -> {
                val entity = item.entity ?: Entity()
                entity.add(item)
                entity
            }
            // todo display skeleton, animations, lights and such
            else -> throw RuntimeException("Cannot open ${item.className} in scene")
        }
    }*/

    fun close(sceneTab: ECSSceneTab) {
        if (currentTab === sceneTab) {
            if (children2.size == 1) {
                LOGGER.warn(Dict["Cannot close last element", "ui.sceneTabs.cannotCloseLast"])
                return
            } else {
                val index = sceneTab.indexInParent
                sceneTab.removeFromParent()
                open(children2.getOrPrevious(index) as ECSSceneTab)
            }
        } else sceneTab.removeFromParent()
        val project = project
        if (project != null) {
            project.openTabs.remove(sceneTab.file)
            project.invalidate()
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        for (it in children3) {
            writer.writeFile("file", it.file)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        try {
            open(files.first(), "Entity", PlayMode.EDITING)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}