package me.anno.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Material.Companion.defaultMaterial
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFX
import me.anno.gpu.pipeline.PipelineStage
import me.anno.maths.Maths.TAUf
import me.anno.utils.structures.maps.LazyMap
import org.joml.Matrix4x3d
import kotlin.math.cos
import kotlin.math.sin

// todo draw highlighted stuff instanced
object PlaneShapes {

    val circleBuffer = LazyMap<Pair<Int, Boolean>, Mesh> { (slices, inner) ->

        val mesh = Mesh()

        if (inner) {

            val positions = FloatArray(slices * 3 * 2)
            val indices = IntArray(slices * 6)
            val f = 0.5f
            for (i in 0 until slices) {

                val angle = i * TAUf / slices
                val x = cos(angle)
                val z = sin(angle)

                val i6 = i * 6
                positions[i6 + 0] = x
                positions[i6 + 2] = z
                positions[i6 + 3] = x * f
                positions[i6 + 5] = z * f

                val j0 = i * 2
                val j1 = i * 2 + 1
                val j2 = (i * 2 + 2) % (slices * 2)
                val j3 = (i * 2 + 3) % (slices * 2)

                indices[i6 + 0] = j0
                indices[i6 + 1] = j1
                indices[i6 + 2] = j2

                indices[i6 + 3] = j2
                indices[i6 + 4] = j1
                indices[i6 + 5] = j3
            }

            mesh.positions = positions
            mesh.indices = indices
        } else {

            val positions = FloatArray(slices * 3)
            val indices = IntArray((slices - 2) * 3)
            for (i in 0 until slices) {

                val angle = i * TAUf / slices
                val x = cos(angle)
                val z = sin(angle)

                val i3 = i * 3
                positions[i3 + 0] = x
                positions[i3 + 2] = z

                if (i + 2 < slices) {
                    indices[i3 + 0] = i + 1
                    indices[i3 + 2] = i + 2
                }
            }

            mesh.positions = positions
            mesh.indices = indices
        }

        mesh
    }

    val buffer = PipelineStage.instancedBuffer

    var size = 0

    val transform = Matrix4x3d()

    fun drawCircle(entity: Entity, color: Int) {
        drawCircle(entity.transform.getDrawMatrix(), color)
    }

    fun drawCircle(m: Matrix4x3d, color: Int) {
        drawCircle(m.m30, m.m31, m.m32, color)
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
        val mesh = circleBuffer[15 to true]
        val material = defaultMaterial
        // init shader
        val cameraMatrix = RenderState.cameraMatrix
        shader.v4f("tint", 1f)
        shader.m4x4("transform", cameraMatrix)
        shader.v1i("hasVertexColors", mesh.hasVertexColors)
        shader.v1b("hasAnimation", false)
        shader.v1i("numberOfLightsPtr", 0)
        material.bind(shader)
        // update buffer
        buffer.isUpToDate = false
        buffer.ensureBufferWithoutResize()
        // draw it
        mesh.drawInstanced(shader, 0, buffer, Mesh.drawDebugLines)
        size = 0
        GFX.check()
    }
}