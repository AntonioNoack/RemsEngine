package me.anno.engine.raycast

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.gpu.CullMode
import me.anno.maths.Maths
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3d
import org.joml.Vector3f

object RaycastMesh {

    private val LOGGER = LogManager.getLogger(RaycastMesh::class)

    fun raycastGlobalMeshClosestHit(query: RayQuery, transform: Transform?, mesh: Mesh): Boolean {

        val original = query.result.distance
        if (query.typeMask.and(Raycast.TRIANGLES) == 0) return false

        val globalTransform = transform?.globalTransform
        // quick-path for easy meshes: no extra checks worth it
        if (mesh.numPrimitives <= 16) {
            raycastGlobalClosestHit1(query, globalTransform, mesh)
        } else {

            // todo it would be great if we would/could project the start+end onto the global aabb,
            //  if they lay outside, so we can use the faster method more often

            // transform the ray into local mesh coordinates
            val result = query.result
            val inverse = result.tmpMat4x3d
            if (transform != null) {
                // local -> global
                globalTransform!!.invert(inverse)
            } else inverse.identity()

            val tmp0 = result.tmpVector3fs
            val tmp1 = result.tmpVector3ds

            val localSrt0 = inverse.transformPosition(query.start, tmp1[0])
            val localEnd0 = inverse.transformPosition(query.end, tmp1[1])
            val localDir0 = tmp1[2].set(tmp1[1]).sub(tmp1[0]).normalize()

            // todo reprojection doesn't work yet correctly (test: monkey ears)
            // project points onto front/back of box
            val extraDistance = 0.0 // projectRayToAABBFront(localSrt0, localDir0, mesh.aabb, dst = localSrt0)
            // projectRayToAABBBack(localEnd0, localDir0, mesh.aabb, dst = localEnd0)

            val localStart = tmp0[0].set(localSrt0)
            val localEnd = tmp0[1].set(localEnd0)
            val localDir = tmp0[2].set(localDir0)

            // if any coordinates of start or end are invalid, work in global coordinates
            val hasValidCoordinates = localStart.isFinite && localDir.isFinite

            // the fast method only works, if the numbers have roughly the same order of magnitude
            // val relativePositionsSquared = localStart.lengthSquared() / localEnd.lengthSquared()
            val orderOfMagnitudeIsFine = true // relativePositionsSquared in 1e-6f..1e6f

            if (hasValidCoordinates && orderOfMagnitudeIsFine && !mesh.hasBones) {
                val t0 = Time.nanoTime
                // todo executing a raycast is 20x cheaper than building a BLAS (30ms vs 600ms for dragon.obj),
                //  so only build it if truly necessary
                //  - async builder?
                //  - build only if demand is high?
                val blas = mesh.raycaster// ?: BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
                val t1 = Time.nanoTime
                if (t1 - t0 > MILLIS_TO_NANOS) {
                    LOGGER.warn(
                        "Took ${(t1 - t0) / 1e6f}ms for BLAS generation for ${mesh.ref}, " +
                                "${mesh.numPrimitives} primitives"
                    )
                }
                if (blas != null) {
                    mesh.raycaster = blas
                    val localMaxDistance = localStart.distance(localEnd)
                    result.distance = localMaxDistance.toDouble()
                    if (blas.findClosestHit(localStart, localDir, result)) {
                        if (globalTransform != null) {
                            result.setFromLocal(
                                globalTransform, localStart, localDir, result.distance.toFloat(),
                                Vector3f(result.geometryNormalWS), query
                            )
                        } else {
                            query.direction.mul(result.distance, result.positionWS).add(query.start)
                        }
                    }
                } else {
                    raycastLocalMeshClosestHit(
                        mesh, globalTransform, inverse, localStart, localDir, localEnd,
                        extraDistance, tmp0, query
                    )
                }
            } else {
                // mesh is scaled to zero on some axis, need to work in global coordinates
                // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates
                raycastGlobalClosestHit(query, globalTransform, mesh)
            }
        }
        return isCloser(query, original)
    }

