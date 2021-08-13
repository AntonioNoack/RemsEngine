package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.utils.types.AABBs.reset
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.AABBs.testLineAABB
import me.anno.utils.types.AABBs.transformAABB
import me.anno.utils.types.Triangles.rayTriangleIntersection
import me.anno.utils.types.Vectors.toVector3f
import org.joml.*

object Raycast {

    enum class TypeMask {
        TRIANGLES,
        COLLIDERS,
        BOTH
    }

    // todo radius for the ray, like sphere-trace, e.g. for bullets

    // todo function for raycast collider (faster but less accurate for meshes)

    /**
     * returns whether something was hit,
     * more information is saved in the result
     * todo this function doesn't seem to be working for large distances, and idk why...
     * */
    fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        maxLength: Double,
        typeMask: TypeMask,
        collisionMask: Int = -1,
        includeDisabled: Boolean = false,
        tmpAABB: AABBd = AABBd(),
        result: RayHit = RayHit()
    ): RayHit? {
        if (maxLength <= 0) return null
        result.distance = maxLength
        direction.normalize()
        val end = Vector3d(direction).mul(maxLength).add(start)
        return raycastTriangles(
            entity, start, direction, end,
            typeMask, collisionMask,
            includeDisabled, tmpAABB, result
        )
    }

    /**
     * finds the minimum distance triangle
     * returns whether something was hit
     * */
    fun raycastTriangles(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        typeMask: TypeMask,
        collisionMask: Int = -1,
        includeDisabled: Boolean = false,
        tmpAABB: AABBd,
        result: RayHit = RayHit()
    ): RayHit? {
        if (result.distance <= 0) return null
        val startDistance = result.distance
        val components = entity.components
        val triangles = typeMask != TypeMask.COLLIDERS
        val colliders = typeMask != TypeMask.TRIANGLES
        for (i in components.indices) {
            val component = components[i]
            if (includeDisabled || component.isEnabled) {
                if (triangles && component is MeshComponent) {
                    val mesh = component.mesh ?: continue
                    if (component.canCollide(collisionMask)) {
                        if (raycastTriangleMesh(entity, mesh, start, direction, end, Matrix4x3d(), result)) {
                            result.meshComponent = component
                        }
                    }
                }
                if (colliders && component is Collider) {
                    if (component.canCollide(collisionMask)) {
                        if (raycastCollider(entity, component, start, direction, end, Matrix4x3d(), result)) {
                            result.collider = component
                        }
                    }
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if ((includeDisabled || child.isEnabled) && child.canCollide(collisionMask)) {
                if (testLineAABB(child.aabb, start, end)) {
                    raycastTriangles(
                        child, start, direction, end, typeMask,
                        collisionMask, includeDisabled, tmpAABB, result
                    )
                }
            }
        }
        return if (result.distance < startDistance) result else null
    }

    fun raycastCollider(
        entity: Entity, collider: Collider,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        inverse: Matrix4x3d, result: RayHit
    ): Boolean {

        val globalTransform = entity.transform.globalTransform // local -> global
        inverse.set(globalTransform).invert()

        val localStart = inverse.transformPosition(Vector3d(start)).toVector3f()
        val localDir = inverse.transformDirection(Vector3d(direction)).toVector3f().normalize()
        val localEnd = inverse.transformPosition(Vector3d(end)).toVector3f()

        val maxDistance = localStart.distance(localEnd)

        val localNormal = Vector3f()
        val localDistance = collider.raycast(localStart, localDir, localNormal, maxDistance)
        // result is ignored, if we already are inside that collider
        // todo this behaviour probably should be customizable
        return if (localDistance > 0f && localDistance < maxDistance) {
            result.setFromLocal(
                globalTransform,
                localStart, localDir, localDistance, localNormal,
                start, direction, end
            )
            true
        } else false

    }

    fun raycastTriangleMeshGlobal(
        entity: Entity, mesh: Mesh,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        inverse: Matrix4x3d, result: RayHit,
    ): Boolean {

        var hasHitTriangle = false

        // todo it would be great if we would/could project the start+end onto the global aabb,
        // todo if they lay outside, so more often we can use the faster method more often

        // transform the ray into local mesh coordinates
        val globalTransform = entity.transform.globalTransform // local -> global
        inverse.set(globalTransform).invert()

        // todo if it is animated, we should ignore the aabb, and must apply the appropriate bone transforms
        // mesh is scaled to zero on some axis, need to work in global coordinates
        // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates

        // first test whether the aabbs really overlap
        val globalAABB = result.tmpAABBd.set(mesh.aabb)
        transformAABB(globalAABB, globalTransform)

        if (testLineAABB(globalAABB, start, end)) {

            val tmp = result.tmpVector3ds
            val tmpNormal = tmp[0]
            val tmpPosition = tmp[1]
            mesh.forEachTriangle(tmp[2], tmp[3], tmp[4]) { a, b, c ->
                globalTransform.transformPosition(a)
                globalTransform.transformPosition(b)
                globalTransform.transformPosition(c)
                val distance = rayTriangleIntersection(
                    start, direction, a, b, c, result.distance, tmpNormal, tmpPosition
                )
                if (distance < result.distance) {
                    result.distance = distance
                    hasHitTriangle = true
                    result.positionWS.set(tmpPosition)
                    result.normalWS.set(tmpNormal)
                }
            }

        }

        return hasHitTriangle

    }

    fun raycastTriangleMesh(
        entity: Entity, mesh: Mesh,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        inverse: Matrix4x3d, result: RayHit,
    ): Boolean {

        var hasHitTriangle = false

        // todo it would be great if we would/could project the start+end onto the global aabb,
        // todo if they lay outside, so more often we can use the faster method more often

        // transform the ray into local mesh coordinates
        val globalTransform = entity.transform.globalTransform // local -> global
        inverse.set(globalTransform).invert()

        val tmp0 = result.tmpVector3fs
        val tmp1 = result.tmpVector3ds

        val localSrt = tmp0[0].set(inverse.transformPosition(tmp1[0].set(start)))
        val localEnd = tmp0[1].set(inverse.transformPosition(tmp1[2].set(end)))
        val localDir = tmp0[2].set(inverse.transformDirection(tmp1[1].set(direction))).normalize()

        // if any coordinates of start or end are invalid, work in global coordinates
        val hasValidCoordinates =
            localSrt.x.isFinite() && localSrt.y.isFinite() && localSrt.z.isFinite() &&
                    localDir.x.isFinite() && localDir.y.isFinite() && localDir.z.isFinite()

        // the fast method only works, if the numbers have roughly the same order of magnitude
        val relativePositionsSquared = (localSrt.lengthSquared() + 1e-7f) / (localEnd.lengthSquared() + 1e-7f)
        val orderOfMagnitudeIsFine = relativePositionsSquared in 0.001f..1000f

        // todo if it is animated, we should ignore the aabb, and must apply the appropriate bone transforms
        if (hasValidCoordinates && orderOfMagnitudeIsFine) {

            // println(Vector3f(localEnd).sub(localStart).normalize().dot(localDir))

            // test whether we intersect the aabb of this mesh
            if (testLineAABB(mesh.aabb, localSrt, localEnd)) {

                // test whether we intersect any triangle of this mesh
                var maxDistance = localSrt.distance(localEnd)
                val tmpPosition = tmp0[3]
                val tmpNormal = tmp0[4]
                val localPosition = tmp0[5]
                val localNormal = tmp0[6]
                mesh.forEachTriangle(tmp0[7], tmp0[8], tmp0[9]) { a, b, c ->
                    // check collision of localStart-localEnd with triangle a,b,c
                    val localDistance = rayTriangleIntersection(
                        localSrt, localDir, a, b, c, maxDistance, tmpNormal, tmpPosition
                    )
                    if (localDistance < maxDistance) {
                        maxDistance = localDistance
                        hasHitTriangle = true
                        localPosition.set(tmpPosition)
                        localNormal.set(tmpNormal)
                    }
                }

                if (hasHitTriangle) {
                    result.setFromLocal(globalTransform, localPosition, localNormal, start, direction, end)
                }

            }

        } else {

            // mesh is scaled to zero on some axis, need to work in global coordinates
            // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates

            // first test whether the aabbs really overlap
            val globalAABB = result.tmpAABBd.set(mesh.aabb)
            transformAABB(globalAABB, globalTransform)

            if (testLineAABB(globalAABB, start, end)) {

                val tmp = result.tmpVector3ds
                val tmpNormal = tmp[0]
                val tmpPosition = tmp[1]
                mesh.forEachTriangle(tmp[2], tmp[3], tmp[4]) { a, b, c ->
                    globalTransform.transformPosition(a)
                    globalTransform.transformPosition(b)
                    globalTransform.transformPosition(c)
                    val distance = rayTriangleIntersection(
                        start, direction, a, b, c, result.distance, tmpNormal, tmpPosition
                    )
                    if (distance < result.distance) {
                        result.distance = distance
                        hasHitTriangle = true
                        result.positionWS.set(tmpPosition)
                        result.normalWS.set(tmpNormal)
                    }
                }

            }

        }

        return hasHitTriangle

    }

    @JvmStatic
    fun main(args: Array<String>) {

        val y = 8e26
        val z = 15e21

        val f = 1e-15

        val start = Vector3d(-1e150, y, z)
        val end = Vector3d(1e100, y, z)

        val aabb = AABBd()

        for (i in 0 until 1000) {

            val x = 1e31 * Math.random()
            println(x)

            aabb.reset()
            aabb.union(x * (1 - f), y * (1 - f), z * (1 - f))
            aabb.union(x * (1 + f), y * (1 + f), z * (1 + f))

            val result = testLineAABB(aabb, start, end)
            if (!result) throw RuntimeException()

        }

    }

}