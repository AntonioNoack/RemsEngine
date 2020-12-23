package me.anno.objects.distributions

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class UniformDistribution(center: Vector4f, size: Vector4f) :
    CenterSizeDistribution(
        "Uniform", "Selects points from the cuboid shape randomly, uniformly",
        center, size) {

    constructor(): this(-1f, 1f)
    constructor(min: Float, max: Float) : this(Vector4f(min), Vector4f(max))
    constructor(min: Vector2f, max: Vector2f) : this(Vector4f(min.x, min.y, min.x, min.y), Vector4f(max.x, max.y, max.x, max.y))
    constructor(min: Vector3f, max: Vector3f) : this(Vector4f(min, 0f), Vector4f(max, 0f))

    override fun nextV2(): Vector2f {
        return Vector2f(
                random.nextFloat(),
                random.nextFloat()
        ).mul(size).add(center)
    }

    override fun nextV3(): Vector3f {
        return Vector3f(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat()
        ).mul(size).add(center)
    }

    override fun nextV4(): Vector4f {
        return Vector4f(
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat(),
                random.nextFloat()
        ).mul(size).add(center)
    }

    override fun getClassName() = "UniformDistribution"

}