package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.engine.ui.scenetabs.ECSSceneTab
import me.anno.engine.ui.scenetabs.ECSSceneTabs
import me.anno.mesh.Shapes.flatCube
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI3

/**
 * todo thanks to RenderGraphs, not having the same size of renderviews is probably costly... (because Framebuffers need to be resized constantly)
 * */
fun main() {
    testUI3("Multi-RenderView") {
        val list = CustomList(false, style)
        val scene = Entity(MeshComponent(flatCube.front)).ref
        ECSSceneTabs.open(ECSSceneTab(scene, PlayMode.EDITING), true)
        for (i in 0 until 3) {
            list.add(SceneView(EditorState, PlayMode.EDITING, style))
        }
        list
    }
}