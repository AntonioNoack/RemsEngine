package me.anno.ecs.components.physics

import org.joml.Vector3f

interface CustomBulletCollider {
    fun createBulletCollider(scale: Vector3f): Any
}