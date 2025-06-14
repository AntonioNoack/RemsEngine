package me.anno.ecs.components.mesh

import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4x3
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Transforming a mesh by an affine matrix
 * */
object TransformMesh {

    fun Mesh.transform(matrix: Matrix4x3): Mesh {
        if (matrix.isIdentity()) return this
        unlinkPositionsAndNormals()
        positions = transformPositionsOrNull(matrix, positions, 3)
        normals = transformDirectionsOrNull(matrix, normals, 3)
        tangents = transformDirectionsOrNull(matrix, tangents, 4)
        invalidateGeometry()
        return this
    }

    fun Mesh.transform(matrix: Matrix4x3f): Mesh {
        if (matrix.isIdentity()) return this
        unlinkPositionsAndNormals()
        positions = transformPositionsOrNull(matrix, positions, 3)
        normals = transformDirectionsOrNull(matrix, normals, 3)
        tangents = transformDirectionsOrNull(matrix, tangents, 4)
        invalidateGeometry()
        return this
    }

    fun Mesh.translate(delta: Vector3d): Mesh {
        if (delta.x == 0.0 && delta.y == 0.0 && delta.z == 0.0) return this
        unlinkPositionsAndNormals()
        positions = translatePositionsOrNull(delta, positions, 3)
        invalidateGeometry()
        return this
    }

    fun Mesh.scale(scale: Vector3f): Mesh {
        if (scale.x == 1f && scale.y == 1f && scale.z == 1f) return this
        unlinkPositionsAndNormals()
        positions = scalePositionsOrNull(scale, positions, 3)
        invalidateGeometry()
        return this
    }

    fun Mesh.rotateX90DegreesImpl(): Mesh {
        unlinkPositionsAndNormals()
        rotateX90Degrees(positions, 3)
        rotateX90Degrees(normals, 3)
        rotateX90Degrees(tangents, 4)
        invalidateGeometry()
        return this
    }

    fun Mesh.rotateY90DegreesImpl(): Mesh {
        unlinkPositionsAndNormals()
        rotateY90Degrees(positions, 3)
        rotateY90Degrees(normals, 3)
        rotateY90Degrees(tangents, 4)
        invalidateGeometry()
        return this
    }

    fun rotateX90Degrees(src: FloatArray?, stride: Int) {
        src ?: return
        forLoopSafely(src.size, stride) { i ->
            val y = src[i + 1]
            val z = src[i + 2]
            src[i + 1] = -z
            src[i + 2] = y
        }
    }

    fun rotateY90Degrees(src: FloatArray?, stride: Int) {
        src ?: return
        forLoopSafely(src.size, stride) { i ->
            val x = src[i]
            val z = src[i + 2]
            src[i] = z
            src[i + 2] = -x
        }
    }

    fun Mesh.unlinkPositionsAndNormals() {
        if (positions === normals) normals = normals?.copyOf()
    }

    @Suppress("SameParameterValue")
    fun transformPositionsOrNull(matrix: Matrix4x3, src: FloatArray?, stride: Int): FloatArray? {
        return transformPositions(matrix, src ?: return null, stride)
    }

    @Suppress("SameParameterValue")
    fun transformPositions(matrix: Matrix4x3, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3d.borrow()
        forLoopSafely(src.size, stride) { i ->
            tmp.set(src, i)
            matrix.transformPosition(tmp)
            tmp.get(src, i)
        }
        return src
    }

    fun transformDirectionsOrNull(matrix: Matrix4x3, src: FloatArray?, stride: Int): FloatArray? {
        return transformDirections(matrix, src ?: return null, stride)
    }

    fun transformDirections(matrix: Matrix4x3, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3f.borrow()
        val tmpM = JomlPools.mat3f.borrow()
            .set(matrix).normal()
        forLoopSafely(src.size, stride) { i ->
            tmp.set(src, i)
            tmpM.transform(tmp)
            tmp.get(src, i)
        }
        return src
    }

    @Suppress("SameParameterValue")
    fun transformPositionsOrNull(matrix: Matrix4x3f, src: FloatArray?, stride: Int): FloatArray? {
        return transformPositions(matrix, src ?: return null, stride)
    }

    @Suppress("SameParameterValue")
    fun transformPositions(matrix: Matrix4x3f, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3d.borrow()
        forLoopSafely(src.size, stride) { i ->
            tmp.set(src, i)
            matrix.transformPosition(tmp)
            tmp.get(src, i)
        }
        return src
    }

    fun transformDirectionsOrNull(matrix: Matrix4x3f, src: FloatArray?, stride: Int): FloatArray? {
        return transformDirections(matrix, src ?: return null, stride)
    }

    fun transformDirections(matrix: Matrix4x3f, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3f.borrow()
        val tmpM = JomlPools.mat3f.borrow()
            .set(matrix).normal()
        forLoopSafely(src.size, stride) { i ->
            tmp.set(src, i)
            tmpM.transform(tmp)
            tmp.get(src, i)
        }
        return src
    }

    @Suppress("SameParameterValue")
    fun translatePositionsOrNull(offset: Vector3d, src: FloatArray?, stride: Int): FloatArray? {
        return translatePositions(offset, src ?: return null, stride)
    }

    @Suppress("SameParameterValue")
    fun translatePositions(offset: Vector3d, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3d.borrow()
        forLoopSafely(src.size, stride) { i ->
            tmp.set(src, i).add(offset).get(src, i)
        }
        return src
    }

    @Suppress("SameParameterValue")
    fun scalePositionsOrNull(scale: Vector3f, src: FloatArray?, stride: Int): FloatArray? {
        return scalePositions(scale, src ?: return null, stride)
    }

    @Suppress("SameParameterValue")
    fun scalePositions(scale: Vector3f, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3d.borrow()
        forLoopSafely(src.size, stride) { i ->
            tmp.set(src, i).mul(scale).get(src, i)
        }
        return src
    }
}