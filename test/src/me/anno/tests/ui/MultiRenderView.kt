package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI3

/**
 * thanks to RenderGraphs, not having the same size of renderviews is probably costly... (because Framebuffers need to be resized constantly)
 *  -> yes, 150 fps -> 46 fps
 *  -> fixed that by using FBStack[] :), now it's constantly 170 fps (20 fps higher thanks to Ryzen 2600 -> Ryzen 7950x3D)
 * */
fun main() {
    testUI3("Multi-RenderView") {
        val list = CustomList(false, style)
        ECSSceneTabs.open(ECSSceneTab(flatCube.ref, PlayMode.EDITING), true)
        for (i in 0 until 3) {
            list.add(SceneView(PlayMode.EDITING, style))
        }
        list
    }
}