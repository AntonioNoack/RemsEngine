package me.anno.tests.collider

import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.engine.DefaultAssets
import me.anno.utils.OS

fun main() {
    // todo why is this not rotating???
    val mesh = DefaultAssets.flatCube.ref
    val collider = BoxCollider()
    testCollider(collider, mesh)
}