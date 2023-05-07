package me.anno.ecs.components.physics

import org.joml.Vector3d

interface CustomBulletCollider {
    fun createBulletCollider(scale: Vector3d): Any
}