package me.anno.tests.gfx.graphs

import me.anno.config.DefaultConfig.style
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.pipeline.PipelineStage
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.render.RenderGraphEditor
import me.anno.tests.gfx.metalRoughness
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.Reflections.getEnumId
import me.anno.utils.assertions.assertEquals

/**
 * Displays a post-processing pipeline in graph form, which can be edited in real-time.
 * */
fun main() {
    OfficialExtensions.initForTests()
    for (entry in PipelineStage.entries) {
        assertEquals(entry.id, getEnumId(entry))
    }
    val graph = RenderMode.DEFAULT.renderGraph!!.clone() as FlowGraph
    val scene = metalRoughness()
    testUI("RenderGraph") {

        EditorState.prefabSource = scene.ref

        val sv = SceneView(PlayMode.EDITING, style)
        val rv = sv.renderView
        rv.orbitCenter.set(0.0, 0.0, -5.0)
        rv.updateEditorCameraTransform()

        val list = CustomList(false, style)
        list.add(sv, 1f)
        list.add(RenderGraphEditor(rv, graph, style), 1f)
        list
    }
}