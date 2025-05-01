package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.maths.Maths
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles
import kotlin.math.abs

object RaycastCollider {
    fun raycastGlobalCollider(query: RayQuery, entity: Entity, collider: Collider): Boolean {

        val localToGlobal = entity.transform.globalTransform
        val globalToLocal = localToGlobal.invert(JomlPools.mat4x3m.create())

        // radius for the ray, like sphere-trace, e.g. for bullets + spread for the radius, so we can test cones
        // (e.g., for inaccurate checks like a large beam)
        // for that, just move towards the ray towards the origin of the collider by min(<radius>, <distance(ray, collider-origin)>)
        val radiusScale = globalToLocal.getScaleLength() / Maths.SQRT3
        var localRadiusAtOrigin = (query.radiusAtOrigin * radiusScale).toFloat()
        var localRadiusPerUnit = query.radiusPerUnit.toFloat() // like an angle -> stays the same for regular scales
        val interpolation = if ((query.radiusAtOrigin > 0.0 || query.radiusPerUnit > 0.0) && collider.isConvex) {
            localRadiusAtOrigin = 0f
            localRadiusPerUnit = 0f
            Triangles.computeConeInterpolation(
                query.start, query.direction,
                localToGlobal.m30, localToGlobal.m31, localToGlobal.m32,
                query.radiusAtOrigin, query.radiusPerUnit
            ).toFloat()
        } else 0f

        val result = query.result
        val tmp3f = result.tmpVector3fs
        val tmp3d = result.tmpVector3ds

        val local = query.local

        val localStart0 = globalToLocal.transformPosition(query.start, tmp3d[0])
        val localStart = local.start.set(localStart0)
        if (interpolation > 0f) localStart.mul(1f - interpolation)

        val localDir0 = globalToLocal.transformDirection(tmp3f[1].set(query.direction))
        val localDir = local.direction.set(localDir0)

        JomlPools.mat4x3m.sub(1)

        val scale = localDir.length()
        val maxDistance = (query.result.distance * scale).toFloat()
        local.radiusAtOrigin = localRadiusAtOrigin
        local.radiusPerUnit = localRadiusPerUnit
        local.maxDistance = maxDistance
        local.hitType = result.hitType

        localDir.safeNormalize()

        val localNormal = tmp3f[2]
        val localDistance = collider.raycast(local, localNormal)

        // println("ld: $localDistance, md: $maxDistance, [$start,$direction] -> [$localStart,$localDir]")
        if (localDistance >= 0f && localDistance < maxDistance) {
            query.result.setFromLocal(localToGlobal, localStart, localDir, abs(localDistance), localNormal, query)
            return true
        }
        return false
    }
}