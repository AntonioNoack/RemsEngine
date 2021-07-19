package me.anno.engine.ui.scenetabs

import me.anno.config.DefaultConfig
import me.anno.ecs.prefab.PrefabInspector
import me.anno.gpu.GFX
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.utils.types.Lists.getOrPrevious
import org.apache.logging.log4j.LogManager

// todo just like the original in Rem's Studio
// todo drop: open that prefab/scene
// todo extra tab to play -> opens the main scene as a game
// todo save tabs...

object ECSSceneTabs : ScrollPanelX(DefaultConfig.style) {

    private val LOGGER = LogManager.getLogger(ECSSceneTabs::class)

    val content = child as PanelList
    val children2 = content.children
    val children3 get() = children2.filterIsInstance<SceneTab>()

    var currentTab: SceneTab? = null

    fun open(file: FileReference) {
        val opened = children3.firstOrNull { it.file == file }
        if (opened != null) {
            open(opened)
        } else {
            GFX.addGPUTask(1) {
                val tab = SceneTab(file)
                content += tab
                open(tab)
            }
        }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        when (type) {
            "SceneTab" -> {
                val tab = dragged!!.getOriginal() as SceneTab
                if (!tab.contains(x, y)) {
                    val oldIndex = tab.indexInParent
                    val newIndex = children2.map { it.x + it.w / 2 }.count { it < x }
                    // println("$oldIndex -> $newIndex, $x ${children2.map { it.x + it.w/2 }}")
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

    fun open(sceneTab: SceneTab) {
        if (currentTab == sceneTab) return
        synchronized(this) {
            currentTab = sceneTab
            PrefabInspector.currentInspector = sceneTab.inspector
            // root = sceneTab.root
            if (sceneTab !in children3) {
                content += sceneTab
            }
        }
    }

    fun close(sceneTab: SceneTab) {
        if (currentTab === sceneTab) {
            if (children2.size == 1) {
                LOGGER.warn(Dict["Cannot close last element", "ui.sceneTabs.cannotCloseLast"])
            } else {
                val index = sceneTab.indexInParent
                sceneTab.removeFromParent()
                open(children2.getOrPrevious(index) as SceneTab)
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

}