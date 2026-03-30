package me.anno.maths.noise

import me.anno.maths.Maths.fract
import org.joml.Vector2f

class PhacelleHash(
    val kx: Float,
    val ky: Float,
) {

    constructor() : this(0.3183099f, 0.3678794f)

    operator fun get(px: Float, py: Float, dst: Vector2f): Vector2f {
        val xx = px * kx + ky
        val xy = py * ky + kx
        val base = 16f * fract(xx * xy * (xx + xy))
        return dst.set(
            fract(kx * base) * 2f - 1f,
            fract(ky * base) * 2f - 1f,
        )
    }
}