package me.anno.tests.gfx.graphs

import me.anno.config.DefaultConfig.style
import me.anno.engine.inspector.CachedReflections.Companion.getEnumId
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.gpu.pipeline.PipelineStage
import me.anno.graph.render.ExprReturnNode
import me.anno.graph.render.RenderGraphEditor
import me.anno.graph.render.effects.BloomNode
import me.anno.graph.render.effects.GizmoNode
import me.anno.graph.render.effects.OutlineEffectNode
import me.anno.graph.render.effects.OutlineEffectSelectNode
import me.anno.graph.render.effects.SSAONode
import me.anno.graph.render.effects.SSRNode
import me.anno.graph.render.scene.CombineLightsNode
import me.anno.graph.render.scene.RenderLightsNode
import me.anno.graph.render.scene.RenderSceneDeferredNode
import me.anno.graph.render.scene.RenderSceneForwardNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.NodeLibrary
import me.anno.graph.types.flow.StartNode
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.tests.gfx.metalRoughness
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI
import kotlin.test.assertEquals

/**
 * Displays a post-processing pipeline in graph form, which can be edited in real-time.
 * */
fun main() {
    forceLoadRenderDoc()
    NodeLibrary.registerClasses()
    registerCustomClass(FlowGraph::class)
    registerCustomClass(StartNode())
    registerCustomClass(RenderSceneDeferredNode())
    registerCustomClass(RenderSceneForwardNode())
    registerCustomClass(RenderLightsNode())
    registerCustomClass(CombineLightsNode())
    registerCustomClass(SSAONode())
    registerCustomClass(SSRNode())
    registerCustomClass(BloomNode())
    registerCustomClass(OutlineEffectSelectNode())
    registerCustomClass(OutlineEffectNode())
    registerCustomClass(GizmoNode())
    registerCustomClass(ExprReturnNode())
    for (entry in PipelineStage.entries) {
        assertEquals(entry.id, getEnumId(entry))
    }
    // todo clone is broken here :/, doesn't clone pipelineStage correctly
    val graph = RenderMode.DEFAULT.renderGraph!!// .clone() as FlowGraph
    val scene = metalRoughness()
    testUI("RenderGraph") {

        EditorState.prefabSource = scene.ref

        val sv = SceneView(PlayMode.EDITING, style)
        val rv = sv.renderer
        rv.orbitCenter.set(0.0, 0.0, -5.0)
        rv.updateEditorCameraTransform()

        val list = CustomList(false, style)
        list.add(sv, 1f)
        list.add(RenderGraphEditor(rv, graph, style), 1f)
        list
    }
}