package me.anno.engine.raycast

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.maths.bvh.HitType
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Triangles
import org.joml.Matrix4x3f
import org.joml.Matrix4x3m
import org.joml.Vector3d

object RaycastSkeletal {

    private class RaycastSkeletalHelper(query: RayQuery, mesh: Mesh, val matrices: List<Matrix4x3f>) {

        val result = query.result
        val tmpF = result.tmpVector3fs
        val tmpD = result.tmpVector3ds
        val positions = mesh.positions!!
        val weights = mesh.boneWeights
        val indices = mesh.boneIndices!!

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
    }

    fun raycastGlobalBoneMesh(
        query: RayQuery, globalTransform: Matrix4x3m?, mesh: Mesh,
        matrices: List<Matrix4x3f>
    ) {
        val typeMask = query.typeMask
        val acceptFront = typeMask.hasFlag(Raycast.TRIANGLE_FRONT)
        val acceptBack = typeMask.hasFlag(Raycast.TRIANGLE_BACK)
        if (!acceptFront && !acceptBack) return

        mesh.positions ?: return
        mesh.boneIndices ?: return

        val helper = RaycastSkeletalHelper(query, mesh, matrices)
        val result = query.result
        val tmpD = result.tmpVector3ds
        val tmpPos = tmpD[0]
        val tmpNor = tmpD[1]
        val anyHit = query.result.hitType == HitType.ANY
        mesh.forEachTriangleIndex { ai, bi, ci ->
            val distance = getDistance(ai, bi, ci, helper, globalTransform, query, result)
            if (isHit(distance, query, result, acceptFront, acceptBack)) {
                result.distance = distance
                result.positionWS.set(tmpPos)
                result.geometryNormalWS.set(tmpNor)
                result.shadingNormalWS.set(tmpNor)
                anyHit
            } else false
        }
    }

    private fun isHit(
        distance: Double, query: RayQuery, result: RayHit,
        acceptFront: Boolean, acceptBack: Boolean
    ): Boolean {
        val tmpD = result.tmpVector3ds
        val tmpNor = tmpD[1]
        return distance < result.distance && if (tmpNor.dot(query.direction) < 0f) acceptFront else acceptBack
    }

    private fun getDistance(
        ai: Int, bi: Int, ci: Int,
        helper: RaycastSkeletalHelper,
        globalTransform: Matrix4x3m?,
        query: RayQuery, result: RayHit,
    ): Double {

        val a = helper.transformByBones(0, ai)
        val b = helper.transformByBones(1, bi)
        val c = helper.transformByBones(2, ci)

        if (globalTransform != null) {
            globalTransform.transformPosition(a)
            globalTransform.transformPosition(b)
            globalTransform.transformPosition(c)
        }

        val maxDistance = result.distance
        val tmpD = result.tmpVector3ds
        val tmpPos = tmpD[0]
        val tmpNor = tmpD[1]
        return Triangles.rayTriangleIntersection(
            query.start, query.direction, a, b, c,
            query.radiusAtOrigin, query.radiusPerUnit,
            maxDistance, tmpPos, tmpNor
        )
    }
}