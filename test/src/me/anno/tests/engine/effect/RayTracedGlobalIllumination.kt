package me.anno.tests.engine.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.render.QuickPipeline
import me.anno.graph.render.effects.BloomNode
import me.anno.graph.render.effects.GizmoNode
import me.anno.graph.render.effects.SSAONode
import me.anno.graph.render.effects.SSRNode
import me.anno.graph.render.scene.CombineLightsNode
import me.anno.graph.render.scene.RenderLightsNode
import me.anno.graph.render.scene.RenderSceneDeferredNode
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.utils.OS.downloads

// todo implement ray-traced global illumination
//  - first as an easy sample,
//  - later complex with ReSTIR and such

class RayTracedGlobalIlluminationNode : ActionNode(
    "RTGI", listOf(
        "Texture", "Color",
        "Texture", "Emissive",
        "Texture", "Illuminated",
    ), listOf(
        "Texture", "Result"
    )
) {
    override fun executeAction() {
        // todo
        //  - build/validate BLASes
        //  - build/validate TLASes
        //  - execute RT shader
        //  - temporal reprojection
    }
}

val RTGIRenderMode = RenderMode(
    "RTGI",
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
    testSceneWithUI("RTGI", downloads.getChild("ogldev-source/crytek_sponza/sponza.obj")) {
        it.renderer.renderMode = RTGIRenderMode
    }
}