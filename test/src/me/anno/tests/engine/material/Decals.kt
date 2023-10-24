package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.decal.DecalMeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.sdf.shapes.SDFSphere
import me.anno.utils.OS
import kotlin.math.PI

fun main() {
    val decal = Entity("Decal")
    decal.setPosition(1.0, 0.0, 0.0)
    decal.setRotation(0.0, PI / 2, 0.0)
    decal.setScale(1.0, 1.0, 0.5)
    val decal2 = DecalMeshComponent()
    decal.add(decal2)
    val mat = decal2.material
    mat.diffuseMap = getReference("res://icon.png")
    mat.normalMap = OS.pictures.getChild("normal bricks.png")
    mat.writeColor = true
    mat.writeNormal = true
    val scene = Entity("Object")
    scene.setRotation(0.0, PI, 0.0)
    scene.add(SDFSphere().apply {
        sdfMaterials = listOf(Material().apply {
            diffuseBase.set(1f, 0.7f, 0.3f, 1f)
        }.ref)
    })
    scene.add(decal)
    testSceneWithUI("Decals", scene)
}