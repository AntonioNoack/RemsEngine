package me.anno.objects.distributions

import me.anno.gpu.GFX.toRadians
import me.anno.objects.inspectable.InspectableVector
import org.joml.*
import kotlin.math.cos
import kotlin.math.sin

abstract class CenterSizeDistribution(
    displayName: String, description: String,
    val center: Vector4f, val size: Vector4f,
    val rotation: Vector4f
) :
    Distribution(displayName, description) {

    fun Float.transform() = this * size.x + center.x
    fun Vector2f.transform(): Vector2f = rotate(mul(Vector2f(size.x, size.y))).add(center.x, center.y)
    fun Vector3f.transform(): Vector3f = rotate(mul(Vector3f(size.x, size.y, size.z))).add(center.x, center.y, center.z)
    fun Vector4f.transform(): Vector4f = rotate(mul(size)).add(center)

    fun rotate(vector: Vector2f): Vector2f {
        val angleDegrees = rotation.x + rotation.y + rotation.z
        if (angleDegrees == 0f) return vector
        val angle = toRadians(angleDegrees)
        val cos = cos(angle)
        val sin = sin(angle)
        return Vector2f(
            cos * vector.x - sin * vector.y,
            sin * vector.x + cos * vector.y
        )
    }

    fun rotate(vector: Vector3f): Vector3f {
        if (rotation.x == 0f && rotation.y == 0f && rotation.z == 0f) return vector
        val quat = Quaternionf()
        if (rotation.y != 0f) quat.rotateY(toRadians(rotation.y))
        if (rotation.x != 0f) quat.rotateX(toRadians(rotation.x))
        if (rotation.z != 0f) quat.rotateZ(toRadians(rotation.z))
        return quat.transform(vector)
    }

    fun rotate(vector: Vector4f): Vector4f {
        return Vector4f(Vector3f(vector.x, vector.y, vector.z), vector.w)
    }

    override fun onDraw(stack: Matrix4fArrayList, color: Vector4f) {
        // draw a sphere
        stack.pushMatrix()
        stack.translate(center.x, center.y, center.z)
        if (rotation.y != 0f) stack.rotateY(toRadians(rotation.y))
        if (rotation.x != 0f) stack.rotateX(toRadians(rotation.x))
        if (rotation.z != 0f) stack.rotateZ(toRadians(rotation.z))
        stack.scale(size.x, size.y, size.z)
        drawTransformed(stack, color)
        stack.popMatrix()
    }

    abstract fun drawTransformed(stack: Matrix4fArrayList, color: Vector4f)

    override fun nextV1(): Float {
        return (random.nextFloat() - 0.5f).transform()
    }

    override fun listProperties(): List<InspectableVector> {
        return listOf(
            InspectableVector(center, "Center"),
            InspectableVector(size, "Radius / Size"),
            InspectableVector(rotation, "Rotation", "", true)
        )
    }

}