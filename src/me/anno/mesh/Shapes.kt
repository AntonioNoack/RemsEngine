package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.gpu.CullMode
import me.anno.gpu.GFX
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.sign

/**
 * a library of often used meshes, so we don't have too many copies in the engine
 * */
@Suppress("unused")
object Shapes {

    private fun scale(src: FloatArray, scale: Float): FloatArray {
        val dst = FloatArray(src.size)
        for (i in src.indices) {
            dst[i] = src[i] * scale
        }
        return dst
    }

    private fun linear(src: FloatArray, offset: Vector3f, scale: Vector3f): FloatArray {
        val dst = FloatArray(src.size)
        forLoopSafely(src.size, 3) { i ->
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
    class FBBMesh(
        name: String,
        val positions: FloatArray,
        val indices: IntArray?,
        val normals: FloatArray? = null
    ) {

        constructor(base: FBBMesh, scale: Float) :
                this(base.front.name, base, scale)

        constructor(name: String, base: FBBMesh, scale: Float) :
                this(name, scale(base.positions, abs(scale)), base.indices, base.normals) {
            front.uvs = base.front.uvs
            back.uvs = base.back.uvs
            both.uvs = base.both.uvs
        }

        fun scaled(scale: Float) =
            FBBMesh(this, scale)

        fun scaled(scale: Vector3f) = linear(Vector3f(), scale)

        fun linear(offset: Vector3f, scale: Vector3f) =
            FBBMesh(front.name, linear(positions, offset, scale), indices)

        val front = Mesh()
        val back = Mesh()
        val both = Mesh()

        init {
            val indices = indices
            val positions = positions
            front.name = name
            front.positions = positions
            front.indices = indices
            front.normals = normals
            front.cullMode = CullMode.FRONT
            back.name = name
            back.positions = positions
            back.indices = indices
            back.normals = normals
            back.cullMode = CullMode.BACK
            both.name = name
            both.positions = positions
            both.indices = indices
            both.normals = normals
            both.cullMode = CullMode.BOTH
            // save us a few allocations
            if (GFX.isGFXThread()) {
                front.ensureBuffer()
                back.buffer = front.buffer
                back.triBuffer = front.triBuffer
                both.buffer = front.buffer
                both.triBuffer = front.triBuffer
            }
        }

        fun withUVs(): FBBMesh {
            val bounds = front.getBounds()
            val su = 1f / (bounds.maxX - bounds.minX)
            val sv = 1f / (bounds.maxY - bounds.minY)
            val uvs = FloatArray(positions.size / 3 * 2)
            forLoopSafely(positions.size, 3) { i ->
                val j = i / 3 * 2
                uvs[j] = (positions[i] - bounds.minX) * su
                uvs[j + 1] = (positions[i + 1] - bounds.minY) * sv
            }
            front.uvs = uvs
            back.uvs = uvs
            both.uvs = uvs
            return this
        }
    }

    val flat11 = FBBMesh(
        "flat11", floatArrayOf(
            -1f, -1f, 0f,
            +1f, -1f, 0f,
            -1f, +1f, 0f,
            +1f, +1f, 0f,
        ), intArrayOf(0, 1, 3, 3, 2, 0)
    ).withUVs()

    /**
     * cube with halfExtents = 1, full extents = 2;
     * smoothly shaded
     * */
    val smoothCube = FBBMesh(
        "smoothCube", floatArrayOf(
            -1f, -1f, -1f,
            -1f, -1f, +1f,
            -1f, +1f, -1f,
            -1f, +1f, +1f,
            +1f, -1f, -1f,
            +1f, -1f, +1f,
            +1f, +1f, -1f,
            +1f, +1f, +1f,
        ), intArrayOf(
            0, 1, 3, 3, 2, 0, // -x, left side
            4, 6, 7, 7, 5, 4, // +x, right side
            1, 0, 4, 4, 5, 1, // -y, bottom side
            2, 3, 7, 7, 6, 2, // +y, top side
            4, 0, 2, 2, 6, 4, // -z, back side
            1, 5, 7, 7, 3, 1, // +z, front side
        )
    )

    val flatCube = FBBMesh("flatCube", unpack(smoothCube.front), null)

    /**
     * cube with half extents 1, full extents 2; front only
     * */
    val cube11Smooth = smoothCube.front

    val cube11Flat = flatCube.front

    // a cube (12t, 8p) has a volume of 8.0 m² to cover a sphere,
    // while a sphere itself only has 4.2 m³,
    // and a tetrahedron (8t, 6p) has 6.9 m³ -> use the tetrahedron

    /**
     * cube with half extents 0.5, full extents 1;
     * shaded smoothly
     * */
    val cube05Smooth = FBBMesh("cube05Smooth", smoothCube, 0.5f)

    /**
     * cube with half extents 0.5, full extents 1;
     * shaded flat
     * */
    val cube05Flat = FBBMesh("cube05Flat", flatCube, 0.5f)

    val tetrahedron = FBBMesh(
        "tetrahedron", floatArrayOf(
            0f, 1f, 0f,
            1f, 0f, 0f,
            0f, 0f, 1f,
            -1f, 0f, 0f,
            0f, 0f, -1f,
            0f, -1f, 0f
        ), intArrayOf(
            1, 0, 2, 2, 0, 3, 3, 0, 4, 4, 0, 1,
            5, 1, 2, 5, 2, 3, 5, 3, 4, 5, 4, 1,
        )
    )

    // 1.226f * 1.414f = scale to cover a sphere, guesses in Blender
    val sphereCoveringTetrahedron = FBBMesh("coveringTetrahedron", tetrahedron, 1.226f * 1.414f)

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
            val base = if (withNormals) flatCube else smoothCube
            val base1 = if (front && back) base.both else if (front) base.front else base.back
            val base2 = base1.positions!!
            val position = mesh.positions.resize(base2.size)
            mesh.positions = position
            val sx = sizeX * 0.5f
            val sy = sizeY * 0.5f
            val sz = sizeZ * 0.5f
            forLoopSafely(position.size, 3) { i ->
                position[i] = sign(base2[i]) * sx + offsetX
                position[i + 1] = sign(base2[i + 1]) * sy + offsetY
                position[i + 2] = sign(base2[i + 2]) * sz + offsetZ
            }
            mesh.indices = base1.indices
            mesh.normals = base1.normals
            mesh.cullMode = base1.cullMode
        }
    }
}