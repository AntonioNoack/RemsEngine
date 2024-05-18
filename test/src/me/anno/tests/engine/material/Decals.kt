package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.DecalMaterial
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.maths.Maths.TAU
import me.anno.mesh.Shapes.smoothCube
import me.anno.utils.OS.pictures
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    OfficialExtensions.initForTests()
    val mat = DecalMaterial()
    mat.diffuseMap = getReference("res://icon.png")
    mat.normalMap = pictures.getChild("Cracked Normals.jpg")
    mat.normalStrength = 0.5f
    mat.linearFiltering = false
    mat.writeColor = true
    mat.writeNormal = true
    val scene = Entity("Scene")
    val numDecals = 5
    for(i in 0 until numDecals) {
        val decal = Entity("Decal", scene)
        val angle = i * TAU / numDecals
        decal.setPosition(sin(angle), 0.0, cos(angle))
        decal.setRotation(0.0, angle, 0.0)
        decal.setScale(0.7, 0.7, 0.35)
        decal.add(MeshComponent(smoothCube.front, mat))
    }
    scene.setRotation(0.0, PI, 0.0)
    scene.add(MeshComponent(getReference("meshes/UVSphere.json")).apply {
        materials = listOf(Material().apply {
            diffuseBase.set(1f, 0.7f, 0.3f, 1f)
        }.ref)
    })
    testSceneWithUI("Decals", scene)
}