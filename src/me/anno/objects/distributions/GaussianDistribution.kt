package me.anno.objects.distributions

import me.anno.language.translation.Dict
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.sqrt

class GaussianDistribution(center: Vector4f, size: Vector4f, rotation: Vector4f = Vector4f()) : CenterSizeDistribution(
    Dict["Gaussian", "obj.dist.gaussian"],
    Dict["Gaussian- or Normal Distribution; sum of many small effects", "obj.dist.gaussian.desc"],
    center, size, rotation
) {

    constructor() : this(Vector4f(), Vector4f())
    constructor(center: Float, stdDeviation: Float) : this(Vector4f(center), Vector4f(stdDeviation))
    constructor(center: Vector2f, stdDeviation: Vector2f) : this(
        Vector4f(center, 0f, 0f),
        Vector4f(stdDeviation, 0f, 0f)
    )

    constructor(center: Vector3f, stdDeviation: Vector3f) : this(Vector4f(center, 0f), Vector4f(stdDeviation, 0f))

    override fun nextV1(): Float {
        return (random.nextGaussian().toFloat() * gaussianScale).transform()
    }

    override fun nextV2(): Vector2f {
        return Vector2f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).mul(gaussianScale).transform()
    }

    override fun nextV3(): Vector3f {
        return Vector3f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).mul(gaussianScale).transform()
    }

    override fun nextV4(): Vector4f {
        return Vector4f(
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat(),
            random.nextGaussian().toFloat()
        ).mul(gaussianScale).transform()
    }

    override fun drawTransformed(stack: Matrix4fArrayList, color: Vector4f) {
        val i0 = 0.68f
        val i1 = 0.95f
        val i2 = 0.99f
        stack.scale(gaussianScale)
        drawSphere(stack, color, 1f)
        stack.scale(2f)
        drawSphere(stack, color, sqrt((i1 - i0) / i0))
        stack.scale(3f / 2f)
        drawSphere(stack, color, sqrt((i2 - i1) / i0))
    }

    override fun getClassName() = "GaussianDistribution"

    companion object {
        const val gaussianScale = 0.5f // to be better comparable to sphere hull and sphere volume
    }

}