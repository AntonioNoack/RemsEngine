package me.anno.objects.particles

import org.joml.Vector3f

// could be encapsulated into joint vectors for probably better performance
data class ParticleState(
    val position: Vector3f,
    val dPosition: Vector3f,
    val rotation: Vector3f,
    val dRotation: Vector3f
)