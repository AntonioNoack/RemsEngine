package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MaterialCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Skybox
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.DepthMode
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.PipelineStage.Companion.TRANSPARENT_PASS
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.pipeline.transparency.GlassPass
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads

fun main() {

    // render scene with transparency

    ECSRegistry.init()
    val scene = Entity()
    scene.addChild(PrefabCache[downloads.getChild("glass_table.glb")]!!
        .createInstance().apply {
            this as Entity
            scale = scale.set(0.01)
        })
    scene.addChild(PrefabCache[downloads.getChild("sphere-glass-4k-materialtest.glb")]!!
        .createInstance().apply {
            this as Entity
            position = position.set(1.2, 0.85, -1.0)
        })
    scene.addChild(PrefabCache[downloads.getChild("glass_bottle-freepoly.org.glb")]!!
        .createInstance().apply {
            this as Entity
            position = position.set(1.6, 0.75, -0.65)
        })
    scene.addChild(PrefabCache[documents.getChild("FineSphere.fbx")]!!
        .createInstance().apply {
            this as Entity
            position = position.set(1.6, 0.85, -1.37)
            scale = scale.set(0.1)
        })
    if (false) scene.addChild(PrefabCache[downloads.getChild("free_1975_porsche_911_930_turbo.zip/scene.gltf")]!!
        .createInstance().apply {
            this as Entity
            position = position.set(-1.6, 0.0, -1.43)
        })
    scene.add(Skybox())
    scene.simpleTraversal {
        if (it is Entity) {
            for (comp in it.components) {
                if (comp is MeshComponent) {
                    val mesh = comp.getMesh() ?: continue
                    val material = MaterialCache[mesh.material] ?: continue
                    println(material.name)
                    when (material.name) {
                        "Piano_vetro", "sanjiaodisikeqiu_t", "08___Default_1001", "DefaultMaterial",
                        "glass", "material_0", "coat", "WINDSHIELD", "Material.003" -> {
                            material.pipelineStage = TRANSPARENT_PASS
                            material.cullMode = CullMode.BOTH
                            material.diffuseBase.w = 0.9f
                            material.metallicMinMax.y = 1f
                        }
                        "930_lights" -> {
                            material.pipelineStage = TRANSPARENT_PASS
                            material.cullMode = CullMode.BOTH
                            material.diffuseBase.w = 0.2f
                        }
                        "930_plastics" -> {
                            material.cullMode = CullMode.BOTH
                        }
                    }
                }
            }
        }
        false
    }
    testSceneWithUI("Scene with Transparency", scene) {
        // it.renderer.renderMode = RenderMode.FORCE_NON_DEFERRED
        it.renderer.pipeline.defaultStage.sorting = Sorting.BACK_TO_FRONT
        it.renderer.pipeline.transparentPass = GlassPass()
        it.renderer.pipeline.stages.add(
            PipelineStage(
                "transparent", Sorting.BACK_TO_FRONT,
                16, BlendMode.DEFAULT,
                DepthMode.CLOSER, false, // both true and false are incorrect here
                CullMode.FRONT, pbrModelShader
            )
        )
    }
}