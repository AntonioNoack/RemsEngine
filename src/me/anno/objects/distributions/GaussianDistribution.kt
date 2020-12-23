package me.anno.objects.distributions

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class GaussianDistribution(
    center: Vector4f,
    size: Vector4f
) : CenterSizeDistribution(
    "Gaussian", "Gaussian- or Normal Distribution; sum of many small effects",
    center, size) {

    constructor() : this(Vector4f(), Vector4f())
    constructor(center: Float, stdDeviation: Float) : this(Vector4f(center), Vector4f(stdDeviation))
    constructor(center: Vector2f, stdDeviation: Vector2f) : this(
        Vector4f(center, 0f, 0f),
        Vector4f(stdDeviation, 0f, 0f)
    )

    constructor(center: Vector3f, stdDeviation: Vector3f) : this(Vector4f(center, 0f), Vector4f(stdDeviation, 0f))

    override fun nextV1(): Float {
        return random.nextGaussian().toFloat() * size.x + center.x
    }

    override fun nextV2(): Vector2f {
        return Vector2f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).mul(size).add(center)
    }

    override fun nextV3(): Vector3f {
        return Vector3f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).mul(size).add(center)
    }

    override fun nextV4(): Vector4f {
        return Vector4f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).mul(size).add(center)
    }

    override fun getClassName() = "GaussianDistribution"

}