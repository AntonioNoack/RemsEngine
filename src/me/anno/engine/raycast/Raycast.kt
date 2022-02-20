package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.components.CollidingComponent
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.maths.Maths.SQRT3
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.AABBs.print
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.AABBs.testLineAABB
import me.anno.utils.types.AABBs.transformAABB
import me.anno.utils.types.Matrices.getScaleLength
import me.anno.utils.types.Triangles.computeConeInterpolation
import me.anno.utils.types.Triangles.rayTriangleIntersection
import me.anno.utils.types.Vectors.print
import me.anno.utils.types.Vectors.toVector3f
import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.Math
import org.joml.Vector3d
import kotlin.math.sqrt

object Raycast {

    // todo flag for mesh-backsides: ignore or respect

    enum class TypeMask {
        TRIANGLES,
        COLLIDERS,
        BOTH
    }

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
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        maxLength: Double,
        typeMask: TypeMask,
        collisionMask: Int = -1,
        includeDisabled: Boolean = false,
        result: RayHit = RayHit()
    ): RayHit? {
        if (maxLength <= 0) return null
        result.distance = maxLength
        direction.normalize()
        val end = Vector3d(direction).mul(maxLength).add(start)
        return raycastTriangles(
            entity, start, direction, end,
            radiusAtOrigin, radiusPerUnit,
            typeMask, collisionMask,
            includeDisabled, result
        )
    }

    /**
     * finds the minimum distance triangle;
     * returns whether something was hit
     * */
    fun raycastTriangles(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        typeMask: TypeMask,
        collisionMask: Int = -1,
        includeDisabled: Boolean = false,
        result: RayHit = RayHit()
    ): RayHit? {
        if (result.distance <= 0) return null
        val originalDistance = result.distance
        val components = entity.components
        val triangles = typeMask != TypeMask.COLLIDERS
        val colliders = typeMask != TypeMask.TRIANGLES
        for (i in components.indices) {
            val component = components[i]
            if (includeDisabled || component.isEnabled) {
                if (component is CollidingComponent && component.canCollide(collisionMask)) {
                    if (triangles && component is MeshBaseComponent) {
                        val mesh = component.getMesh()
                        if (mesh != null && raycastTriangleMesh(
                                entity, mesh, start, direction, end,
                                radiusAtOrigin, radiusPerUnit, result
                            )
                        ) {
                            result.mesh = mesh
                            result.component = component
                        }
                    }
                    if (colliders && component is Collider) {
                        if (raycastCollider(
                                entity, component, start, direction, end,
                                radiusAtOrigin, radiusPerUnit, result
                            )
                        ) {
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
                if (testLineAABB(child.aabb, start, direction, result.distance)) {
                    raycastTriangles(
                        child, start, direction, end,
                        radiusAtOrigin, radiusPerUnit,
                        typeMask, collisionMask, includeDisabled, result
                    )
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
        entity: Entity?, mesh: Mesh,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        result: RayHit,
    ): Boolean {

        val original = result.distance

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

        val localSrt = tmp0[0].set(inverse.transformPosition(tmp1[0].set(start)))
        val localEnd = tmp0[1].set(inverse.transformPosition(tmp1[1].set(end)))
        val localDir = tmp0[2].set(localEnd).sub(localSrt).normalize()

        // if any coordinates of start or end are invalid, work in global coordinates
        val hasValidCoordinates =
            localSrt.x.isFinite() && localSrt.y.isFinite() && localSrt.z.isFinite() &&
                    localDir.x.isFinite() && localDir.y.isFinite() && localDir.z.isFinite()

        // the fast method only works, if the numbers have roughly the same order of magnitude
        val relativePositionsSquared = (localSrt.lengthSquared() + 1e-7f) / (localEnd.lengthSquared() + 1e-7f)
        val orderOfMagnitudeIsFine = relativePositionsSquared in 1e-3f..1e3f

        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        if (hasValidCoordinates && orderOfMagnitudeIsFine) {

            // LOGGER.info(Vector3f(localEnd).sub(localStart).normalize().dot(localDir))
            // for that test, extend the radius at the start & end or sth like that
            // calculate local radius & radius extend
            val radiusScale = inverse.getScaleLength() / SQRT3 // a guess
            val localRadiusAtOrigin = (radiusAtOrigin * radiusScale).toFloat()
            val localRadiusPerUnit = radiusPerUnit.toFloat()

            // test whether we intersect the aabb of this mesh
            if (testLineAABB(mesh.aabb, localSrt, localEnd, localRadiusAtOrigin, localRadiusPerUnit)) {

                // test whether we intersect any triangle of this mesh
                var localEnd2 = localSrt.distance(localEnd)
                val originalLocalEnd = localEnd2
                val tmpPos = tmp0[3]
                val tmpNor = tmp0[4]
                val localPosition = tmp0[5]
                val localNormal = tmp0[6]

                mesh.forEachTriangle(tmp0[7], tmp0[8], tmp0[9]) { a, b, c ->
                    // check collision of localStart-localEnd with triangle a,b,c
                    val localDistance = rayTriangleIntersection(
                        localSrt, localDir, a, b, c,
                        localRadiusAtOrigin, localRadiusPerUnit,
                        localEnd2, tmpPos, tmpNor
                    )
                    if (localDistance < localEnd2) {
                        localEnd2 = localDistance
                        localPosition.set(tmpPos)
                        localNormal.set(tmpNor)
                    }
                }

                if (localEnd2 < originalLocalEnd) {
                    result.setFromLocal(globalTransform, localPosition, localNormal, start, direction, end)
                }

            }

        } else {

            // mesh is scaled to zero on some axis, need to work in global coordinates
            // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates

            // first test whether the aabbs really overlap
            val globalAABB = result.tmpAABBd.set(mesh.aabb)
            transformAABB(globalAABB, globalTransform)

            if (testLineAABB(globalAABB, start, direction, result.distance)) {

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
            direction.mulAdd(result.distance, start, end) // end = start + distance * end, needed for colliders
            // LOGGER.info("Hit ${mesh.prefab!!.source.nameWithoutExtension.withLength(5)} @ ${result.distance.f3()} from ${original.f3()}")
        }

        return result.distance < original

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

            val result = testLineAABB(aabb, start, dir, 2e3)
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

            val result = testLineAABB(aabb, start, dir, 2e20)
            if (!result) throw RuntimeException("${start.print()} + t * ${dir.print()} does not intersect ${aabb.print()}")

        }

        LogManager.getLogger("Raycast")
            .info("Finished precision test")

    }

}