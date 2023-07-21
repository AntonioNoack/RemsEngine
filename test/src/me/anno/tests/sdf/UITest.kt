package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.shapes.SDFSphere
import me.anno.sdf.uv.LinearUVMapper
import me.anno.sdf.uv.UVSphereMapper
import me.anno.utils.OS.pictures

// implement and test different UV mapping classes
// more classes?
// to do perspective might be useful...
fun main() {

    val scene = Entity()
    scene.add(SkyBox())

    val materials = listOf(
        Material().apply {
            diffuseMap = pictures.getChild("4k.jpg")
        }.ref
    )

    scene.add(SDFSphere().apply {
        position.x = 0f
        sdfMaterials = materials
        addChild(LinearUVMapper())
    })

    scene.add(SDFSphere().apply {
        position.x = 2.5f
        sdfMaterials = materials
        addChild(UVSphereMapper())
    })

    testSceneWithUI("SDFUI", scene)

}