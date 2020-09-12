package me.anno.ui.editor.sceneTabs

import me.anno.config.DefaultConfig
import me.anno.objects.Transform
import me.anno.studio.Studio.root
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.editor.files.addChildFromFile
import me.anno.utils.getOrPrevious
import org.apache.logging.log4j.LogManager
import java.io.File

// may there only be once instance? yes
// todo hide bar, if not used?
object SceneTabs : ScrollPanelX(DefaultConfig.style) {

    val LOGGER = LogManager.getLogger(SceneTabs::class)

    val content = child as PanelList
    val children2 = content.children
    val children3 get() = children2.filterIsInstance<SceneTab>()

    var currentTab: SceneTab? = null

    fun open(file: File) {
        val opened = children3.firstOrNull { it.file == file }
        if (opened != null) {
            open(opened)
        } else {
            addChildFromFile(null, file, { transform ->
                var file2 = file
                if(!file2.extension.equals("json", true)){
                    file2 = File(file2.parentFile, file2.name+".json")
                }
                val tab = SceneTab(file2, transform)
                content += tab
                open(tab)
            })
        }
    }

    fun open(transform: Transform) {
        val opened = children3.firstOrNull { it.root === transform }
        if (opened != null) {
            open(opened)
        } else {
            val tab = SceneTab(null, transform)
            content += tab
            open(tab)
        }
    }

    fun open(sceneTab: SceneTab) {
        currentTab = sceneTab
        root = sceneTab.root
    }

    fun close(sceneTab: SceneTab){
        if(currentTab === sceneTab){
            if(children2.size == 1){
                LOGGER.warn("Cannot close last element")
            } else {
                val index = sceneTab.indexInParent
                sceneTab.removeFromParent()
                open(children2.getOrPrevious(index) as SceneTab)
            }
        } else sceneTab.removeFromParent()
    }

    fun closeAll(){
        children2.clear()
    }

}