package me.anno.maths.bvh

import me.anno.engine.raycast.RayHit
import me.anno.utils.types.Triangles.rayTriangleIntersectionFront
import org.joml.AABBf
import org.joml.Vector3f

class BLASLeaf(
    val start: Int, val length: Int,
    val geometry: GeometryData,
    bounds: AABBf
) : BLASNode(bounds) {

    override fun maxDepth() = 1

    override fun raycast(pos: Vector3f, dir: Vector3f, invDir: Vector3f, dirIsNeg: Int, hit: RayHit): Boolean {
        hit.blasCtr++
        return if (bounds.isRayIntersecting(pos, invDir, hit.distance.toFloat())) {
            hit.trisCtr += length

            val vs = hit.tmpVector3fs
            // 0-3 are used by Raycast
            val a = vs[4]
            val b = vs[5]
            val c = vs[6]
            val localHitTmp = vs[7]
            val localNormalTmp = vs[8]

            val geometryNormal = vs[9]
            val shadingNormal = vs[10]
            val barycentrics = vs[11]

            var bestLocalDistance = hit.distance.toFloat()
            val bestLocalDistance0 = bestLocalDistance

            val positions = geometry.positions
            val indices = geometry.indices
            val normals = geometry.normals

            val anyHit = hit.hitType == HitType.ANY
            var i3 = start * 3
            val j3 = i3 + length * 3
            var triangleIndexX3 = -1
            while (i3 < j3) {

                val ai = indices[i3] * 3
                val bi = indices[i3 + 1] * 3
                val ci = indices[i3 + 2] * 3
                a.set(positions, ai)
                b.set(positions, bi)
                c.set(positions, ci)
                i3 += 3

                val localDistance = rayTriangleIntersectionFront(
                    pos, dir, a, b, c, bestLocalDistance,
                    localNormalTmp, localHitTmp, barycentrics
                )

                if (localDistance < bestLocalDistance) {
                    bestLocalDistance = localDistance
                    geometryNormal.set(localNormalTmp)
                    interpolateBarycentrics(normals, ai, bi, ci, barycentrics, shadingNormal)
                    triangleIndexX3 = i3
                    if (anyHit) break
                }
            }

            if (bestLocalDistance < bestLocalDistance0) {
                hit.distance = bestLocalDistance.toDouble()
                hit.geometryNormalWS.set(geometryNormal)
                hit.shadingNormalWS.set(shadingNormal)
                hit.barycentric.set(barycentrics)
                hit.triangleIndex = triangleIndexX3 / 3
                true
            } else false
        } else false
    }

    override fun findGeometryData() = geometry

    companion object {
        fun interpolateBarycentrics(
            values: FloatArray, ai: Int, bi: Int, ci: Int,
            bary: Vector3f, dst: Vector3f
        ) {
            val (bx, by, bz) = bary
            dst.set(
                bx * values[ai] + by * values[bi] + bz * values[ci],
                bx * values[ai + 1] + by * values[bi + 1] + bz * values[ci + 1],
                bx * values[ai + 2] + by * values[bi + 2] + bz * values[ci + 2],
            )
        }
    }
}