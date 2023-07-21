package me.anno.tests.ecs

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.decal.DecalMeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference
import me.anno.utils.OS
import me.anno.utils.types.Floats.toRadians

fun main() {
    val decal = Entity("Decal")
    decal.position = decal.position.set(-0.075, 0.09, 0.04)
    decal.rotation = decal.rotation.rotateY((-35.0).toRadians())
    decal.scale = decal.scale.set(0.025, 0.025, 0.01)
    val decal2 = DecalMeshComponent()
    decal.add(decal2)
    val mat = decal2.material
    mat.linearFiltering = false
    mat.diffuseMap = FileReference.getReference("res://icon.png")
    mat.normalMap = OS.pictures.getChild("normal bricks.png")
    mat.writeNormal = true
    val scene = Entity("Object")
    scene.add(MeshComponent(OS.downloads.getChild("3d/bunny.obj"))) // stanford bunny
    scene.add(decal)
    testSceneWithUI("Decals", scene)
}