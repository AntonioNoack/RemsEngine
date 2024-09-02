package me.anno.tests.collider

import me.anno.ecs.components.collider.SphereCollider
import me.anno.engine.DefaultAssets

fun main() {
    val mesh = DefaultAssets.icoSphere.ref
    val collider = SphereCollider()
    testCollider(collider, mesh)
}