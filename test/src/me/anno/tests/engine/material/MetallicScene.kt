package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.ImagePlane
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Skybox
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.studio.StudioBase
import me.anno.utils.OS.downloads

fun main() {

    // todo light reflections missing? idk... probably light model should have small spheres

    // todo forward lighting model in editor now looks weird/cheap

    // todo if would be nice if we supported FileReference->Material-editing in the same inspector:
    //  - colors might be scene dependent, and that can be important!
    //  -> or open a second scene/window with it

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

    scene.add(
        Entity("Golden Cube", SDFBox().apply {
            val golden = Material()
            golden.diffuseBase.set(0xfd / 255f, 0xb6 / 255f, 0x56 / 255f)
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
            golden.diffuseBase.set(0xfd / 255f, 0xb6 / 255f, 0x56 / 255f)
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
        StudioBase.instance?.enableVSync = false
    }
}