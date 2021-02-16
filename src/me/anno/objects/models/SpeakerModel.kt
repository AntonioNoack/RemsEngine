package me.anno.objects.models

import me.anno.gpu.GFX
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.objects.Transform
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.Maths
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object SpeakerModel {

    private const val speakerEdges = 64
    val speakerModel = StaticBuffer(
        listOf(
            Attribute("attr0", 3),
            Attribute("attr1", 2)
        ), speakerEdges * 3 * 2 + 4 * 2 * 2
    ).apply {

        fun addLine(r0: Float, d0: Float, r1: Float, d1: Float, dx: Int, dy: Int) {
            put(r0 * dx, r0 * dy, d0, 0f, 0f)
            put(r1 * dx, r1 * dy, d1, 0f, 0f)
        }

        fun addRing(radius: Float, depth: Float, edges: Int) {
            val dr = (Math.PI * 2 / edges).toFloat()
            fun putPoint(i: Int) {
                val angle1 = dr * i
                put(sin(angle1) * radius, cos(angle1) * radius, depth, 0f, 0f)
            }
            putPoint(0)
            for (i in 1 until edges) {
                putPoint(i)
                putPoint(i)
            }
            putPoint(0)
        }

        val scale = 0.5f

        addRing(0.45f * scale, 0.02f * scale, speakerEdges)
        addRing(0.50f * scale, 0.01f * scale, speakerEdges)
        addRing(0.80f * scale, 0.30f * scale, speakerEdges)

        val dx = listOf(0, 0, 1, -1)
        val dy = listOf(1, -1, 0, 0)
        for (i in 0 until 4) {
            addLine(0.45f * scale, 0.02f * scale, 0.50f * scale, 0.01f * scale, dx[i], dy[i])
            addLine(0.50f * scale, 0.01f * scale, 0.80f * scale, 0.30f * scale, dx[i], dy[i])
        }

        lines()

    }

    fun drawSpeakers(
        stack: Matrix4fArrayList,
        color: Vector4f,
        is3D: Boolean,
        amplitude: Float
    ) {
        if (GFX.isFinalRendering) return
        color.w = Maths.clamp(color.w * 0.5f * abs(amplitude), 0f, 1f)
        if (is3D) {
            val r = 0.85f
            stack.translate(r, 0f, 0f)
            Grid.drawBuffer(stack, color, speakerModel)
            stack.translate(-2 * r, 0f, 0f)
            Grid.drawBuffer(stack, color, speakerModel)
        } else {
            // mark the speaker with yellow,
            // and let it face upwards (+y) to symbolize, that it's global
            color.z *= 0.8f // yellow
            stack.rotate(-1.5708f, Transform.xAxis)
            Grid.drawBuffer(stack, color, speakerModel)
        }
    }

    fun destroy(){
        speakerModel.destroy()
    }

}