package me.anno.tests.engine.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.effects.BloomNode
import me.anno.graph.visual.render.effects.GizmoNode
import me.anno.graph.visual.render.effects.SSAONode
import me.anno.graph.visual.render.effects.SSRNode
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.graph.visual.render.scene.RenderSceneDeferredNode
import me.anno.graph.visual.actions.ActionNode
import me.anno.utils.OS.downloads

// todo implement screen space global illumination :3
//  - like in SSAO, we're tracing rays from A to B
//  - we need to do that recursively a few times
//  - we need to "accumulate" color / emissive
//  - we need to sample randomly, so should/can we do that temporarily?

class ScreenSpaceGlobalIlluminationNode : ActionNode(
    "SSGI", listOf(
        "Texture", "Color",
        "Texture", "Emissive",
        "Texture", "Illuminated",
    ), listOf(
        "Texture", "Result"
    )
) {
    override fun executeAction() {
        // todo
    }
}

val SSGIRenderMode = RenderMode(
    "SSGI",
    QuickPipeline()
        .then(RenderSceneDeferredNode())
        .then(RenderLightsNode())
        .then(SSAONode())
        .then(CombineLightsNode())
        .then(ScreenSpaceGlobalIlluminationNode())
        .then(SSRNode())
        .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
        .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
        .finish()
)

fun main() {
    testSceneWithUI("SSGI", downloads.getChild("ogldev-source/crytek_sponza/sponza.obj")) {
        it.renderer.renderMode = SSGIRenderMode
    }
}