package me.anno.graph.visual.render.effects.framegen

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.Shader
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

class SavedCameraState {

    private val cameraPosition = Vector3d()
    private val cameraDirection = Vector3f()
    private val cameraRotation = Quaternionf()
    private val cameraMatrixInv = Matrix4f()
    private val tmp = Matrix4f()

    fun bind(shader: Shader) {
        val p0 = cameraPosition
        val p1 = RenderState.cameraPosition
        val cameraMatrixI = RenderState.cameraMatrix
            .translate((p0.x - p1.x).toFloat(), (p0.y - p1.y).toFloat(), (p0.z - p1.z).toFloat(), tmp)
        shader.m4x4("cameraMatrixI", cameraMatrixI)
        DepthTransforms.bindDepthUniforms(shader, cameraDirection, cameraRotation, cameraMatrixInv)
    }

    fun save() {
        cameraPosition.set(RenderState.cameraPosition)
        cameraRotation.set(RenderState.cameraRotation)
        cameraDirection.set(RenderState.cameraDirection)
        cameraMatrixInv.set(RenderState.cameraMatrixInv)
    }
}