package me.anno.remsstudio.objects.distributions

import me.anno.remsstudio.objects.models.CubemapModel.cubemapLineModel
import me.anno.ui.editor.sceneView.Grid
import me.anno.maths.Maths.max
import org.joml.*
import kotlin.math.abs

class CuboidHullDistribution(center: Vector4f, size: Vector4f, rotation: Vector4f = Vector4f()) :
    CenterSizeDistribution(
        "Cuboid Hull",
        "Selects points from the cuboid hull randomly, uniformly", "obj.dist.cuboidHull",
        center, size, rotation
    ) {

    constructor() : this(0f, 1f)
    constructor(center: Float, size: Float) : this(Vector4f(center), Vector4f(size))
    constructor(center: Vector2f, size: Vector2f) : this(
        Vector4f(center.x, center.y, center.x, center.y),
        Vector4f(size.x, size.y, size.x, size.y)
    )

    constructor(center: Vector3f, size: Vector3f) : this(Vector4f(center, 0f), Vector4f(size, 0f))

    override fun nextV1(): Float {
        val x = random.nextFloat()
        return (if (x > 0.5f) 1f else -1f).transform()
    }

    override fun nextV2(): Vector2f {
        val maxScale = max(abs(scale.x), abs(scale.y))
        var x = (random.nextFloat() * 2f - 1f) * scale.x / maxScale
        var y = (random.nextFloat() * 2f - 1f) * scale.y / maxScale
        if (abs(x) > abs(y)) {
            x = if (x > 0f) +1f else -1f
            y *= maxScale / scale.y // undo scaling
        } else {
            y = if (y > 0f) +1f else -1f
            x *= maxScale / scale.x // undo scaling
        }
        if(x.isNaN()) x = 0f
        if(y.isNaN()) y = 0f
        return Vector2f(
            x, y
        ).transform()
    }

    override fun nextV3(): Vector3f {
        val maxScale = max(abs(scale.x), abs(scale.y), abs(scale.z), 1e-16f)
        var x = (random.nextFloat() * 2f - 1f) * scale.x / maxScale
        var y = (random.nextFloat() * 2f - 1f) * scale.y / maxScale
        var z = (random.nextFloat() * 2f - 1f) * scale.z / maxScale
        val ax = abs(x)
        val ay = abs(y)
        val az = abs(z)
        when (max(ax, max(ay, az))) {
            ax -> {
                x = if (x > 0f) +1f else -1f
                y *= maxScale / scale.y
                z *= maxScale / scale.z
            }
            ay -> {
                y = if (y > 0f) +1f else -1f
                x *= maxScale / scale.x
                z *= maxScale / scale.z
            }
            else -> {
                z = if (z > 0f) +1f else -1f
                x *= maxScale / scale.x
                y *= maxScale / scale.y
            }
        }
        if(x.isNaN()) x = 0f
        if(y.isNaN()) y = 0f
        if(z.isNaN()) z = 0f
        return Vector3f(
            x, y, z
        ).transform()
    }

    override fun nextV4(): Vector4f {
        val maxScale = max(abs(scale.x), abs(scale.y), abs(scale.z), abs(scale.w), 1e-16f)
        var x = (random.nextFloat() * 2f - 1f) * scale.x / maxScale
        var y = (random.nextFloat() * 2f - 1f) * scale.y / maxScale
        var z = (random.nextFloat() * 2f - 1f) * scale.z / maxScale
        var w = (random.nextFloat() * 2f - 1f) * scale.w / maxScale
        val ax = abs(x)
        val ay = abs(y)
        val az = abs(z)
        val aw = abs(w)
        when (max(ax, ay, az, aw)) {
            ax -> {
                x = if (x > 0f) +1f else -1f
                y *= maxScale / scale.y
                z *= maxScale / scale.z
                w *= maxScale / scale.w
            }
            ay -> {
                y = if (y > 0f) +1f else -1f
                x *= maxScale / scale.x
                z *= maxScale / scale.z
                w *= maxScale / scale.w
            }
            az -> {
                z = if (z > 0f) +1f else -1f
                x *= maxScale / scale.x
                y *= maxScale / scale.y
                w *= maxScale / scale.w
            }
            else -> {
                w = if (w > 0f) +1f else -1f
                x *= maxScale / scale.x
                y *= maxScale / scale.y
                z *= maxScale / scale.z
            }
        }
        if(x.isNaN()) x = 0f
        if(y.isNaN()) y = 0f
        if(z.isNaN()) z = 0f
        if(w.isNaN()) w = 0f
        return Vector4f(
            x, y, z, w
        ).transform()
    }

    override fun drawTransformed(stack: Matrix4fArrayList, color: Vector4fc) {
        // draw cube out of lines
        Grid.drawBuffer(stack, color, cubemapLineModel)
    }

    override val className get() = "CuboidHullDistribution"

}