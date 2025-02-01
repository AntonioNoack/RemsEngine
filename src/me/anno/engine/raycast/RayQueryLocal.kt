package me.anno.engine.raycast

import me.anno.maths.bvh.HitType
import org.joml.Vector3f

class RayQueryLocal(
    val start: Vector3f,
    val direction: Vector3f,
    var maxDistance: Float,
    var radiusAtOrigin: Float,
    var radiusPerUnit: Float,
    var hitType: HitType
) {
    constructor(start: Vector3f, direction: Vector3f, maxDistance: Float, hitType: HitType) :
            this(start, direction, maxDistance, 0f, 0f, hitType)
}