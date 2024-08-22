package me.anno.tests.collider

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.OS
import org.joml.Quaterniond

fun main() {

    OfficialExtensions.initForTests()

    val mesh = OS.documents.getChild("redMonkey.glb")
    val meshCollider = MeshCollider()
    meshCollider.meshFile = mesh
    meshCollider.isConvex = true // false disables rotation; I'm happy, that at least it moves at all :)

    val monkey = Entity("Monkey")
    monkey.add(MeshComponent(mesh))
    monkey.add(meshCollider)
    monkey.setPosition(0.0, 2.0, 0.0)
    monkey.transform.localRotation = Quaterniond().rotateX(0.1).rotateZ(0.1) // rotate a little to avoid symmetry
    val body = Rigidbody()
    body.mass = 1.0
    monkey.add(body)

    val floor = Entity("Floor")
    val s = 100.0
    floor.setPosition(0.0, -s, 0.0)
    floor.setScale(s)
    floor.add(BoxCollider())
    floor.add(MeshComponent(flatCube.front))
    floor.add(Rigidbody())

    val scene = Entity("Scene")
    Systems.registerSystem("bullet", BulletPhysics())
    scene.add(monkey)
    scene.add(floor)

    testSceneWithUI("Mesh Collider", scene)
}