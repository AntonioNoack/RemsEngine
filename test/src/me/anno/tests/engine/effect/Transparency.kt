package me.anno.tests.engine.effect

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.GLASS_PASS
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads

fun main() {

    // render scene with transparency
    // todo scene transforms have become non-deterministic

    // todo set IORs
    // todo can we set an IOR such that a material is always visible?

    ECSRegistry.init()
    val scene = Entity()
    scene.addChild(PrefabCache[downloads.getChild("glass_table.glb")].waitFor()!!
        .newInstance(Entity::class)!!.apply {
            setScale(0.01f)
        })
    scene.addChild(PrefabCache[downloads.getChild("sphere-glass-4k-materialtest.glb")].waitFor()!!
        .newInstance(Entity::class)!!.apply {
            setPosition(1.2, 0.85, -1.0)
        })
    scene.addChild(PrefabCache[downloads.getChild("glass_bottle-freepoly.org.glb")].waitFor()!!
        .newInstance(Entity::class)!!.apply {
            setPosition(1.6, 0.75, -0.65)
        })
    scene.addChild(PrefabCache[documents.getChild("FineSphere.fbx")].waitFor()!!
        .newInstance(Entity::class)!!.apply {
            setPosition(1.6, 0.85, -1.37)
            setScale(0.1f)
        })
    if (false) scene.addChild(PrefabCache[downloads.getChild("free_1975_porsche_911_930_turbo.zip/scene.gltf")].waitFor()!!
        .newInstance(Entity::class)!!.apply {
            setPosition(-1.6, 0.0, -1.43)
        })
    scene.add(Skybox())
    scene.forAllComponentsInChildren(MeshComponent::class) { comp ->
        val mesh = comp.getMeshOrNull()
        val material = MaterialCache.getEntry(mesh?.materials?.firstOrNull()).waitFor()
        if (material != null) {
            println(material.name)
            when (material.name) {
                "Piano_vetro", "sanjiaodisikeqiu_t", "08___Default_1001", "DefaultMaterial",
                "glass", "material_0", "coat", "WINDSHIELD", "Material.003" -> {
                    material.pipelineStage = GLASS_PASS
                    material.cullMode = CullMode.BOTH
                    material.diffuseBase.w = 0.9f
                    material.metallicMinMax.y = 1f
                }
                "930_lights" -> {
                    material.pipelineStage = GLASS_PASS
                    material.cullMode = CullMode.BOTH
                    material.diffuseBase.w = 0.2f
                }
                "930_plastics" -> {
                    material.cullMode = CullMode.BOTH
                }
            }
        }
    }
    testSceneWithUI("Scene with Transparency", scene)
}