    fun raycastGlobalMeshAnyHit(query: RayQuery, transform: Transform?, mesh: Mesh): Boolean {

        if (query.typeMask.and(Raycast.TRIANGLES) == 0) return false

        val maxDistance = query.result.distance
        val globalTransform = transform?.globalTransform
        // quick-path for easy meshes: no extra checks worth it
        if (mesh.numPrimitives <= 16) {
            raycastGlobalAnyHit1(globalTransform, mesh, query)
        } else {

            // todo it would be great if we would/could project the start+end onto the global aabb,
            //  if they lay outside, so we can use the faster method more often

            // transform the ray into local mesh coordinates
            val result = query.result
            val inverse = result.tmpMat4x3d
            if (globalTransform != null) {
                // local -> global
                globalTransform.invert(inverse)
            } else inverse.identity()

            val tmp0 = result.tmpVector3fs
            val tmp1 = result.tmpVector3ds

            val localSrt0 = inverse.transformPosition(tmp1[0].set(query.start))
            val localEnd0 = inverse.transformPosition(tmp1[1].set(query.end))
            val localDir0 = tmp1[2].set(tmp1[1]).sub(tmp1[0]).normalize()

            // todo reprojection doesn't work yet correctly (test: monkey ears)
            // project points onto front/back of box
            val extraDistance = 0f // projectRayToAABBFront(localSrt0, localDir0, mesh.aabb, dst = localSrt0)
            // projectRayToAABBBack(localEnd0, localDir0, mesh.aabb, dst = localEnd0)

            val localStart = tmp0[0].set(localSrt0)
            val localEnd = tmp0[1].set(localEnd0)
            val localDir = tmp0[2].set(localDir0)

            // if any coordinates of start or end are invalid, work in global coordinates
            val hasValidCoordinates = localStart.isFinite && localDir.isFinite

            // the fast method only works, if the numbers have roughly the same order of magnitude
            // val relativePositionsSquared = localStart.lengthSquared() / localEnd.lengthSquared()
            val orderOfMagnitudeIsFine = true // relativePositionsSquared in 1e-6f..1e6f

            if (hasValidCoordinates && orderOfMagnitudeIsFine && !mesh.hasBones) {
                val blas = mesh.raycaster ?: BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16)
                if (blas != null) {
                    mesh.raycaster = blas
                    val localMaxDistance = localStart.distance(localEnd)
                    result.distance = localMaxDistance.toDouble()
                    if (blas.findAnyHit(localStart, localDir, result)) {
                        if (globalTransform != null) {
                            result.setFromLocal(
                                globalTransform, localStart, localDir, result.distance.toFloat(),
                                Vector3f(result.geometryNormalWS), query
                            )
                        } else {
                            query.direction.mul(result.distance, result.positionWS).add(query.start)
                        }
                    }
                } else {
                    raycastLocalMeshAnyHit(
                        mesh, globalTransform, inverse, localStart, localDir, localEnd,
                        extraDistance, tmp0, query
                    )
                }
            } else {
                // mesh is scaled to zero on some axis, need to work in global coordinates
                // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates
                raycastGlobalAnyHit(globalTransform, mesh, query)
            }
        }
        return isCloser(query, maxDistance)
    }

    fun raycastLocalMeshClosestHit(
        mesh: Mesh, globalTransform: Matrix4x3d?, inverse: Matrix4x3d?,
        localSrt: Vector3f, localDir: Vector3f, localEnd: Vector3f,
        extraDistance: Double, tmp0: List<Vector3f>, query: RayQuery,
    ) {

        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        val typeMask = query.typeMask
        if (typeMask.and(Raycast.TRIANGLES) == 0) return
        val acceptFront = getCullingFront(typeMask, mesh.cullMode)
        val acceptBack = getCullingBack(typeMask, mesh.cullMode)

        // LOGGER.info(Vector3f(localEnd).sub(localStart).normalize().dot(localDir))
        // for that test, extend the radius at the start & end or sth like that
        // calculate local radius & radius extend
        val radiusScale = if (inverse != null) inverse.getScaleLength() / Maths.SQRT3 else 1.0// a guess
        val localRadiusAtOrigin = (query.radiusAtOrigin * radiusScale).toFloat()
        val localRadiusPerUnit = query.radiusPerUnit.toFloat()
        val localMaxDistance = localSrt.distance(localEnd)

        // test whether we intersect the aabb of this mesh
        if (mesh.getBounds().testLine(localSrt, localDir, localRadiusAtOrigin, localRadiusPerUnit, localMaxDistance)) {

            // test whether we intersect any triangle of this mesh
            val localMaxDistance2 = localMaxDistance + extraDistance
            var bestLocalDistance = localMaxDistance
            val localHitTmp = tmp0[3]
            val localNormalTmp = tmp0[4]
            val localHit = tmp0[5]
            val localNormal = tmp0[6]

            mesh.forEachTriangle(tmp0[7], tmp0[8], tmp0[9]) { a, b, c ->
                // check collision of localStart-localEnd with triangle ABC
                val localDistance = Triangles.rayTriangleIntersection(
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
                false
            }

            if (bestLocalDistance < localMaxDistance2) {
                query.result.setFromLocal(globalTransform, localHit, localNormal, query)
            }
        }
    }

    fun raycastGlobalClosestHit(query: RayQuery, globalTransform: Matrix4x3d?, mesh: Mesh) {
        if ((query.typeMask and Raycast.TRIANGLES) == 0) return
        // first test whether the aabbs really overlap
        val globalAABB = query.result.tmpAABBd.set(mesh.getBounds())
        if (globalTransform != null) globalAABB.transformAABB(globalTransform)
        if (globalAABB.testLine(query.start, query.direction, query.result.distance)) {
            raycastGlobalClosestHit1(query, globalTransform, mesh)
        }
    }

    fun raycastGlobalClosestHit1(query: RayQuery, globalTransform: Matrix4x3d?, mesh: Mesh) {
        val typeMask = query.typeMask
        if (typeMask.and(Raycast.TRIANGLES) == 0) return
        val acceptFront = getCullingFront(typeMask, mesh.cullMode)
        val acceptBack = getCullingBack(typeMask, mesh.cullMode)
        val tmp = query.result.tmpVector3ds
        mesh.forEachTriangle(tmp[2], tmp[3], tmp[4]) { a, b, c ->
            val tmpPos = tmp[0]
            val tmpNor = tmp[1]
            if (globalTransform != null) {
                globalTransform.transformPosition(a)
                globalTransform.transformPosition(b)
                globalTransform.transformPosition(c)
            }
            val maxDistance = query.result.distance
            val distance = Triangles.rayTriangleIntersection(
                query.start, query.direction, a, b, c,
                query.radiusAtOrigin, query.radiusPerUnit,
                maxDistance, tmpPos, tmpNor
            )
            val result = query.result
            if (distance < result.distance) {
                if (if (tmpNor.dot(query.direction) < 0f) acceptFront else acceptBack) {
                    result.distance = distance
                    result.positionWS.set(tmpPos)
                    result.geometryNormalWS.set(tmpNor)
                    result.shadingNormalWS.set(tmpNor)
                }
            }
            false
        }
    }

    fun raycastLocalMeshAnyHit(
        mesh: Mesh, globalTransform: Matrix4x3d?, inverse: Matrix4x3d,
        localSrt: Vector3f, localDir: Vector3f, localEnd: Vector3f,
        extraDistance: Float, tmp0: List<Vector3f>, // todo respect extra distance...
        query: RayQuery,
    ): Boolean {

        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        val typeMask = query.typeMask
        if (typeMask.and(Raycast.TRIANGLES) == 0) return false
        val acceptFront = getCullingFront(typeMask, mesh.cullMode)
        val acceptBack = getCullingBack(typeMask, mesh.cullMode)

        // for that test, extend the radius at the start & end or sth like that
        // calculate local radius & radius extend
        val radiusScale = inverse.getScaleLength() / Maths.SQRT3 // a guess
        val localRadiusAtOrigin = (query.radiusAtOrigin * radiusScale).toFloat()
        val localRadiusPerUnit = query.radiusPerUnit.toFloat()
        val localMaxDistance = localSrt.distance(localEnd)

        // test whether we intersect the aabb of this mesh
        if (mesh.getBounds().testLine(localSrt, localDir, localRadiusAtOrigin, localRadiusPerUnit, localMaxDistance)) {

            // test whether we intersect any triangle of this mesh
            var bestLocalDistance = localMaxDistance
            val localHitTmp = tmp0[3]
            val localNormalTmp = tmp0[4]
            val localHit = tmp0[5]
            val localNormal = tmp0[6]

            var hitSth = false
            mesh.forEachTriangle(tmp0[7], tmp0[8], tmp0[9]) { a, b, c ->
                // check collision of localStart-localEnd with triangle ABC
                val localDistance = Triangles.rayTriangleIntersection(
                    localSrt, localDir, a, b, c,
                    localRadiusAtOrigin, localRadiusPerUnit,
                    bestLocalDistance, localHitTmp, localNormalTmp
                )
                if (localDistance < bestLocalDistance && if (localNormalTmp.dot(localDir) < 0f) acceptFront else acceptBack) {
                    bestLocalDistance = localDistance
                    localHit.set(localHitTmp)
                    localNormal.set(localNormalTmp)
                    query.result.setFromLocal(globalTransform, localHit, localNormal, query)
                    hitSth = true
                }
                hitSth
            }
            return hitSth
        }
        return false
    }

    fun raycastGlobalAnyHit(globalTransform: Matrix4x3d?, mesh: Mesh, query: RayQuery) {
        if ((query.typeMask and Raycast.TRIANGLES) == 0) return
        // first test whether the aabbs really overlap
        val globalAABB = query.result.tmpAABBd.set(mesh.getBounds())
        if (globalTransform != null) globalAABB.transformAABB(globalTransform)
        if (globalAABB.testLine(query.start, query.direction, query.result.distance)) {
            raycastGlobalAnyHit1(globalTransform, mesh, query)
        }
    }

    fun raycastGlobalAnyHit1(globalTransform: Matrix4x3d?, mesh: Mesh, query: RayQuery) {
        val typeMask = query.typeMask
        if (typeMask.and(Raycast.TRIANGLES) == 0) return
        val acceptFront = getCullingFront(typeMask, mesh.cullMode)
        val acceptBack = getCullingBack(typeMask, mesh.cullMode)
        val result = query.result
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
            val distance = Triangles.rayTriangleIntersection(
                query.start, query.direction, a, b, c,
                query.radiusAtOrigin, query.radiusPerUnit,
                maxDistance, tmpPos, tmpNor
            )
            if (distance < result.distance && (if (tmpNor.dot(query.direction) < 0f) acceptFront else acceptBack)) {
                result.distance = distance
                result.positionWS.set(tmpPos)
                result.geometryNormalWS.set(tmpNor)
                result.shadingNormalWS.set(tmpNor)
                true
            } else false
        }
    }

    fun isCloser(query: RayQuery, maxDistance: Double): Boolean {
        return if (query.result.distance < maxDistance) {
            query.direction.mulAdd(
                query.result.distance,
                query.start,
                query.end
            ) // end = start + distance * dir, needed for colliders
            true
        } else false
    }

    fun raycastLocalMeshAnyHit(mesh: Mesh, start: Vector3f, dir: Vector3f, maxDistance: Float, typeMask: Int): Boolean {

        if (maxDistance <= 0.0) return false
        if (typeMask.and(Raycast.TRIANGLES) == 0) return false
        val acceptFront = getCullingFront(typeMask, mesh.cullMode)
        val acceptBack = getCullingBack(typeMask, mesh.cullMode)

        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        // test whether we intersect the aabb of this mesh
        if (mesh.getBounds().testLine(start, dir, maxDistance)) {
            val localHitTmp = JomlPools.vec3f.create()
            val localNormalTmp = JomlPools.vec3f.create()
            val a = JomlPools.vec3f.create()
            val b = JomlPools.vec3f.create()
            val c = JomlPools.vec3f.create()
            var hitSth = false
            mesh.forEachTriangle(a, b, c) { ai, bi, ci ->
                // check collision of localStart-localEnd with triangle ABC
                val localDistance = Triangles.rayTriangleIntersection(
                    start, dir, ai, bi, ci, maxDistance, localHitTmp, localNormalTmp
                )
                hitSth = localDistance < maxDistance && if (localNormalTmp.dot(dir) < 0f) acceptFront else acceptBack
                hitSth
            }
            JomlPools.vec3f.sub(5)
            return hitSth
        }
        return false
    }

    fun getCullingFront(typeMask: Int, mode: CullMode): Boolean {
        return getCulling(typeMask, mode, Raycast.TRIANGLE_FRONT, Raycast.TRIANGLE_BACK)
    }

    fun getCullingBack(typeMask: Int, mode: CullMode): Boolean {
        return getCulling(typeMask, mode, Raycast.TRIANGLE_BACK, Raycast.TRIANGLE_FRONT)
    }

    fun getCulling(typeMask: Int, mode: CullMode, frontMask: Int, backMask: Int): Boolean {
        val mask = when (mode) {
            CullMode.FRONT -> frontMask
            CullMode.BACK -> backMask
            else -> Raycast.TRIANGLES
        }
        return mask.and(typeMask) != 0
    }

    fun raycastLocalMeshClosestHit(
        mesh: Mesh, start: Vector3f, dir: Vector3f,
        maxDistance: Float, typeMask: Int, dstNormal: Vector3f?
    ): Float {

        var bestDistance = Float.POSITIVE_INFINITY
        if (maxDistance <= 0f) return bestDistance

        if (typeMask.and(Raycast.TRIANGLES) == 0) return bestDistance
        val acceptFront = getCullingFront(typeMask, mesh.cullMode)
        val acceptBack = getCullingBack(typeMask, mesh.cullMode)

        // todo if it is animated, we should ignore the aabb (or extend it), and must apply the appropriate bone transforms
        // test whether we intersect the aabb of this mesh
        if (mesh.getBounds().testLine(start, dir, bestDistance)) {
            val localHitTmp = JomlPools.vec3f.create()
            val localNormalTmp = JomlPools.vec3f.create()
            val a = JomlPools.vec3f.create()
            val b = JomlPools.vec3f.create()
            val c = JomlPools.vec3f.create()

            mesh.forEachTriangle(a, b, c) { ai, bi, ci ->
                // check collision of localStart-localEnd with triangle ABC
                val localDistance = Triangles.rayTriangleIntersection(
                    start, dir, ai, bi, ci, bestDistance, localNormalTmp, localHitTmp,
                )
                if (localDistance < bestDistance && localDistance <= maxDistance &&
                    if (localNormalTmp.dot(dir) < 0f) acceptFront else acceptBack
                ) {
                    bestDistance = localDistance
                    dstNormal?.set(localNormalTmp)
                }
                false
            }

            JomlPools.vec3f.sub(5)
            if (bestDistance.isFinite()) {
                dstNormal?.safeNormalize()
            }
        }
        return bestDistance
    }
}