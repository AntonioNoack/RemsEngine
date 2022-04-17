package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.GFX
import me.anno.gpu.pipeline.PipelineStage
import me.anno.maths.Maths.TAUf
import org.joml.Matrix4x3d
import kotlin.math.cos
import kotlin.math.sin

// todo draw highlighted stuff instanced
object PlaneShapes {

    val circleBuffer = Mesh()

    init {

        // create circle buffer

        val slices = 15
        val positions = FloatArray(slices * 3 * 2)
        val indices = IntArray(slices * 6)
        val f = 0.5f
        for (i in 0 until slices) {

            val angle = i * TAUf / slices
            val x = cos(angle)
            val z = sin(angle)

            positions[i * 6 + 0] = x
            positions[i * 6 + 1] = 0f
            positions[i * 6 + 2] = z
            positions[i * 6 + 3] = x * f
            positions[i * 6 + 4] = 0f
            positions[i * 6 + 5] = z * f

            val i0 = i * 2
            val i1 = i * 2 + 1
            val i2 = (i * 2 + 2) % (slices * 2)
            val i3 = (i * 2 + 3) % (slices * 2)

            indices[i * 4 + 0] = i0
            indices[i * 4 + 0] = i1
            indices[i * 4 + 0] = i2

            indices[i * 4 + 0] = i2
            indices[i * 4 + 0] = i3
            indices[i * 4 + 0] = i0

        }

        circleBuffer.positions = positions
        circleBuffer.indices = indices
        circleBuffer.checkCompleteness()

    }

    val buffer = PipelineStage.meshInstanceBuffer

    var size = 0

    val transform = Matrix4x3d()

    fun drawCircle(entity: Entity, color: Int) {
        drawCircle(entity.transform.drawTransform, color)
    }

    fun drawCircle(m: Matrix4x3d, color: Int) {
        drawCircle(m.m30(), m.m31(), m.m32(), color)
    }

    @Suppress("UNUSED_PARAMETER")
    fun drawCircle(x: Double, y: Double, z: Double, color: Int) {
        /*val stride = buffer.attributes[0].stride
        val buffer = buffer.nioBuffer!!
        buffer.limit(buffer.capacity())
        buffer.position(stride * size)
        transform.identity()
        transform.setTranslation(x, y, z)
        transform.scale(10.0)
        M4x3Delta.m4x3delta(transform, RenderView.camPosition, RenderView.worldScale, buffer)
        buffer.putInt(color)
        if (++size >= instancedBatchSize) {
            finish()
        }*/
    }

    // todo doesn't work yet
    fun finish() {
        if (size <= 0) return
        GFX.check()
        val shader = pbrModelShader.value
        shader.use()
        val mesh = circleBuffer
        val material = defaultMaterial
        // init shader
        val cameraMatrix = RenderView.cameraMatrix
        GFX.shaderColor(shader, "tint", -1)
        shader.m4x4("transform", cameraMatrix)
        shader.v3f("ambientLight", 1f)
        shader.v1b("hasVertexColors", mesh.hasVertexColors)
        shader.v1b("hasAnimation", false)
        shader.v1i("numberOfLightsPtr", 0)
        material.defineShader(shader)
        // update buffer
        buffer.isUpToDate = false
        buffer.ensureBufferWithoutResize()
        // draw it
        mesh.drawInstanced(shader, 0, buffer)
        size = 0
        GFX.check()
    }

}