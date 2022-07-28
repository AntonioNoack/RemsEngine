package me.anno.tests.collider

import me.anno.Engine
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.collider.MeshCollider
import me.anno.engine.ECSRegistry
import me.anno.utils.OS
import org.joml.Vector3d

fun main() {
    ECSRegistry.init()
    val mesh = MeshCache[OS.documents.getChild("redMonkey.glb")]!!
    val collider = MeshCollider()
    collider.mesh = mesh
    collider.isConvex = false
    collider.createBulletShape(Vector3d(1.0))
    Engine.requestShutdown()
}