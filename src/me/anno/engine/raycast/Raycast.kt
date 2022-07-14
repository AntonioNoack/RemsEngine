package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.SQRT3
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Matrices.getScaleLength
import me.anno.utils.types.Triangles.computeConeInterpolation
import me.anno.utils.types.Triangles.rayTriangleIntersection
import me.anno.utils.types.Vectors.print
import me.anno.utils.types.Vectors.toVector3f
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Math
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.sqrt

object Raycast {

    // todo flag for mesh-backsides: ignore or respect

    val TRIANGLES = 1
    val COLLIDERS = 2
    val SDFS = 4

    /**
     * returns whether something was hit,
     * more information is saved in the result
     * todo this function doesn't seem to be working for large distances, and idk why...
     * */
    fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        maxDistance: Double,
        typeMask: Int,
        collisionMask: Int = -1,
        ignored: Set<PrefabSaveable> = emptySet(),
        includeDisabled: Boolean = false,
        result: RayHit = RayHit()
    ): RayHit? {
        if (maxDistance <= 0.0) return null
        result.distance = maxDistance
        direction.normalize()
        val end = JomlPools.vec3d.create()
            .set(direction).mul(maxDistance).add(start)
        val hit = raycast(
            entity, start, direction, end,
            radiusAtOrigin, radiusPerUnit,
            typeMask, collisionMask, ignored,
            includeDisabled, result
        )
        JomlPools.vec3d.sub(1)
        return hit
    }

    /**
     * finds the minimum distance triangle;
     * returns whether something was hit
     * */
    fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        typeMask: Int,
        collisionMask: Int = -1,
        ignored: Set<PrefabSaveable> = emptySet(),
        includeDisabled: Boolean = false,
        result: RayHit = RayHit()
    ): RayHit? {
        if (result.distance <= 0.0) return null
        val originalDistance = result.distance
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if ((includeDisabled || component.isEnabled) && component !in ignored) {
                if (component is CollidingComponent &&
                    component.hasRaycastType(typeMask) &&
                    component.canCollide(collisionMask)
                ) {
                    if (component.raycast(
                            entity, start, direction, end, radiusAtOrigin, radiusPerUnit, typeMask,
                            includeDisabled, result
                        ) && result.distance <= 0.0
                    ) return result
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if ((includeDisabled || child.isEnabled) && child.canCollide(collisionMask) && child !in ignored) {
                if (child.aabb.testLine(start, direction, result.distance)) {
                    if (raycast(
                            child, start, direction, end,
                            radiusAtOrigin, radiusPerUnit,
                            typeMask, collisionMask, ignored,
                            includeDisabled, result
                        ) != null && result.distance <= 0.0
                    ) return result
                }
            }
        }
        return if (result.distance < originalDistance) result else null
    }

    fun raycastCollider(
        entity: Entity, collider: Collider,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        result: RayHit
    ): Boolean {

        val global = entity.transform.globalTransform // local -> global
        val inverse = JomlPools.mat4x3d.create().set(global).invert()

        // radius for the ray, like sphere-trace, e.g. for bullets + spread for the radius, so we can test cones
        // (e.g. for inaccurate checks like a large beam)
        // for that, just move towards the ray towards the origin of the collider by min(<radius>, <distance(ray, collider-origin)>)
        val radiusScale = inverse.getScaleLength() / SQRT3
        var testRadiusAtOrigin = (radiusAtOrigin * radiusScale).toFloat()
        var testRadiusPerUnit = radiusPerUnit.toFloat() // like an angle -> stays the same for regular scales
        val interpolation = if (collider.isConvex) {
            testRadiusAtOrigin = 0f
            testRadiusPerUnit = 0f
            1f - computeConeInterpolation(
                start, direction,
                global.m30(), global.m31(), global.m32(),
                radiusAtOrigin, radiusPerUnit
            ).toFloat()
        } else 1f

        val tmp3f = result.tmpVector3fs
        val tmp3d = result.tmpVector3ds

        val globalStart = tmp3d[0].set(start)
        val globalDir = tmp3d[1].set(direction)

        val localStart = inverse.transformPosition(globalStart).toVector3f(tmp3f[0])
        if (interpolation < 1f) localStart.mul(interpolation)

        val localDir = inverse.transformDirection(globalDir).toVector3f(tmp3f[1])

        JomlPools.mat4x3d.sub(1)

        val maxDistance = sqrt(localDir.lengthSquared() / direction.lengthSquared()).toFloat()

        val localNormal = tmp3f[3]
        val localDistance = collider.raycast(
            localStart, localDir, testRadiusAtOrigin, testRadiusPerUnit,
            localNormal, maxDistance
        )
        if (localDistance < maxDistance) {
            if (localDistance >= 0f || result.hitIfInside) {
                result.setFromLocal(
                    global,
                    localStart, localDir, localDistance, localNormal,
                    start, direction, end
                )
                return true
            }
        }
        return false

    }

    /*fun raycastTriangleMeshGlobal(
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
                    start, direction, a, b, c, result.distance, tmpPosition, tmpNormal
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

    }*/

    fun raycastTriangleMesh(
        entity: Entity?, mesh: Mesh, start: Vector3d,
        direction: Vector3d, end: Vector3d, radiusAtOrigin: Double,
        radiusPerUnit: Double, result: RayHit,
    ): Boolean {

        val original = result.distance
        if (original <= 0.0) return false

        // calculate bounds
        mesh.ensureBuffer()

        // todo it would be great if we would/could project the start+end onto the global aabb,
        // todo if they lay outside, so more often we can use the faster method more often

        // transform the ray into local mesh coordinates
        val inverse = JomlPools.mat4x3d.create()
        val globalTransform = JomlPools.mat4x3d.create()
        if (entity != null) {
            globalTransform.set(entity.transform.globalTransform) // local -> global
            inverse.set(globalTransform).invert()
        } else inverse.identity()

        val tmp0 = result.tmpVector3fs
        val tmp1 = result.tmpVector3ds

        val localSrt0 = inverse.transformPosition(tmp1[0].set(start))
        val localEnd0 = inverse.transformPosition(tmp1[1].set(end))
        val localDir0 = tmp1[2].set(tmp1[1]).sub(tmp1[0]).normalize()

        // todo reprojection doesn't work yet correctly (test: monkey ears)
        // project points onto front/back of box
        val extraDistance = 0.0 // projectRayToAABBFront(localSrt0, localDir0, mesh.aabb, dst = localSrt0)
        // projectRayToAABBBack(localEnd0, localDir0, mesh.aabb, dst = localEnd0)

        val localSrt = tmp0[0].set(localSrt0)
        val localEnd = tmp0[1].set(localEnd0)
        val localDir = tmp0[2].set(localDir0)

        // if any coordinates of start or end are invalid, work in global coordinates
        val hasValidCoordinates = localSrt.isFinite && localDir.isFinite

        // the fast method only works, if the numbers have roughly the same order of magnitude
        val relativePositionsSquared = localSrt.lengthSquared() / localEnd.lengthSquared()
        val orderOfMagnitudeIsFine = relativePositionsSquared in 1e-6f..1e6f

        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        if (hasValidCoordinates && orderOfMagnitudeIsFine) {

            // LOGGER.info(Vector3f(localEnd).sub(localStart).normalize().dot(localDir))
            // for that test, extend the radius at the start & end or sth like that
            // calculate local radius & radius extend
            val radiusScale = inverse.getScaleLength() / SQRT3 // a guess
            val localRadiusAtOrigin = (radiusAtOrigin * radiusScale).toFloat()
            val localRadiusPerUnit = radiusPerUnit.toFloat()
            val localMaxDistance = localSrt.distance(localEnd)

            // test whether we intersect the aabb of this mesh
            if (mesh.aabb.testLine(
                    localSrt,
                    localDir,
                    localRadiusAtOrigin,
                    localRadiusPerUnit,
                    localMaxDistance
                )
            ) {

                // test whether we intersect any triangle of this mesh
                val localMaxDistance2 = localMaxDistance + extraDistance
                var bestLocalDistance = localMaxDistance
                val localHitTmp = tmp0[3]
                val localNormalTmp = tmp0[4]
                val localHit = tmp0[5]
                val localNormal = tmp0[6]

                mesh.forEachTriangle(tmp0[7], tmp0[8], tmp0[9]) { a, b, c ->
                    // check collision of localStart-localEnd with triangle a,b,c
                    val localDistance = rayTriangleIntersection(
                        localSrt, localDir, a, b, c,
                        localRadiusAtOrigin, localRadiusPerUnit,
                        bestLocalDistance, localHitTmp, localNormalTmp
                    )
                    if (localDistance < bestLocalDistance) {
                        bestLocalDistance = localDistance
                        localHit.set(localHitTmp)
                        localNormal.set(localNormalTmp)
                    }
                }

                if (bestLocalDistance < localMaxDistance2) {
                    result.setFromLocal(globalTransform, localHit, localNormal, start, direction, end)
                }

            }

        } else {

            // mesh is scaled to zero on some axis, need to work in global coordinates
            // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates

            // first test whether the aabbs really overlap
            val globalAABB = result.tmpAABBd.set(mesh.aabb)
            globalAABB.transformAABB(globalTransform)

            if (globalAABB.testLine(start, direction, result.distance)) {

                val tmp = result.tmpVector3ds
                mesh.forEachTriangle(tmp[2], tmp[3], tmp[4]) { a, b, c ->
                    val tmpPos = tmp[0]
                    val tmpNor = tmp[1]
                    globalTransform.transformPosition(a)
                    globalTransform.transformPosition(b)
                    globalTransform.transformPosition(c)
                    val maxDistance = result.distance
                    val distance = rayTriangleIntersection(
                        start, direction, a, b, c,
                        radiusAtOrigin, radiusPerUnit,
                        maxDistance, tmpPos, tmpNor
                    )
                    if (distance < result.distance) {
                        result.distance = distance
                        result.positionWS.set(tmpPos)
                        result.normalWS.set(tmpNor)
                    }
                }
            }
        }

        JomlPools.mat4x3d.sub(2)

        if (result.distance < original) {
            direction.mulAdd(result.distance, start, end) // end = start + distance * dir, needed for colliders
            // LOGGER.info("Hit ${mesh.prefab!!.source.nameWithoutExtension.withLength(5)} @ ${result.distance.f3()} from ${original.f3()}")
        }

        return result.distance < original

    }

    private val ex = RuntimeException()

    @Suppress("unused")
    fun raycastTriangleMesh(mesh: Mesh, start: Vector3f, dir: Vector3f, distance: Float): Boolean {
        if (distance <= 0.0) return false
        mesh.ensureBuffer()
        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        // test whether we intersect the aabb of this mesh
        if (mesh.aabb.testLine(start, dir, distance)) {
            val localHitTmp = JomlPools.vec3f.create()
            val localNormalTmp = JomlPools.vec3f.create()
            val a = JomlPools.vec3f.create()
            val b = JomlPools.vec3f.create()
            val c = JomlPools.vec3f.create()
            try {
                mesh.forEachTriangle(a, b, c) { ai, bi, ci ->
                    // check collision of localStart-localEnd with triangle a,b,c
                    val localDistance = rayTriangleIntersection(
                        start, dir, ai, bi, ci,
                        distance, localHitTmp, localNormalTmp
                    )
                    if (localDistance < distance) {
                        throw ex
                    }
                }
            } catch (e: RuntimeException) {
                if (e !== ex) throw e
                return true
            } finally {
                JomlPools.vec3f.sub(5)
            }
        }
        return false
    }

    @JvmStatic
    fun main(args: Array<String>) {
        simpleTest()
        precisionTest()
    }

    fun simpleTest() {

        val y = 1.0
        val z = 1.0

        val f = 1e-3

        val start = Vector3d(-1e3, y, z)
        val dir = Vector3d(1.0, 0.0, 0.0)

        val aabb = AABBd()


        for (i in 0 until 1000) {

            val x = Math.random()

            aabb.clear()
            aabb.union(x * (1 - f), y * (1 - f), z * (1 - f))
            aabb.union(x * (1 + f), y * (1 + f), z * (1 + f))

            val result = aabb.testLine(start, dir, 2e3)
            if (!result) throw RuntimeException("${start.print()} + t * ${dir.print()} does not intersect ${aabb.print()}")

        }

        LogManager.getLogger("Raycast")
            .info("Finished simple test")

    }

    fun precisionTest() {

        val y = 1.0
        val z = 1.0

        val f = 0.1

        val start = Vector3d(-1e20, y, z)
        val dir = Vector3d(1.0, 0.0, 0.0)

        val aabb = AABBd()

        for (i in 0 until 1000) {

            val x = Math.random()

            aabb.clear()
            aabb.union(x * (1 - f), y * (1 - f), z * (1 - f))
            aabb.union(x * (1 + f), y * (1 + f), z * (1 + f))

            val result = aabb.testLine(start, dir, 2e20)
            if (!result) throw RuntimeException("${start.print()} + t * ${dir.print()} does not intersect ${aabb.print()}")

        }

        LogManager.getLogger("Raycast")
            .info("Finished precision test")

    }

}