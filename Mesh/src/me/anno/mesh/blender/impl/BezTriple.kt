package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData
import org.joml.Vector3f

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_curve_types.h#L105
 * */
class BezTriple(ptr: ConstructorData) : BlendData(ptr) {

    val offset = getOffset("vec[3][3]")

    val handle1Point get() = Vector3f(f32s(offset, 3))
    val controlPoint get() = Vector3f(f32s(offset + 12, 3))
    val handle2Point get() = Vector3f(f32s(offset + 24, 3))
    val controlKfIndex get() = f32(offset + 12)
    val controlKfValue get() = f32(offset + 16)

    // vec[3][3]: float, alfa: float, weight: float, radius: float, ipo: char, h1: uchar, h2: uchar,
    // f1: uchar, f2: uchar, f3: uchar, hide: char, easing: char, back: float,
    // amplitude: float, period: float, auto_handle_type: char

    override fun toString(): String {
        return "BezTriple { $handle1Point $controlPoint $handle2Point }"
    }
}