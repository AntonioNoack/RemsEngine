package me.anno.tests.engine.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.visual.render.QuickPipeline
import me.anno.graph.visual.render.effects.BloomNode
import me.anno.graph.visual.render.effects.DepthToNormalNode
import me.anno.graph.visual.render.effects.GizmoNode
import me.anno.graph.visual.render.effects.OutlineEffectNode
import me.anno.graph.visual.render.effects.OutlineEffectSelectNode
import me.anno.graph.visual.render.effects.SSAONode
import me.anno.graph.visual.render.effects.SSRNode
import me.anno.graph.visual.render.scene.CombineLightsNode
import me.anno.graph.visual.render.scene.RenderLightsNode
import me.anno.graph.visual.render.scene.RenderDeferredNode
import me.anno.mesh.Shapes.flatCube
import org.joml.Vector4f

fun main() {

    val D2NTest1 = RenderMode(
        "D2N Used",
        QuickPipeline()
            .then(RenderDeferredNode())
            .then(DepthToNormalNode())
            .then(RenderLightsNode())
            .then(SSAONode())
            .then(CombineLightsNode())
            .then(SSRNode())
            .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
            .then(OutlineEffectSelectNode())
            .then1(OutlineEffectNode(), mapOf("Fill Colors" to listOf(Vector4f()), "Radius" to 1))
            .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
            .finish()
    )

    val D2NTest2 = RenderMode( // todo how is this completely blank???
        "D2N Result",
        QuickPipeline()
            .then(RenderDeferredNode())
            .then(DepthToNormalNode(), mapOf("Normal" to listOf("Color")))
            .finish()
    )

    testSceneWithUI("DepthToNormal", flatCube.front) {
        it.renderView.renderMode = D2NTest1
    }
}