package me.anno.engine.raycast

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.SQRT3
import me.anno.maths.Maths.hasFlag
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles.computeConeInterpolation
import me.anno.utils.types.Triangles.rayTriangleIntersection
import org.joml.*
import kotlin.math.abs

object Raycast {

    val TRIANGLE_FRONT = 1
    val TRIANGLE_BACK = 2
    val TRIANGLES = TRIANGLE_FRONT or TRIANGLE_BACK // 3
    val COLLIDERS = 4
    val SDFS = 8

    // todo option for smoothed collision surfaces by their normal

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
        entity.validateMasks()
        entity.validateAABBs()
        entity.validateTransform()
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
        return if (result.distance < originalDistance) {
            result.geometryNormalWS.normalize()
            result
        } else null
    }

    fun raycastCollider(
        entity: Entity, collider: Collider,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        result: RayHit
    ): Boolean {

        val global = entity.transform.globalTransform // local -> global
        val inverse = global.invert(JomlPools.mat4x3d.create())

        // radius for the ray, like sphere-trace, e.g. for bullets + spread for the radius, so we can test cones
        // (e.g., for inaccurate checks like a large beam)
        // for that, just move towards the ray towards the origin of the collider by min(<radius>, <distance(ray, collider-origin)>)
        val radiusScale = inverse.getScaleLength() / SQRT3
        var testRadiusAtOrigin = (radiusAtOrigin * radiusScale).toFloat()
        var testRadiusPerUnit = radiusPerUnit.toFloat() // like an angle -> stays the same for regular scales
        val interpolation = if ((radiusAtOrigin > 0.0 || radiusPerUnit > 0.0) && collider.isConvex) {
            testRadiusAtOrigin = 0f
            testRadiusPerUnit = 0f
            1f - computeConeInterpolation(
                start, direction,
                global.m30, global.m31, global.m32,
                radiusAtOrigin, radiusPerUnit
            ).toFloat()
        } else 1f

        val tmp3f = result.tmpVector3fs
        val tmp3d = result.tmpVector3ds

        val globalStart = tmp3d[0].set(start)
        val globalDir = tmp3d[1].set(direction)

        val localStart = tmp3f[0].set(inverse.transformPosition(globalStart))
        if (interpolation < 1f) localStart.mul(interpolation)

        val localDir = tmp3f[1].set(inverse.transformDirection(globalDir))

        JomlPools.mat4x3d.sub(1)

        val maxDistance = (end.distance(start) * localDir.length() / direction.length()).toFloat()

        val localNormal = tmp3f[3]
        val localDistance = collider.raycast(
            localStart, localDir, testRadiusAtOrigin, testRadiusPerUnit,
            localNormal, maxDistance
        )
        // println("ld: $localDistance, md: $maxDistance, [$start,$direction] -> [$localStart,$localDir]")
        if (abs(localDistance) < maxDistance) {
            if (localDistance >= 0f || result.hitIfInside) {
                result.setFromLocal(
                    global, localStart, localDir, abs(localDistance), localNormal,
                    start, direction, end
                )
                return true
            }
        }
        return false

    }

    // val blasCache = CacheSection("BLAS")

    fun raycastTriangleMesh(
        transform: Transform?, mesh: Mesh,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        result: RayHit, typeMask: Int
    ): Boolean {

        val original = result.distance
        if (original <= 0.0) return false
        if (typeMask.and(TRIANGLES) == 0) return false

        val globalTransform = transform?.globalTransform
        // quick-path for easy meshes: no extra checks worth it
        if (mesh.numPrimitives <= 16) {
            globalRaycast2(
                result, globalTransform, mesh,
                start, direction,
                radiusAtOrigin, radiusPerUnit,
                typeMask
            )
        } else {

            // todo it would be great if we would/could project the start+end onto the global aabb,
            //  if they lay outside, so we can use the faster method more often

            // transform the ray into local mesh coordinates
            val inverse = result.tmpMat4x3d
            if (transform != null) {
                // local -> global
                globalTransform!!.invert(inverse)
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
                // todo doesn't work yet with BLAS
                /*val blas = blasCache.getFileEntry(mesh.ref, true, 5000, true) { _, _ ->
                    BVHBuilder.buildBLAS(mesh, SplitMethod.MIDDLE, 16)
                } as? BLASNode
                if (blas != null) {
                    val localMaxDistance = localSrt.distance(localEnd)
                    result.distance = localMaxDistance.toDouble()
                    if (blas.intersect(localSrt, localDir, result)) {
                        result.localToGlobal(globalTransform, start, direction, end)
                    }
                } else {*/
                localRaycast(
                    mesh, globalTransform, inverse, start, direction, end, localSrt, localDir, localEnd,
                    radiusAtOrigin, radiusPerUnit, result, typeMask, extraDistance, tmp0
                )
                // }
            } else {
                // mesh is scaled to zero on some axis, need to work in global coordinates
                // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates
                globalRaycast(
                    result, globalTransform, mesh,
                    start, direction,
                    radiusAtOrigin, radiusPerUnit,
                    typeMask
                )
            }
        }
        return eval(result, start, direction, end, original)
    }

    fun eval(result: RayHit, start: Vector3d, direction: Vector3d, end: Vector3d, original: Double): Boolean {
        if (result.distance < original) {
            direction.mulAdd(result.distance, start, end) // end = start + distance * dir, needed for colliders
            // LOGGER.info("Hit ${mesh.prefab!!.source.nameWithoutExtension.withLength(5)} @ ${result.distance.f3()} from ${original.f3()}")
        }
        return result.distance < original
    }

    fun localRaycast(
        mesh: Mesh, globalTransform: Matrix4x3d?, inverse: Matrix4x3d,
        start: Vector3d, direction: Vector3d, end: Vector3d,
        localSrt: Vector3f, localDir: Vector3f, localEnd: Vector3f,
        radiusAtOrigin: Double, radiusPerUnit: Double,
        result: RayHit, typeMask: Int,
        extraDistance: Double, tmp0: Array<Vector3f>,
    ) {

        val acceptFront = typeMask.hasFlag(TRIANGLE_FRONT)
        val acceptBack = typeMask.hasFlag(TRIANGLE_BACK)

        // LOGGER.info(Vector3f(localEnd).sub(localStart).normalize().dot(localDir))
        // for that test, extend the radius at the start & end or sth like that
        // calculate local radius & radius extend
        val radiusScale = inverse.getScaleLength() / SQRT3 // a guess
        val localRadiusAtOrigin = (radiusAtOrigin * radiusScale).toFloat()
        val localRadiusPerUnit = radiusPerUnit.toFloat()
        val localMaxDistance = localSrt.distance(localEnd)

        // test whether we intersect the aabb of this mesh
        if (mesh.getBounds()
                .testLine(localSrt, localDir, localRadiusAtOrigin, localRadiusPerUnit, localMaxDistance)
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
                    if (if (localNormalTmp.dot(localDir) < 0f) acceptFront else acceptBack) {
                        bestLocalDistance = localDistance
                        localHit.set(localHitTmp)
                        localNormal.set(localNormalTmp)
                    }
                }
            }

            if (bestLocalDistance < localMaxDistance2) {
                result.setFromLocal(globalTransform, localHit, localNormal, start, direction, end)
            }
        }
    }

    fun globalRaycast(
        result: RayHit, globalTransform: Matrix4x3d?, mesh: Mesh, start: Vector3d, direction: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double, typeMask: Int
    ) {
        if ((typeMask and TRIANGLES) == 0) return
        // first test whether the aabbs really overlap
        val globalAABB = result.tmpAABBd.set(mesh.aabb)
        if (globalTransform != null) globalAABB.transformAABB(globalTransform)
        if (globalAABB.testLine(start, direction, result.distance)) {
            globalRaycast2(
                result, globalTransform, mesh,
                start, direction,
                radiusAtOrigin, radiusPerUnit,
                typeMask
            )
        }
    }

    fun globalRaycast2(
        result: RayHit, globalTransform: Matrix4x3d?, mesh: Mesh, start: Vector3d, direction: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double, typeMask: Int
    ) {
        val acceptFront = typeMask.hasFlag(TRIANGLE_FRONT)
        val acceptBack = typeMask.hasFlag(TRIANGLE_BACK)
        if (!acceptFront && !acceptBack) return
        val tmp = result.tmpVector3ds
        mesh.forEachTriangle(tmp[2], tmp[3], tmp[4]) { a, b, c ->
            val tmpPos = tmp[0]
            val tmpNor = tmp[1]
            if (globalTransform != null) {
                globalTransform.transformPosition(a)
                globalTransform.transformPosition(b)
                globalTransform.transformPosition(c)
            }
            val maxDistance = result.distance
            val distance = rayTriangleIntersection(
                start, direction, a, b, c,
                radiusAtOrigin, radiusPerUnit,
                maxDistance, tmpPos, tmpNor
            )
            if (distance < result.distance) {
                if (if (tmpNor.dot(direction) < 0f) acceptFront else acceptBack) {
                    result.distance = distance
                    result.positionWS.set(tmpPos)
                    result.geometryNormalWS.set(tmpNor)
                }
            }
        }
    }

    fun globalRaycastByBones(
        result: RayHit, globalTransform: Matrix4x3d?,
        mesh: Mesh, start: Vector3d, direction: Vector3d,
        radiusAtOrigin: Double, radiusPerUnit: Double, typeMask: Int,
        matrices: Array<Matrix4x3f>
    ) {
        val acceptFront = typeMask.hasFlag(TRIANGLE_FRONT)
        val acceptBack = typeMask.hasFlag(TRIANGLE_BACK)
        if (!acceptFront && !acceptBack) return
        val tmpF = result.tmpVector3fs
        val tmpD = result.tmpVector3ds
        val positions = mesh.positions ?: return
        val weights = mesh.boneWeights
        val indices = mesh.boneIndices ?: return
        fun transformByBones(i: Int, j: Int): Vector3d {
            val tmp = tmpF[i]
            val i4 = j * 4
            if (weights == null) {
                tmp.set(positions, j * 3)
                // just use single index
                val index = indices[i4].toInt().and(255)
                if (index < matrices.size) {
                    val matrix = matrices[index]
                    matrix.transformPosition(tmp)
                }
            } else {
                val tmp2 = tmpF[i + 1].set(positions, j * 3)
                val tmp3 = tmpF[i + 2]
                tmp.set(0f) // sum
                var unitFactor = 1f
                // interpolate using weights
                for (k in 0 until 4) {
                    val index = indices[i4 + k].toInt().and(255)
                    val weight = weights[i4 + k]
                    if (weight != 0f && index < matrices.size) {
                        val matrix = matrices[index]
                        matrix.transformPosition(tmp2, tmp3) // tmp3 = matrix * tmp2
                        tmp3.mulAdd(weight, tmp, tmp) // tmp += tmp3 * weight
                        unitFactor -= weight
                    }
                }
                tmp2.mulAdd(unitFactor, tmp, tmp) // tmp += tmp2 * unitFactor
            }
            return tmpD[i + 2].set(tmp)
        }

        val tmpPos = tmpD[0]
        val tmpNor = tmpD[1]
        mesh.forEachTriangleIndex { ai, bi, ci ->

            val a = transformByBones(0, ai)
            val b = transformByBones(1, bi)
            val c = transformByBones(2, ci)

            if (globalTransform != null) {
                globalTransform.transformPosition(a)
                globalTransform.transformPosition(b)
                globalTransform.transformPosition(c)
            }

            val maxDistance = result.distance
            val distance = rayTriangleIntersection(
                start, direction, a, b, c,
                radiusAtOrigin, radiusPerUnit,
                maxDistance, tmpPos, tmpNor
            )
            if (distance < result.distance && if (tmpNor.dot(direction) < 0f) acceptFront else acceptBack) {
                result.distance = distance
                result.positionWS.set(tmpPos)
                result.geometryNormalWS.set(tmpNor)
            }
        }
    }

    private val StopIteration = Throwable()

    @Suppress("unused")
    fun raycastTriangleMesh(
        mesh: Mesh, start: Vector3f, dir: Vector3f, distance: Float,
        typeMask: Int
    ): Boolean {

        if (distance <= 0.0) return false
        val acceptFront = typeMask.hasFlag(TRIANGLE_FRONT)
        val acceptBack = typeMask.hasFlag(TRIANGLE_BACK)
        if (!acceptFront && !acceptBack) return false

        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        // test whether we intersect the aabb of this mesh
        if (mesh.getBounds().testLine(start, dir, distance)) {
            val localHitTmp = JomlPools.vec3f.create()
            val localNormalTmp = JomlPools.vec3f.create()
            val a = JomlPools.vec3f.create()
            val b = JomlPools.vec3f.create()
            val c = JomlPools.vec3f.create()
            try {
                mesh.forEachTriangle(a, b, c) { ai, bi, ci ->
                    // check collision of localStart-localEnd with triangle ABC
                    val localDistance = rayTriangleIntersection(
                        start, dir, ai, bi, ci,
                        distance, localHitTmp, localNormalTmp
                    )
                    if (localDistance < distance && if (localNormalTmp.dot(dir) < 0f) acceptFront else acceptBack)
                        throw StopIteration
                }
            } catch (e: Throwable) {
                if (e !== StopIteration) throw e
                return true
            } finally {
                JomlPools.vec3f.sub(5)
            }
        }
        return false
    }

}