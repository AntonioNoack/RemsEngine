package me.anno.engine.raycast

import me.anno.ecs.prefab.PrefabSaveable
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * requests for ray collisions have become quite complex, so here is a class to wrap them
 * */
class RayQuery(
    val start: Vector3d,
    val direction: Vector3f,
    val end: Vector3d,
    val radiusAtOrigin: Double,
    val radiusPerUnit: Double,
    var typeMask: Int,
    var collisionMask: Int,
    var includeDisabled: Boolean,
    var ignored: Set<PrefabSaveable>,
    val result: RayHit,
) {

    constructor(
        start: Vector3d, direction: Vector3f, maxDistance: Double, radiusAtOrigin: Double, radiusPerUnit: Double,
        typeMask: Int, collisionMask: Int, includeDisabled: Boolean, ignored: Set<PrefabSaveable>
    ) : this(
        start, direction, Vector3d(direction).mul(maxDistance).add(start), radiusAtOrigin,
        radiusPerUnit, typeMask, collisionMask, includeDisabled, ignored, RayHit(maxDistance)
    )

    constructor(
        start: Vector3d, direction: Vector3f, maxDistance: Double,
        typeMask: Int, collisionMask: Int, includeDisabled: Boolean, ignored: Set<PrefabSaveable>
    ) : this(
        start, direction, maxDistance, 0.0, 0.0,
        typeMask, collisionMask, includeDisabled, ignored
    )

    constructor(
        start: Vector3d, direction: Vector3f, maxDistance: Double,
        typeMask: Int, collisionMask: Int
    ) : this(
        start, direction, maxDistance, 0.0, 0.0,
        typeMask, collisionMask, false, emptySet()
    )

    constructor(start: Vector3d, direction: Vector3f, maxDistance: Double) :
            this(start, direction, maxDistance, -1, -1)

    val local = RayQueryLocal(
        Vector3f(),
        Vector3f(),
        0f,
        radiusAtOrigin.toFloat(),
        radiusPerUnit.toFloat(),
        result.hitType
    )
}