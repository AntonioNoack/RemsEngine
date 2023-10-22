package me.anno.tests.gfx.graphs

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GFXBase
import me.anno.graph.render.RenderGraph
import me.anno.graph.render.RenderGraphEditor
import me.anno.graph.types.NodeLibrary
import me.anno.tests.gfx.metalRoughness
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI

/**
 * Displays a post-processing pipeline in graph form, which can be edited in real-time.
 * */
fun main() {
    GFXBase.forceLoadRenderDoc()
    NodeLibrary.registerClasses()
    val graph = RenderGraph.combined1
    val scene = metalRoughness()
    testUI("RenderGraph") {

        EditorState.prefabSource = scene.ref

        val sv = SceneView(PlayMode.EDITING, style)
        val rv = sv.renderer
        rv.position.set(0.0, 0.0, -5.0)
        rv.updateEditorCameraTransform()

        val list = CustomList(false, style)
        list.add(sv, 1f)
        list.add(RenderGraphEditor(rv, graph, style), 1f)
        list
    }
}