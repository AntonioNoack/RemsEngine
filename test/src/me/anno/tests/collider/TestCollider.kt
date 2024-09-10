package me.anno.tests.collider

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference
import me.anno.mesh.Shapes.flatCube

fun testCollider(colliderImpl: Collider, mesh: FileReference) {

    OfficialExtensions.initForTests()
    Systems.registerSystem(BulletPhysics())

    val scene = Entity("Scene")
    Entity("Tested", scene)
        .add(MeshComponent(mesh))
        .add(colliderImpl)
        .setPosition(0.0, 3.0, 0.0)
        .setRotation(0.3, 0.0, 0.1) // rotate a little to avoid symmetry
        .add(Rigidbody().apply { mass = 1.0 })

    val s = 10.0
    Entity("Floor", scene)
        .setPosition(0.0, -s, 0.0)
        .setScale(s)
        .add(BoxCollider())
        .add(MeshComponent(flatCube.front))
        .add(Rigidbody())

    testSceneWithUI("Collider Test", scene)
}