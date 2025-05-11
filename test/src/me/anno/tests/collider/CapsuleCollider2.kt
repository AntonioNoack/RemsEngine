package me.anno.tests.collider

import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.mesh.shapes.CapsuleModel.createCapsule

fun main() {
    // todo why is it getting stuck soo early??
    val collider = CapsuleCollider()
    val mesh = createCapsule(
        20, 10,
        collider.radius, collider.halfHeight
    )
    testCollider(collider, mesh.ref)
}
