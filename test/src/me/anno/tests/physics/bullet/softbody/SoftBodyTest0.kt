package me.anno.tests.physics.bullet.softbody

import com.bulletphysics.softbody.SoftBody
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import org.joml.Vector3f
import org.joml.Vector3i

fun main() {
    val mesh = Mesh()
    SoftBody.cuboid(Vector3f(1f), Vector3i(5, 6, 7), mesh, 1f)
    testSceneWithUI("SoftBody", Entity().add(MeshComponent(mesh)))
}