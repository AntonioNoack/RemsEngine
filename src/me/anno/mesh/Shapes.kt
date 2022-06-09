package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.sign

/**
 * a library of often used meshes, so we don't have too many copies in the engine
 * */
@Suppress("unused")
object Shapes {

    fun scale(src: FloatArray, scale: Float, dst: FloatArray = FloatArray(src.size)): FloatArray {
        for (i in src.indices) {
            dst[i] = src[i] * scale
        }
        return dst
    }

    fun linear(src: FloatArray, offset: Vector3f, scale: Vector3f, dst: FloatArray = FloatArray(src.size)): FloatArray {
        for (i in src.indices step 3) {
            dst[i] = src[i] * scale.x + offset.x
            dst[i + 1] = src[i + 1] * scale.y + offset.y
            dst[i + 2] = src[i + 2] * scale.z + offset.z
        }
        return dst
    }

    /**
     * creates a flat shaded positions array from an indexed mesh
     * */
    fun unpack(base: Mesh): FloatArray {
        val pos = base.positions!!
        val idx = base.indices ?: return pos
        val ret = FloatArray(idx.size * 3)
        var i = 0
        for (index in idx) {
            var i3 = index * 3
            ret[i++] = pos[i3++]
            ret[i++] = pos[i3++]
            ret[i++] = pos[i3]
        }
        return ret
    }

    /**
     * front/back/both mesh container
     * may support normals in the future
     *
     * @param positions mesh positions
     * @param indices mesh indices for indexed meshes, may be null
     * @param normals mesh normals, or null for flat shading
     * */
    class FBBMesh(val positions: FloatArray, val indices: IntArray?, val normals: FloatArray? = null) {

        constructor(base: FBBMesh, scale: Float) :
                this(scale(base.positions, abs(scale)), base.indices, base.normals)

        fun scaled(scale: Float) =
            FBBMesh(this, scale)

        fun linear(offset: Vector3f, scale: Vector3f) =
            FBBMesh(linear(positions, offset, scale), indices)

        val front = Mesh()
        val back = Mesh()
        val both = Mesh()

        init {
            val indices = indices
            val positions = positions
            front.positions = positions
            front.indices = indices
            front.normals = normals
            back.positions = FloatArray(positions.size) { -positions[it] }
            back.indices = indices
            back.normals = if (normals != null) scale(normals, -1f) else null
            val vertexCount = positions.size / 3
            val bothIndices = if (indices != null) {
                val bi = IntArray(indices.size * 2)
                for (i in indices.indices) {
                    val ii = indices[i]
                    bi[i] = ii
                    bi[i + indices.size] = ii + vertexCount
                }
                bi
            } else null
            val bothPositions = FloatArray(positions.size * 2)
            for (i in positions.indices) {
                val pi = positions[i]
                bothPositions[i] = pi
                bothPositions[i + positions.size] = -pi
            }
            both.positions = bothPositions
            both.indices = bothIndices
            both.normals = if (normals != null) normals + back.normals!! else null
        }
    }

    /**
     * cube with halfExtends = 1, full extends = 2;
     * smoothly shaded
     * */
    val smoothCube = FBBMesh(
        floatArrayOf(
            -1f, -1f, -1f,
            -1f, -1f, +1f,
            -1f, +1f, -1f,
            -1f, +1f, +1f,
            +1f, -1f, -1f,
            +1f, -1f, +1f,
            +1f, +1f, -1f,
            +1f, +1f, +1f,
        ), intArrayOf(
            0, 1, 3, 3, 2, 0,
            1, 5, 7, 7, 3, 1,
            2, 3, 7, 7, 6, 2,
            4, 0, 2, 2, 6, 4,
            4, 6, 7, 7, 5, 4,
            1, 0, 4, 4, 5, 1
        )
    )

    val flatCube = FBBMesh(unpack(smoothCube.front), null)

    /**
     * cube with half extends 1, full extends 2; front only
     * */
    val cube11Smooth = smoothCube.front

    val cube11Flat = flatCube.front

    // a cube (12t, 8p) has a volume of 8.0m² to cover a sphere,
    // while a sphere itself only has 4.2m³,
    // and a tetrahedron (8t, 6p) has 6.9m³ -> use the tetrahedron

    /**
     * cube with half extends 0.5, full extends 1;
     * shaded smoothly
     * */
    val cube05Smooth = FBBMesh(smoothCube, 0.5f)

    /**
     * cube with half extends 0.5, full extends 1;
     * shaded flat
     * */
    val cube05Flat = FBBMesh(flatCube, 0.5f)

    val tetrahedron = FBBMesh(
        floatArrayOf(
            0f, 1f, 0f,
            1f, 0f, 0f,
            0f, 0f, 1f,
            -1f, 0f, 0f,
            0f, 0f, -1f,
            0f, -1f, 0f
        ), intArrayOf(
            0, 1, 2, 0, 2, 3, 0, 3, 4, 0, 4, 1,
            1, 2, 5, 2, 3, 5, 3, 4, 5, 4, 1, 5
        )
    )

    // 1.226f * 1.414f = scale to cover a sphere, guesses in Blender
    val sphereCoveringTetrahedron = FBBMesh(tetrahedron, 1.226f * 1.414f)

    fun createCube(
        mesh: Mesh,
        sizeX: Float,
        sizeY: Float,
        sizeZ: Float,
        offsetX: Float,
        offsetY: Float,
        offsetZ: Float,
        withNormals: Boolean,
        front: Boolean,
        back: Boolean,
    ) {
        if (!front && !back) {
            // mmh, awkward request
            mesh.positions = FloatArray(0)
        } else {
            var position = mesh.positions
            val base = if (withNormals) flatCube else smoothCube
            val base1 = if (front && back) base.both else if (front) base.front else base.back
            val base2 = base1.positions!!
            if (position == null || position.size != base2.size) {
                position = FloatArray(base2.size)
                mesh.positions = position
            }
            val sx = sizeX * 0.5f
            val sy = sizeY * 0.5f
            val sz = sizeZ * 0.5f
            for (i in position.indices step 3) {
                position[i + 0] = sign(base2[i + 0]) * sx + offsetX
                position[i + 1] = sign(base2[i + 1]) * sy + offsetY
                position[i + 2] = sign(base2[i + 2]) * sz + offsetZ
            }
            mesh.indices = base1.indices
            mesh.normals = base1.normals
        }
    }

}