package me.anno.tests.gfx.forwardplus

import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderMode.Companion.opaqueNodeSettings
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.forceLoadRenderDoc
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.effects.ToneMappingNode
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.tests.engine.light.createLightTypesScene

val bucketSize = 32
val maxLightsPerTile = 32

val lightBucketLayout = bind(Attribute("index", AttributeType.UINT32, 1))

/**
 * Forward+ rendering (very WIP)
 * */
fun main() {

    forceLoadRenderDoc()

    val renderMode = RenderMode(
        "Forward+",
        QuickPipeline()
            .then(FillLightBucketsNode())


            // .then(BoxCullingNode())
            .then1(RenderDeferredNode(), opaqueNodeSettings)
            // .then(RenderDecalsNode())
            .then(RenderLightsNode())
            // .then(SSAONode())
            .then(CombineLightsNode())
            // .then(SSRNode())
            // .then(RenderGlassNode())
            .then(ToneMappingNode())
            // .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
            // .then(OutlineEffectSelectNode())
            // .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
            // .then(GizmoNode())
            // .then(UnditherNode())
            // .then(FXAANode())
            .finish()
    )

    testSceneWithUI("Forward+", createLightTypesScene()) {
        (it.editControls as DraggingControls).settings.renderMode = renderMode
    }
}