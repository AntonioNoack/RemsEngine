package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.WindowRenderFlags
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS.downloads
import me.anno.utils.OS.res

fun main() {

    OfficialExtensions.initForTests()

    // done we need an exposure setting: can be found in RenderGraph
    // todo we need an auto-exposure implementation
    // done: and desaturation in the dark: there is a sample night-shader, which implements that

    testSceneWithUI("Metallic", createMetallicScene()) {
        WindowRenderFlags.enableVSync = false
    }
}

fun createMetallicScene(): Entity {

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
        .setScale(5f)
        .addComponent(sun)
    scene.add(sunEntity)
    sky.applyOntoSun(sunEntity, sun, 20f)
    Entity("Image", scene)
        .add(ImagePlane().apply {
            material.diffuseMap = res.getChild("icon.png")
            // todo way to split transparency rendering into opaque + transparent?
            //  - opaque (a == 1)
            //  - transparent (0 < a < 1)
            material.linearFiltering = false
            // material.pipelineStage = TRANSPARENT_PASS
        }).apply {
            position = position.set(2.0, 0.0, 0.0)
        }
    // fixed: gold didn't look like gold :(
    //  - color was yellow, but reflection of white stuff was blue (because of sky, probably...)

    val gold = 0xf5ba6c
    Entity("Golden Cube", scene)
        .setPosition(-2.5, 0.0, 0.0)
        .add(SDFBox().apply {
            val golden = Material.metallic(gold, 1f)
            sdfMaterials = listOf(golden.ref)
        })

    Entity("Floor", scene)
        .add(SDFBox())
        .setPosition(0.0, -6.0, 0.0)
        .setScale(5f)

    Entity("Lucy", scene)
        .add(MeshComponent(downloads.getChild("3d/lucy0.fbx"), Material.metallic(gold, 0f)))
        .setPosition(0.0, -1.0, -2.5)
        .setScale(2.5f)

    val cubes = Entity("Cubes", scene)
    for (j in 0..5) {
        for (i in 0..10) {
            val sc = 0.1f
            Entity("Cube[$i,$j]", cubes)
                .setPosition((i - 5) * sc * 3.5, -1.0 + sc, 2.5 + (j - 2.5) * sc * 3.5)
                .setScale(sc)
                .add(MeshComponent(flatCube.front, Material().apply {
                    metallicMinMax.set(j / 5f)
                    roughnessMinMax.set(i / 10f)
                }))
        }
    }

    return scene
}