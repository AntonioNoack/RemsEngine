package me.anno.gpu.geometry

import me.anno.utils.maths.Maths
import org.joml.Vector2f
import org.joml.Vector3f

class Point3D(val pt: Vector3f, val normal: Vector3f, val uv: Vector2f) {
    fun mix(b: Point3D, factor: Float): Point3D = Point3D(
        Maths.mix(pt, b.pt, factor),
        Maths.mix(normal, b.normal, factor),
        Maths.mix(uv, b.uv, factor)
    )
}