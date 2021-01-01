package me.anno.objects.models

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ArrowModel {

    val arrowModel by lazy { createModel() }
    val arrowLineModel by lazy { createLineModel() }

    private const val smallHeight = 0.25f
    private val leftTop = Vector2f(-1f, smallHeight)
    private val leftBottom = Vector2f(-1f, -smallHeight)

    private const val center = 0.2f
    private val centerTop = Vector2f(center,smallHeight)
    private val centerBottom = Vector2f(center,-smallHeight)

    private const val arrow = 0.5f
    private val arrowTop = Vector2f(center,arrow)
    private val arrowBottom = Vector2f(center,-arrow)
    private val front = Vector2f(+1f, 0f)

    private fun createModel(): StaticBuffer {

        val attributes = listOf(
            Attribute("attr0", 3)
        )

        val triangleCount = 6
        val vertexCount = triangleCount * 3

        val buffer = StaticBuffer(
            attributes,
            vertexCount
        )

        fun addTriangle(a: Vector2f, b: Vector2f, c: Vector2f) {

            // from both sides
            buffer.put(a)
            buffer.put(0f)
            buffer.put(b)
            buffer.put(0f)
            buffer.put(c)
            buffer.put(0f)
            buffer.put(a)
            buffer.put(0f)
            buffer.put(c)
            buffer.put(0f)
            buffer.put(b)
            buffer.put(0f)

        }

        addTriangle(leftTop, centerBottom, centerTop)
        addTriangle(leftBottom, leftTop, centerBottom)
        addTriangle(arrowTop,arrowBottom,front)

        return buffer

    }

    private fun createLineModel(): StaticBuffer {

        val attributes = listOf(
            Attribute("attr0", 3)
        )

        val vertexCount = 2 * 7

        val buffer = StaticBuffer(
            attributes,
            vertexCount
        )

        fun addLine(a: Vector2f, b: Vector2f){
            buffer.put(a)
            buffer.put(0f)
            buffer.put(b)
            buffer.put(0f)
        }

        addLine(leftBottom, leftTop)
        addLine(leftTop, centerTop)
        addLine(centerTop, arrowTop)
        addLine(arrowTop, front)
        addLine(front, arrowBottom)
        addLine(arrowBottom, centerBottom)
        addLine(centerBottom, leftBottom)

        return buffer

    }

}