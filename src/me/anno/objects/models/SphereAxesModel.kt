package me.anno.objects.models

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.Maths.pow
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object SphereAxesModel {

    val sphereAxesModels = Array(5) { lazy { createLineModel(it) } }

    private fun createLineModel(sides0: Int): StaticBuffer {

        val attributes = listOf(
            Attribute("attr0", 3)
        )

        val sideCount = pow(2f, sides0 + 3f).toInt()

        val vertexCount = sideCount * 9 * 2

        val buffer = StaticBuffer(
            attributes,
            vertexCount
        )

        fun addAxis(func: (x: Float, y: Float) -> Vector3f) {
            val zero = func(1f, 0f)
            buffer.put(zero)
            for (i in 1 until sideCount) {
                val angle = i * 6.2830f / sideCount
                val v = func(cos(angle), sin(angle))
                buffer.put(v)
                buffer.put(v)
            }
            buffer.put(zero)
        }

        val s = sqrt(0.5f)

        // x y z
        addAxis { x, y -> Vector3f(x, y, 0f) }
        addAxis { x, y -> Vector3f(0f, x, y) }
        addAxis { x, y -> Vector3f(x, 0f, y) }

        // xy yz zx
        addAxis { x, y -> Vector3f(x, y*s, +y*s) }
        addAxis { x, y -> Vector3f(x, y*s, -y*s) }
        addAxis { x, y -> Vector3f(y*s, x, +y*s) }
        addAxis { x, y -> Vector3f(y*s, x, -y*s) }
        addAxis { x, y -> Vector3f(y*s, +y*s, x) }
        addAxis { x, y -> Vector3f(y*s, -y*s, x) }

        return buffer

    }

}