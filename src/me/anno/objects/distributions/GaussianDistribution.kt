package me.anno.objects.distributions

import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class GaussianDistribution(
    center: Vector4f,
    size: Vector4f
) : CenterSizeDistribution(
    "Gaussian", "Gaussian- or Normal Distribution; sum of many small effects",
    center, size, null
) {

    constructor() : this(Vector4f(), Vector4f())
    constructor(center: Float, stdDeviation: Float) : this(Vector4f(center), Vector4f(stdDeviation))
    constructor(center: Vector2f, stdDeviation: Vector2f) : this(
        Vector4f(center, 0f, 0f),
        Vector4f(stdDeviation, 0f, 0f)
    )

    constructor(center: Vector3f, stdDeviation: Vector3f) : this(Vector4f(center, 0f), Vector4f(stdDeviation, 0f))

    override fun nextV1(): Float {
        return random.nextGaussian().toFloat().transform()
    }

    override fun nextV2(): Vector2f {
        return Vector2f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).transform()
    }

    override fun nextV3(): Vector3f {
        return Vector3f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).transform()
    }

    override fun nextV4(): Vector4f {
        return Vector4f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).transform()
    }

    override fun drawTransformed(stack: Matrix4fArrayList) {
        val i0 = 0.68f
        val i1 = 0.95f
        val i2 = 0.99f
        drawSphere(stack, 1f)
        stack.scale(2f)
        drawSphere(stack, (i1 - i0) / i0)
        stack.scale(3f / 2f)
        drawSphere(stack, (i2 - i1) / i0)
    }

    override fun getClassName() = "GaussianDistribution"

}