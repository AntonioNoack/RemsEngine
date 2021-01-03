package me.anno.objects.distributions

import me.anno.language.translation.Dict
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class SphereHullDistribution(center: Vector4f, size: Vector4f, rotation: Vector4f = Vector4f()) : CenterSizeDistribution(
    Dict["Sphere Hull", "obj.dist.sphere.hull"],
    Dict["Distributes points from the hull of a sphere or circle, not from the volume", "obj.dist.sphere.hull.desc"],
    center, size, rotation
) {

    constructor() : this(0f, 1f)
    constructor(center: Vector2f, size: Float) : this(Vector4f(center, 0f, 0f), Vector4f(size))
    constructor(center: Vector3f, size: Float) : this(Vector4f(center, 0f), Vector4f(size))
    constructor(center: Vector2f, size: Vector2f) : this(Vector4f(center, 0f, 0f), Vector4f(size, 0f, 0f))
    constructor(center: Float, size: Float) : this(Vector3f(center), size)

    override fun nextV2(): Vector2f {
        return Vector2f(
            random.nextFloat() - 0.5f,
            random.nextFloat() - 0.5f
        ).mul(size).normalize().transform()
    }

    override fun nextV3(): Vector3f {
        return Vector3f(
            random.nextFloat() - 0.5f,
            random.nextFloat() - 0.5f,
            random.nextFloat() - 0.5f
        ).mul(size).normalize().transform()
    }

    override fun drawTransformed(stack: Matrix4fArrayList, color: Vector4f) {
        drawSphere(stack, color, 1f)
    }

    override fun getClassName() = "SphereHullDistribution"

}