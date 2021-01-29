package me.anno.objects.models

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.types.Vectors.minus
import me.anno.utils.types.Vectors.plus
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object SphereModel {

    val sphereModels = Array(5) { lazy { createModel(it) } }
    val sphereLineModels = Array(5) { lazy { createLineModel(it) } }

    private fun createModel(subdivisions: Int): StaticBuffer {

        val attributes = listOf(
            Attribute("attr0", 3)
        )

        val triangleCount = 20 shl (subdivisions * 2)
        val vertexCount = triangleCount * 3

        val buffer = StaticBuffer(
            attributes,
            vertexCount
        )

        val z = 0.447215f

        fun addTriangle(a: Vector3f, b: Vector3f, c: Vector3f, sub: Int = 0) {
            if (sub < subdivisions) {
                // subdivide further
                val ab = (a + b).normalize()
                val bc = (b + c).normalize()
                val ca = (c + a).normalize()
                addTriangle(a, ab, ca, sub + 1)
                addTriangle(b, ab, bc, sub + 1)
                addTriangle(c, bc, ca, sub + 1)
                addTriangle(ab, bc, ca, sub + 1)
            } else {
                // keep the normals the same
                if((a-b).cross(a-c).dot(a) < 0f){
                    buffer.put(a)
                    buffer.put(b)
                    buffer.put(c)
                } else {
                    buffer.put(a)
                    buffer.put(c)
                    buffer.put(b)
                }
            }
        }

        val p0 = Vector3f(0f,1f,0f)
        val p1 = Vector3f(0f,-1f,0f)

        val ring0 = Array(8){
            val angle = (it * 0.2 * 2 * PI).toFloat()
            Vector3f(cos(angle), z, sin(angle)).normalize()
        }

        val ring1 = Array(8){
            val angle = ((it + 0.5) * 0.2 * 2 * PI).toFloat()
            Vector3f(cos(angle), -z, sin(angle)).normalize()
        }

        for(i in 0 until 5){
            // top hull
            addTriangle(ring0[i], ring0[i+1], p0)
            // in-between
            addTriangle(ring0[i], ring0[i+1], ring1[i])
            addTriangle(ring1[i], ring1[i+1], ring0[i+1])
            // bottom hull
            addTriangle(ring1[i], ring1[i+1], p1)
        }

        return buffer

    }

    private fun createLineModel(subdivisions: Int): StaticBuffer {

        val attributes = listOf(
            Attribute("attr0", 3)
        )

        val triangleCount = 20 shl (subdivisions * 2)
        val vertexCount = triangleCount * 3 * 2

        val buffer = StaticBuffer(
            attributes,
            vertexCount
        )

        val z = 0.447215f

        fun addTriangle(a: Vector3f, b: Vector3f, c: Vector3f, sub: Int = 0) {
            if (sub < subdivisions) {
                // subdivide further
                val ab = (a + b).normalize()
                val bc = (b + c).normalize()
                val ca = (c + a).normalize()
                addTriangle(a, ab, ca, sub + 1)
                addTriangle(b, ab, bc, sub + 1)
                addTriangle(c, bc, ca, sub + 1)
                addTriangle(ab, bc, ca, sub + 1)
            } else {
                // I don't care about the normals
                buffer.put(a)
                buffer.put(b)
                buffer.put(b)
                buffer.put(c)
                buffer.put(c)
                buffer.put(a)
            }
        }

        val p0 = Vector3f(0f,1f,0f)
        val p1 = Vector3f(0f,-1f,0f)

        val ring0 = Array(8){
            val angle = (it * 0.2 * 2 * PI).toFloat()
            Vector3f(cos(angle), z, sin(angle)).normalize()
        }

        val ring1 = Array(8){
            val angle = ((it + 0.5) * 0.2 * 2 * PI).toFloat()
            Vector3f(cos(angle), -z, sin(angle)).normalize()
        }

        for(i in 0 until 5){
            // top hull
            addTriangle(ring0[i], ring0[i+1], p0)
            // in-between
            addTriangle(ring0[i], ring0[i+1], ring1[i])
            addTriangle(ring1[i], ring1[i+1], ring0[i+1])
            // bottom hull
            addTriangle(ring1[i], ring1[i+1], p1)
        }

        return buffer

    }

}