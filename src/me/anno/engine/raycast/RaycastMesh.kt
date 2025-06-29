package me.anno.engine.raycast

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.gpu.CullMode
import me.anno.maths.Maths
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.HitType
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Triangles
import org.joml.Matrix4x3
import org.joml.Vector3f

object RaycastMesh {

    fun raycastGlobalMesh(query: RayQuery, transform: Transform?, mesh: Mesh): Boolean {
        val original = query.result.distance
        if (query.typeMask.and(Raycast.TRIANGLES) == 0) return false

        raycastGlobalViaLocal(query, transform?.globalTransform, mesh)
        return isCloser(query, original)
    }

    fun raycastLocalMesh(
        mesh: Mesh, globalTransform: Matrix4x3?, inverse: Matrix4x3?,
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

            val anyHit = query.result.hitType == HitType.ANY
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
                        anyHit
                    } else false
                } else false
            }

            if (bestLocalDistance < localMaxDistance2) {
                query.result.setFromLocal(globalTransform, localHit, localNormal, query)
            }
        }
    }

    fun raycastGlobal(query: RayQuery, globalTransform: Matrix4x3?, mesh: Mesh) {
        if ((query.typeMask and Raycast.TRIANGLES) == 0) return
        // first test whether the aabbs really overlap
        val globalAABB = query.result.tmpAABBd.set(mesh.getBounds())
        if (globalTransform != null) globalAABB.transformAABB(globalTransform)
        if (globalAABB.testLine(query.start, query.direction, query.result.distance)) {
            raycastGlobalImpl(query, globalTransform, mesh)
        }
    }

    fun raycastGlobalImpl(query: RayQuery, globalTransform: Matrix4x3?, mesh: Mesh) {
        val typeMask = query.typeMask
        if (typeMask.and(Raycast.TRIANGLES) == 0) return
        val acceptFront = getCullingFront(typeMask, mesh.cullMode)
        val acceptBack = getCullingBack(typeMask, mesh.cullMode)
        val tmp = query.result.tmpVector3ds
        mesh.forEachTriangle(tmp[3], tmp[4], tmp[5]) { a, b, c ->
            val dstPosition = tmp[0]
            val dstNormal = tmp[1]
            val tmpDir = tmp[2]
            if (globalTransform != null) {
                globalTransform.transformPosition(a)
                globalTransform.transformPosition(b)
                globalTransform.transformPosition(c)
            }
            val maxDistance = query.result.distance
            val distance = Triangles.rayTriangleIntersection(
                query.start, tmpDir.set(query.direction), a, b, c,
                query.radiusAtOrigin, query.radiusPerUnit,
                maxDistance, dstPosition, dstNormal
            )
            val result = query.result
            if (distance < result.distance) {
                if (if (dstNormal.dot(query.direction) < 0f) acceptFront else acceptBack) {
                    result.distance = distance
                    result.positionWS.set(dstPosition)
                    result.geometryNormalWS.set(dstNormal)
                    result.shadingNormalWS.set(dstNormal)
                }
            }
            false
        }
    }

    fun raycastGlobalViaLocal(query: RayQuery, globalTransform: Matrix4x3?, mesh: Mesh) {

        // transform the ray into local mesh coordinates
        val result = query.result
        val inverse = result.tmpMat4x3m
        if (globalTransform != null) {
            // local -> global
            globalTransform.invert(inverse)
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
            val blas = BLASCache.getBLAS(mesh)
            if (blas != null) {
                raycastBLAS(blas, localStart, localDir, localEnd, globalTransform, query)
            } else {
                raycastLocalMesh(
                    mesh, globalTransform, inverse, localStart, localDir, localEnd,
                    extraDistance, tmp0, query
                )
            }
        } else {
            // mesh is scaled to zero on some axis, need to work in global coordinates
            // this is quite a bit more expensive, because we need to transform each mesh point into global coordinates
            raycastGlobal(query, globalTransform, mesh)
        }
    }

    fun raycastBLAS(
        blas: BLASNode, localStart: Vector3f, localDir: Vector3f, localEnd: Vector3f,
        globalTransform: Matrix4x3?, query: RayQuery
    ) {
        val result = query.result
        val localMaxDistance = localStart.distance(localEnd)
        result.distance = localMaxDistance.toDouble()
        if (blas.raycast(localStart, localDir, result)) {
            if (globalTransform != null) {
                result.setFromLocal(
                    globalTransform, localStart, localDir, result.distance.toFloat(),
                    Vector3f(result.geometryNormalWS), query
                )
            } else {
                query.direction.mul(result.distance, result.positionWS).add(query.start)
            }
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

    fun raycastLocalMesh(
        mesh: Mesh, start: Vector3f, dir: Vector3f,
        maxDistance: Float, typeMask: Int, dstNormal: Vector3f?,
        anyHit: Boolean
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
                    start, dir, ai, bi, ci,
                    bestDistance, localHitTmp, localNormalTmp,
                )
                if (localDistance < bestDistance && localDistance <= maxDistance &&
                    if (localNormalTmp.dot(dir) < 0f) acceptFront else acceptBack
                ) {
                    bestDistance = localDistance
                    dstNormal?.set(localNormalTmp)
                    anyHit
                } else false
            }

            JomlPools.vec3f.sub(5)
            if (bestDistance.isFinite()) {
                dstNormal?.safeNormalize()
            }
        }
        return bestDistance
    }
}