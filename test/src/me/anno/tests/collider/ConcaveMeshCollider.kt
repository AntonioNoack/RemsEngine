package me.anno.tests.collider

import me.anno.ecs.components.collider.MeshCollider
import me.anno.utils.OS

fun main() {
    val mesh = OS.documents.getChild("redMonkey.glb")
    val collider = MeshCollider(mesh)
    collider.isConvex = true // false disables rotation; I'm happy, that at least it moves at all :)
    testCollider(collider, mesh)
}