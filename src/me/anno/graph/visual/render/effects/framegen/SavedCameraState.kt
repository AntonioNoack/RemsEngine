package me.anno.graph.visual.render.effects.framegen

import me.anno.engine.ui.render.RenderState
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.Shader
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Vector3d

class SavedCameraState {

    private var worldScale = 1.0
    private val cameraPosition = Vector3d()
    private val cameraDirection = Vector3d()
    private val cameraRotation = Quaterniond()
    private val cameraMatrixInv = Matrix4f()
    private val tmp = Matrix4f()

    fun bind(shader: Shader) {
        val p0 = cameraPosition
        val p1 = RenderState.cameraPosition
        val scale = RenderState.worldScale
        val cameraMatrixI = RenderState.cameraMatrix
            .scale((RenderState.worldScale / worldScale).toFloat(), tmp)
            .translate(
                ((p0.x - p1.x) * scale).toFloat(),
                ((p0.y - p1.y) * scale).toFloat(),
                ((p0.z - p1.z) * scale).toFloat(),
            )
        shader.m4x4("cameraMatrixI", cameraMatrixI)
        DepthTransforms.bindDepthUniforms(shader, cameraDirection, cameraRotation, cameraMatrixInv)
    }

    fun save() {
        worldScale = RenderState.worldScale
        cameraPosition.set(RenderState.cameraPosition)
        cameraRotation.set(RenderState.cameraRotation)
        cameraDirection.set(RenderState.cameraDirection)
        cameraMatrixInv.set(RenderState.cameraMatrixInv)
    }
}