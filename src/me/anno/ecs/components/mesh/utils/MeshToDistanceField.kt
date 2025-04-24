package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.maths.geometry.Rasterizer
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.utils.types.Triangles.subCross
import org.joml.AABBf
import org.joml.Planef
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/**
 * Reverse operation to MarchingCubes.
 * */
object MeshToDistanceField {

    private val sides = ByteArray(27)

    init {
        var i = 0
        for (z in -1..1) {
            for (y in -1..1) {
                for (x in -1..1) {
                    val value = (x + 1) + (y + 1).shl(2) + (z + 1).shl(4)
                    sides[i++] = value.toByte()
                }
            }
        }
    }

    fun getIndex(fieldSize: Vector3i, x: Int, y: Int, z: Int): Int {
        return fieldSize.x * (fieldSize.y * z + y) + x
    }

    fun meshToDistanceField(mesh: Mesh, bounds: AABBf, fieldSize: Vector3i): FloatArray {
        val field = FloatArray(fieldSize.x * fieldSize.y * fieldSize.z)
        field.fill(Float.POSITIVE_INFINITY)
        meshToSparseDistanceField(mesh, bounds, fieldSize, field)
        // kind of good at smoothing the edges, but when a side is unbalanced, the result is biased
        // (e.g., when one side is NaNs and the other is defined)
        smoothField(fieldSize, field)
        spreadValues(fieldSize, field)
        return field
    }

    fun meshToSparseDistanceField(mesh: Mesh, bounds: AABBf, fieldSize: Vector3i, field: FloatArray) {

        val positions = mesh.positions ?: return
        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()

        val x0 = bounds.minX
        val y0 = bounds.minY
        val z0 = bounds.minZ

        val dx = (fieldSize.x - 1) / bounds.deltaX
        val dy = (fieldSize.y - 1) / bounds.deltaY
        val dz = (fieldSize.z - 1) / bounds.deltaZ

        // scale A,B,C by bounds and fieldSize
        fun transform(v: Vector3f) {
            v.x = (v.x - x0) * dx
            v.y = (v.y - y0) * dy
            v.z = (v.z - z0) * dz
        }

        val fieldBounds = AABBf(
            0f, 0f, 0f,
            fieldSize.x.toFloat(),
            fieldSize.y.toFloat(),
            fieldSize.z.toFloat()
        )

        val plane = Planef()
        val normal = Vector3f()

        mesh.forEachTriangleIndex { ai, bi, ci ->

            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            c.set(positions, ci * 3)

            transform(a)
            transform(b)
            transform(c)

            plane.setFromTriangle(a, b, c, normal)

            Rasterizer.rasterize(a, b, c, fieldBounds) { x, y, z ->
                for (i in sides.indices) {
                    val side = sides[i].toInt()
                    val dx = side.and(3)
                    val dy = side.shr(2).and(3)
                    val dz = side.shr(4).and(3)
                    val xi = x + dx - 1
                    val yi = y + dy - 1
                    val zi = z + dz - 1
                    if (xi in 0 until fieldSize.x &&
                        yi in 0 until fieldSize.y &&
                        zi in 0 until fieldSize.z
                    ) {
                        val index = getIndex(fieldSize, xi, yi, zi)
                        // calculate distance of triangle to this vertex
                        val newDistance = plane.dot(xi.toFloat(), yi.toFloat(), zi.toFloat())
                        // join with previous distance
                        val prevDistance = field[index]
                        field[index] = absMinV2(prevDistance, newDistance)
                    }
                }
            }
            false
        }
    }

    fun smoothField(fieldSize: Vector3i, field: FloatArray) {
        val clone = field.clone()
        for (z in 1 until fieldSize.z - 1) {
            for (y in 1 until fieldSize.y - 1) {
                for (x in 1 until fieldSize.x - 1) {
                    var weight = 0
                    var sum = 0f
                    for (i in sides.indices) {
                        val side = sides[i].toInt()
                        val dx = side.and(3)
                        val dy = side.shr(2).and(3)
                        val dz = side.shr(4).and(3)
                        val xi = x + dx - 1
                        val yi = y + dy - 1
                        val zi = z + dz - 1
                        val valueI = clone[getIndex(fieldSize, xi, yi, zi)]
                        if (valueI.isFinite()) {
                            weight++
                            sum += valueI
                        }
                    }
                    if (weight > 0) {
                        field[getIndex(fieldSize, x, y, z)] = sum / weight
                    }
                }
            }
        }
    }

    fun spreadValues(fieldSize: Vector3i, field: FloatArray) {
        val newlySet = BooleanArray(field.size)
        for (z in 0 until fieldSize.z) {
            for (y in 0 until fieldSize.y) {
                for (x in 0 until fieldSize.x) {
                    val index = getIndex(fieldSize, x, y, z)
                    val value = field[index]
                    if (value.isFinite() && !newlySet[index]) {
                        for (side in BlockSide.entries) {
                            val xi = x + side.x
                            val yi = y + side.y
                            val zi = z + side.z
                            if (xi in 0 until fieldSize.x &&
                                yi in 0 until fieldSize.y &&
                                zi in 0 until fieldSize.z
                            ) {
                                val index2 = getIndex(fieldSize, xi, yi, zi)
                                if (!field[index2].isFinite()) {
                                    field[index2] = value + if (value < 0f) -1f else +1f
                                    newlySet[index2] = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This function tries to create a continuous outer shape.
     * */
    private fun absMinV2(a: Float, b: Float): Float {
        return if (sign(a) == sign(b)) {
            if (abs(a) < abs(b)) a else b
        } else min(a, b)
    }

    fun Planef.setFromTriangle(a: Vector3f, b: Vector3f, c: Vector3f, tmpNormal: Vector3f): Planef {
        subCross(a, b, c, tmpNormal).safeNormalize()
        return set(pos = a, tmpNormal)
    }
}