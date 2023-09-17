package me.anno.tests.ecs

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Skybox
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS.downloads

fun main() {
    // todo we need an exposure setting or auto-exposure
    // fixed: SkyboxBase looked like it's not reflected
    val scene = Entity()
    scene.add(SDFSphere().apply {
        name = "Red Sphere"
        val redMetal = Material()
        redMetal.diffuseBase.set(0.9f, 0.1f, 0.1f)
        redMetal.metallicMinMax.set(1f)
        redMetal.roughnessMinMax.set(0f)
        sdfMaterials = listOf(redMetal.ref)
    })
    scene.add(Skybox())
    scene.add(Entity("Image", ImagePlane(getReference("res://icon.png")).apply {
        // todo way to split transparency rendering into opaque + transparent?
        //  - opaque (a == 1)
        //  - transparent (0 < a < 1)
        material.linearFiltering = false
        // material.pipelineStage = TRANSPARENT_PASS
    }).apply {
        position = position.set(2.0, 0.0, 0.0)
    })
    // fixed: gold didn't look like gold :(
    //  - color was yellow, but reflection of white stuff was blue (because of sky, probably...)
    val golden = Material()
    golden.diffuseBase.set(0xfd / 255f, 0xb6 / 255f, 0x56 / 255f)
    golden.metallicMinMax.set(1f)
    golden.roughnessMinMax.set(0f)
    scene.add(Entity("Golden Cube", SDFBox().apply {
        sdfMaterials = listOf(golden.ref)
    }).apply {
        position = position.set(-2.5, 0.0, 0.0)
    })
    scene.add(Entity("Floor", SDFBox()).apply {
        position = position.set(0.0, -6.0, 0.0)
        scale = scale.set(5.0)
    })
    val lucy = PrefabCache[downloads.getChild("3d/lucy0.fbx")]?.createInstance() as? Entity
    if (lucy != null) {
        scene.add(lucy.apply {
            name = "Lucy"
            position = position.set(0.0, -1.0, -2.5)
            scale = scale.set(2.5)
            forAllComponentsInChildren(MeshComponent::class) {
                it.materials = listOf(golden.ref)
            }
        })
    }
    testSceneWithUI("Metallic", scene)
    // todo bug: LIGHT_SUM_MSAA doesn't work
    // but MSAA_DEFERRED does, so idk...
    // todo bug: SSR does not work with MSAA deferred (roughness and metallic look incorrectly bound)
    // todo bug: rotating Gizmos happens around 0 instead of object origin...
}