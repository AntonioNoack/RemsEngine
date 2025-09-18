package me.anno.games.flatworld.streets

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.spline.SplineMesh
import me.anno.ecs.components.mesh.spline.SplineProfile
import me.anno.maths.MinMax.max
import me.anno.utils.Color.black
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.Vector2f
import org.joml.Vector3d

object StreetMeshBuilder {

    val streetProfile = SplineProfile(
        listOf(
            Vector2f(-7f, -0.49f),
            Vector2f(-4.81f, +0.3f),
            Vector2f(-4.8f, +0.6f),
            Vector2f(-3.31f, +0.6f),
            Vector2f(-3.3f, +0.5f),
            Vector2f(+3.3f, +0.5f), // dark
            Vector2f(+3.31f, +0.6f), // light
            Vector2f(+4.8f, +0.6f), // light
            Vector2f(+4.81f, +0.3f), // green
            Vector2f(+7f, -0.49f), // green
        ), null,
        IntArrayList(
            intArrayOf(
                0x77dd77 or black,
                0x77dd77 or black,
                0xaaaaaa or black,
                0xaaaaaa or black,
                0x555555 or black,
                0x555555 or black,
                0xaaaaaa or black,
                0xaaaaaa or black,
                0x77dd77 or black,
                0x77dd77 or black,
            )
        ), false
    )

    fun buildMesh(segment: StreetSegment, dst: Mesh): Mesh {
        // for the start, use a LineRenderer or
        val profile = streetProfile
        val points = if (segment.b != null) {
            generatePoints(segment)
        } else {
            listOf(segment.a, segment.c).leftRight()
        }
        SplineMesh.generateSplineMesh(
            dst, profile, false,
            false, false,
            true, points
        )
        dst.invalidateGeometry()
        return dst
    }

    private fun generatePoints(segment: StreetSegment): List<Vector3d> {
        val (a, b, c) = segment
        val steps = max(2, (20f * a.sub(b!!, Vector3d()).angle(b.sub(c, Vector3d()))).toInt())
        // ensure left/right is correct at start/end
        val middle = createArrayList(steps - 2) {
            val t = (it + 1) / (steps - 1.0)
            segment.interpolate(t)
        }
        val result = ArrayList<Vector3d>(steps * 2)
        val ai = segment.interpolate(0.001)
        val ci = segment.interpolate(0.999)
        leftRightStep(result, a, a, ai)
        when (middle.size) {
            0 -> {}
            1 -> {
                val middleI = middle.first()
                leftRightStep(result, middleI, a, c)
            }
            else -> {
                leftRightStep(result, middle.first(), a, ai)
                for (i in 1 until middle.lastIndex) {
                    middle.leftRightStep(result, i)
                }
                leftRightStep(result, middle.last(), ci, c)
            }
        }
        leftRightStep(result, c, ci, c)
        return result
    }

    private fun List<Vector3d>.leftRight(): List<Vector3d> {
        val result = ArrayList<Vector3d>(size * 2)
        for (i in this@leftRight.indices) {
            leftRightStep(result, i)
        }
        return result
    }

    private fun List<Vector3d>.leftRightStep(result: ArrayList<Vector3d>, i: Int) {
        val p = this[i]
        val p0 = if (i == 0) p else this[i - 1]
        val p1 = if (i == lastIndex) p else this[i + 1]
        leftRightStep(result, p, p0, p1)
    }

    private fun leftRightStep(
        result: ArrayList<Vector3d>,
        p: Vector3d, p0: Vector3d, p1: Vector3d
    ) {
        val dx = p1.sub(p0, Vector3d())
            .cross(0.0, 1.0, 0.0)
            .safeNormalize()
        leftRightStepAdd(result, p, dx)
    }

    private fun leftRightStepAdd(result: ArrayList<Vector3d>, p: Vector3d, dx: Vector3d) {
        result.add(p.sub(dx, Vector3d()))
        result.add(p.add(dx, Vector3d()))
    }
}