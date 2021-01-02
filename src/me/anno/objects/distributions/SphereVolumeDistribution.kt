package me.anno.objects.distributions

import me.anno.language.translation.Dict
import org.joml.Matrix4fArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class SphereVolumeDistribution(center: Vector4f, size: Vector4f) : CenterSizeDistribution(
    Dict["Sphere Volume", "obj.dist.sphere.volume"],
    Dict["Points from the inside of the sphere", "obj.dist.sphere.volume.desc"],
    center, size, null
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

    override fun drawTransformed(stack: Matrix4fArrayList) {
        drawSphere(stack)
    }

    override fun getClassName() = "SphereVolumeDistribution"

}