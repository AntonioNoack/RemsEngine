package me.anno.tests.gfx.graphs

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.inspector.CachedReflections.Companion.getEnumId
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.gpu.pipeline.PipelineStage
import me.anno.graph.visual.render.RenderGraphEditor
import me.anno.tests.gfx.metalRoughness
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import kotlin.test.assertEquals

/**
 * Displays a post-processing pipeline in graph form, which can be edited in real-time.
 * */
fun main() {
    forceLoadRenderDoc()
    ECSRegistry.init()
    for (entry in PipelineStage.entries) {
        assertEquals(entry.id, getEnumId(entry))
    }
    // todo clone is broken here :/, doesn't clone pipelineStage correctly
    val graph = RenderMode.DEFAULT.renderGraph!!// .clone() as FlowGraph
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