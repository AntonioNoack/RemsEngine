package me.anno.tests.collider

import me.anno.bullet.BulletPhysics
import me.anno.bullet.Rigidbody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.DefaultAssets.plane
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference

fun testCollider(colliderImpl: Collider, mesh: FileReference, extraCode: ((Entity) -> Unit)? = null) {

    OfficialExtensions.initForTests()
    Systems.registerSystem(BulletPhysics())

    val scene = Entity("Scene")
    val body = Entity("Tested", scene)
        .add(MeshComponent(mesh))
        .add(colliderImpl)
        .setPosition(0.0, 3.0, 0.0)
        .setRotation(0.3f, 0f, 0.1f) // rotate a little to avoid symmetry
        .add(Rigidbody().apply { mass = 1.0 })
    extraCode?.invoke(body)

    Entity("Floor", scene)
        .add(InfinitePlaneCollider())
        .add(MeshComponent(plane))
        .add(Rigidbody().apply { mass = 0.0 })
        .setScale(10f)

    testSceneWithUI("Collider Test", scene)
}