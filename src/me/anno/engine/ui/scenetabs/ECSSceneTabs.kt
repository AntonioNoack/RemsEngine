package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX.windowStack
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.editor.files.thumbs.Thumbs
import me.anno.utils.hpc.SyncMaster
import me.anno.utils.types.Lists.getOrPrevious
import org.apache.logging.log4j.LogManager

// todo just like the original in Rem's Studio
// todo drop: open that prefab/scene
// todo extra tab to play -> opens the main scene as a game
// todo save tabs...

// todo show the current scene with a different background color
// todo the same for Rems Studio
object ECSSceneTabs : ScrollPanelX(DefaultConfig.style) {

    private val LOGGER = LogManager.getLogger(ECSSceneTabs::class)

    val content = child as PanelList
    val children2 = content.children
    val children3 get() = children2.filterIsInstance<ECSSceneTab>()

    var currentTab: ECSSceneTab? = null
        set(value) {
            if (field != value) {
                field?.onStop()
                value?.onStart()
                field = value
            }
        }

    fun open(syncMaster: SyncMaster, prefab: Prefab): ECSSceneTab {
        val opened = children3.firstOrNull { it.file == prefab.src }
        return if (opened != null) {
            open(opened)
            opened
        } else {
            val tab = ECSSceneTab(syncMaster, prefab)
            content += tab
            open(tab)
            tab
        }
    }

    fun open(syncMaster: SyncMaster, file: FileReference, classNameIfNull: String): ECSSceneTab {
        val tab = add(syncMaster, file, classNameIfNull)
        open(tab)
        return tab
    }

    fun add(syncMaster: SyncMaster, file: FileReference, classNameIfNull: String): ECSSceneTab {
        val opened = children3.firstOrNull { it.file == file }
        return if (opened == null) {
            val tab = ECSSceneTab(syncMaster, file, classNameIfNull)
            content += tab
            tab
        } else opened
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

    fun open(sceneTab: ECSSceneTab) {
        if (currentTab == sceneTab) return
        synchronized(this) {
            currentTab = sceneTab
            PrefabInspector.currentInspector = sceneTab.inspector
            // root = sceneTab.root
            val prefab = sceneTab.inspector.prefab
            val prefabInstance = prefab.createInstance()
            val world = lazy { createWorld(prefabInstance, prefab.src) }
            for (window in windowStack) {
                window.panel.listOfAll {
                    when (it) {
                        is RenderView -> {
                            val library = it.library
                            library.world = world.value
                            library.select(prefabInstance)
                            println("setting world to ${library.world}")
                        }
                        is ECSTreeView -> {
                            it.invalidateLayout()
                        }
                    }
                }
            }
            if (sceneTab !in children3) {
                content += sceneTab
            }
            for (panel in children) panel.tickUpdate() // to assign the colors without delay
        }
    }

    private fun createWorld(item: ISaveable, src: FileReference): Entity {
        // todo if there is no lights at all, we should all them "virtually", temporarily
        return when (item) {
            is Entity -> item
            is Mesh -> {
                val entity = Entity()
                entity.add(MeshComponent(src))
                entity.add(MeshRenderer())
                entity
            }
            is Material -> {
                val entity = Entity()
                val mesh = Thumbs.sphereMesh.clone()
                mesh.materials = listOf(src)
                entity.add(mesh)
                entity.add(MeshRenderer())
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
    }

    fun close(sceneTab: ECSSceneTab) {
        if (currentTab === sceneTab) {
            if (children2.size == 1) {
                LOGGER.warn(Dict["Cannot close last element", "ui.sceneTabs.cannotCloseLast"])
            } else {
                val index = sceneTab.indexInParent
                sceneTab.removeFromParent()
                open(children2.getOrPrevious(index) as ECSSceneTab)
            }
        } else sceneTab.removeFromParent()
    }

    fun closeAll() {
        children2.clear()
    }

    fun save(writer: BaseWriter) {
        for (it in children3) {
            writer.writeFile("file", it.file)
        }
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val syncMaster = rootPanel.listOfAll.filterIsInstance<RenderView>().firstOrNull()?.library?.syncMaster
        if (syncMaster != null) {
            try {
                open(syncMaster, files.first(), "Entity")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}