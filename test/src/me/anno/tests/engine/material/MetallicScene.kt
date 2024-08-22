package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.EngineBase
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS.downloads
import me.anno.utils.OS.res

fun main() {

    OfficialExtensions.initForTests()

    // todo light shine around lamps
    //  particles in air, and viewing ray really close to light source

    // todo we need an exposure setting or auto-exposure (and desaturation in the dark)
    // fixed: SkyboxBase looked like it's not reflected
    val scene = Entity()
    scene.add(SDFSphere().apply {
        name = "Red Sphere"
        val redMetal = Material.metallic(0xe51a1a, 0f)
        sdfMaterials = listOf(redMetal.ref)
    })
    val sky = Skybox()
    scene.add(sky)
    val sun = DirectionalLight()
    sun.shadowMapCascades = 1
    val sunEntity = Entity("Sun")
        .setScale(5.0)
        .addComponent(sun)
    scene.add(sunEntity)
    sky.applyOntoSun(sunEntity, sun, 20f)
    scene.add(Entity("Image", ImagePlane().apply {
        material.diffuseMap = res.getChild("icon.png")
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

    scene.add(
        Entity("Golden Cube", SDFBox().apply {
            val golden = Material.metallic(0xf5ba6c, 1f)
            sdfMaterials = listOf(golden.ref)
        }).setPosition(-2.5, 0.0, 0.0)
    )
    scene.add(
        Entity("Floor", SDFBox())
            .setPosition(0.0, -6.0, 0.0)
            .setScale(5.0)
    )
    val lucy = PrefabCache[downloads.getChild("3d/lucy0.fbx")]?.createInstance() as? Entity
    if (lucy != null) {
        scene.add(lucy.apply {
            name = "Lucy"
            setPosition(0.0, -1.0, -2.5)
            setScale(2.5)
            val golden = Material.metallic(0xf5ba6c, 0f)
            forAllComponentsInChildren(MeshComponent::class) {
                it.materials = listOf(golden.ref)
            }
        })
    }
    val cubes = Entity("Cubes", scene)
    for (j in 0..5) {
        for (i in 0..10) {
            val sc = 0.1
            val child = Entity(cubes)
            child.setPosition((i - 5) * sc * 3.5, -1.0 + sc, 2.5 + (j - 2.5) * sc * 3.5)
            child.setScale(sc)
            child.add(MeshComponent(flatCube.front, Material().apply {
                metallicMinMax.set(j / 5f)
                roughnessMinMax.set(i / 10f)
            }))
        }
    }
    testSceneWithUI("Metallic", scene) {
        EngineBase.enableVSync = false
    }
}