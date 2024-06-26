package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.engine.EngineBase
import me.anno.engine.OfficialExtensions
import me.anno.utils.Color.black
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS.downloads

fun main() {

    OfficialExtensions.initForTests()

    // todo light shine around lamps
    //  particles in air, and viewing ray really close to light source

    // todo metals look cheap in forward rendering -> fix that somehow with cheats

    // todo we need an exposure setting or auto-exposure (and desaturation in the dark)
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
        material.diffuseMap = getReference("res://icon.png")
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
            val golden = Material()
            (0xf5ba6c or black).toVecRGBA(golden.diffuseBase)
            golden.metallicMinMax.set(1f)
            golden.roughnessMinMax.set(1f)
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
            val golden = Material()
            (0xf5ba6c or black).toVecRGBA(golden.diffuseBase)
            golden.metallicMinMax.set(1f)
            golden.roughnessMinMax.set(0f)
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
            child.add(MeshComponent(flatCube.front).apply {
                materials = listOf(Material().apply {
                    metallicMinMax.set(j / 5f)
                    roughnessMinMax.set(i / 10f)
                }.ref)
            })
        }
    }
    testSceneWithUI("Metallic", scene) {
        EngineBase.enableVSync = false
    }
}