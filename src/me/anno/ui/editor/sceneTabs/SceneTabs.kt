package me.anno.ui.editor.sceneTabs

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.language.translation.Dict
import me.anno.studio.StudioBase.Companion.dragged
import me.anno.studio.rems.RemsStudio.root
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.editor.files.FileContentImporter
import me.anno.studio.rems.ui.StudioFileImporter.addChildFromFile
import me.anno.ui.editor.sceneView.SceneTabData
import me.anno.utils.types.Lists.getOrPrevious
import org.apache.logging.log4j.LogManager

// may there only be once instance? yes
object SceneTabs : ScrollPanelX(DefaultConfig.style) {

    private val LOGGER = LogManager.getLogger(SceneTabs::class)

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
                addChildFromFile(null, file, FileContentImporter.SoftLinkMode.COPY_CONTENT, false) { transform ->
                    var file2 = file
                    if (!file2.extension.equals("json", true)) {
                        file2 = getReference(file2.getParent(), file2.name + ".json")
                    }
                    val tab = SceneTab(file2, transform, null)
                    content += tab
                    open(tab)
                }
            }
        }
    }

    /*fun open(transform: Transform) {
        synchronized(this) {
            val opened = children3.firstOrNull { it.root === transform }
            if (opened != null) {
                open(opened)
            } else {
                val tab = SceneTab(null, transform, null)
                content += tab
                open(tab)
            }
        }
    }*/

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
            root = sceneTab.root
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
        children3.forEach {
            writer.add(SceneTabData(it))
        }
    }

}