package me.anno.objects.distributions

import me.anno.objects.InspectableVector
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class ConstantDistribution(val center: Vector4f) : Distribution("Constant", "Always the same value") {

    constructor() : this(0f)
    constructor(center: Float) : this(Vector4f(center))
    constructor(center: Vector2f) : this(Vector4f(center.x, center.y, center.x, center.y))
    constructor(center: Vector3f) : this(Vector4f(center, 0f))

    override fun nextV1(): Float = center.x

    override fun nextV2(): Vector2f {
        return Vector2f(center.x, center.y)
    }

    override fun nextV3(): Vector3f {
        return Vector3f(center.x, center.y, center.z)
    }

    override fun nextV4(): Vector4f {
        return Vector4f(center)
    }

    override fun listProperties(): List<InspectableVector> {
        return listOf(
            InspectableVector(center, "Center")
        )
    }

    override fun getClassName() = "ConstantDistribution"

}