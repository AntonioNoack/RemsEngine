package me.anno.mesh.gltf

import de.javagl.jgltf.model.impl.DefaultCameraModel
import de.javagl.jgltf.viewer.ExternalCamera
import org.joml.Matrix4f

class ExternalCameraImpl : ExternalCamera {

    val model = DefaultCameraModel(
        { result ->
            val value = viewMatrix
            System.arraycopy(value, 0, result, 0, 16)
            value
        }, { result, _ ->
            val value = projectionMatrix
            System.arraycopy(value, 0, result, 0, 16)
            value
        }
    )

    private val projectionMatrixInternal = FloatArray(16)

    companion object {
        private val unitMatrix = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }

    override fun getViewMatrix(): FloatArray {
        return unitMatrix
    }

    fun update(matrix4f: Matrix4f) {
        matrix4f.get(projectionMatrixInternal)
    }

    override fun getProjectionMatrix(): FloatArray {
        return projectionMatrixInternal
    }

}