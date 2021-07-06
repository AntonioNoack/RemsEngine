package me.anno.objects.distributions

import org.joml.*

class SphereVolumeDistribution(center: Vector4f, size: Vector4f, rotation: Vector4f = Vector4f()) :
    CenterSizeDistribution(
        "Sphere",
        "Points from the inside of the sphere", "obj.dist.sphere",
        center, size, rotation
    ) {

    constructor() : this(0f, 1f)
    constructor(center: Vector2f, size: Float) : this(Vector4f(center, 0f, 0f), Vector4f(size))
    constructor(center: Vector3f, size: Float) : this(Vector4f(center, 0f), Vector4f(size))
    constructor(center: Vector2f, size: Vector2f) : this(Vector4f(center, 0f, 0f), Vector4f(size, 0f, 0f))
    constructor(center: Float, size: Float) : this(Vector3f(center), size)

    override fun nextV2(): Vector2f {
        var x: Float
        var y: Float
        do {
            x = random.nextFloat() * 2f - 1f
            y = random.nextFloat() * 2f - 1f
        } while (x * x + y * y > 1f)
        return Vector2f(x, y).transform()
    }

    override fun nextV3(): Vector3f {
        var x: Float
        var y: Float
        var z: Float
        do {
            x = random.nextFloat() * 2f - 1f
            y = random.nextFloat() * 2f - 1f
            z = random.nextFloat() * 2f - 1f
        } while (x * x + y * y + z * z > 1f)
        return Vector3f(x, y, z).transform()
    }

    override fun drawTransformed(stack: Matrix4fArrayList, color: Vector4fc) {
        drawSphere(stack, color, 1f)
    }

    override val className get() = "SphereDistribution"

}