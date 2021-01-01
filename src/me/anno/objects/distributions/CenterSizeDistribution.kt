package me.anno.objects.distributions

import me.anno.objects.InspectableVector
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

// todo rotate non-uniform shapes (all but spheres)
abstract class CenterSizeDistribution(
    displayName: String, description: String,
    val center: Vector4f, val size: Vector4f,
    val rotation: Vector3f?
) :
    Distribution(displayName, description) {

    fun Float.transform() = this * size.x + center.x
    fun Vector2f.transform() = mul(size.x, size.y).add(center.x, center.y)
    fun Vector3f.transform() = mul(size.x, size.y, size.z).add(center.x, center.y, center.z)
    fun Vector4f.transform() = mul(size).add(center)

    override fun onDraw(stack: Matrix4fArrayList) {
        // draw a sphere
        stack.pushMatrix()
        stack.translate(center.x, center.y, center.z)
        stack.scale(size.x, size.y, size.z)
        drawTransformed(stack)
        stack.popMatrix()
    }

    abstract fun drawTransformed(stack: Matrix4fArrayList)

    override fun nextV1(): Float {
        return (random.nextFloat() - 0.5f).transform()
    }

    override fun listProperties(): List<InspectableVector> {
        return listOf(
            InspectableVector(center, "Center"),
            InspectableVector(size, "Radius / Size")
        )
    }

}