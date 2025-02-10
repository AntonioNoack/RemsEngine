package me.anno.ecs.components.mesh

import me.anno.utils.pooling.JomlPools
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Transforming a mesh by an affine matrix
 * */
object TransformMesh {

    fun Mesh.transform(matrix: Matrix4x3d): Mesh {
        positions = transformPositionsOrNull(matrix, positions, 3)
        normals = transformDirectionsOrNull(matrix, normals, 3)
        tangents = transformDirectionsOrNull(matrix, tangents, 4)
        invalidateGeometry()
        return this
    }

    fun Mesh.transform(matrix: Matrix4x3f): Mesh {
        positions = transformPositionsOrNull(matrix, positions, 3)
        normals = transformDirectionsOrNull(matrix, normals, 3)
        tangents = transformDirectionsOrNull(matrix, tangents, 4)
        invalidateGeometry()
        return this
    }

    fun Mesh.translate(delta: Vector3d): Mesh {
        positions = translatePositionsOrNull(delta, positions, 3)
        invalidateGeometry()
        return this
    }

    fun Mesh.scale(scale: Vector3f): Mesh {
        positions = scalePositionsOrNull(scale, positions, 3)
        invalidateGeometry()
        return this
    }

    @Suppress("SameParameterValue")
    fun transformPositionsOrNull(matrix: Matrix4x3d, src: FloatArray?, stride: Int): FloatArray? {
        return transformPositions(matrix, src ?: return null, stride)
    }

    @Suppress("SameParameterValue")
    fun transformPositions(matrix: Matrix4x3d, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3d.borrow()
        for (i in 0 until src.size - 2 step stride) {
            tmp.set(src, i)
            matrix.transformPosition(tmp)
            tmp.get(src, i)
        }
        return src
    }

    fun transformDirectionsOrNull(matrix: Matrix4x3d, src: FloatArray?, stride: Int): FloatArray? {
        return transformDirections(matrix, src ?: return null, stride)
    }

    fun transformDirections(matrix: Matrix4x3d, src: FloatArray, stride: Int): FloatArray {
        val tmp = JomlPools.vec3f.borrow()
        val tmpM = JomlPools.mat3f.borrow()
            .set(matrix).normal()
        for (i in 0 until src.size - 2 step stride) {
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
        for (i in 0 until src.size - 2 step stride) {
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
        for (i in 0 until src.size - 2 step stride) {
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
        for (i in 0 until src.size - 2 step stride) {
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
        for (i in 0 until src.size - 2 step stride) {
            tmp.set(src, i).mul(scale).get(src, i)
        }
        return src
    }
}