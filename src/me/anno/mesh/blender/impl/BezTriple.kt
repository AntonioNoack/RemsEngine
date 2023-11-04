package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import org.joml.Vector3f
import java.nio.ByteBuffer

/**
 * https://github.com/blender/blender/blob/master/source/blender/makesdna/DNA_curve_types.h#L105
 * */
class BezTriple(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position) {

    val offset = getOffset("vec[3][3]")

    val handle1Point get() = Vector3f(floats(offset, 3))
    val controlPoint get() = Vector3f(floats(offset + 12, 3))
    val handle2Point get() = Vector3f(floats(offset + 24, 3))
    val controlKfIndex get() = float(offset + 12)
    val controlKfValue get() = float(offset + 16)

    // vec[3][3]: float, alfa: float, weight: float, radius: float, ipo: char, h1: uchar, h2: uchar,
    // f1: uchar, f2: uchar, f3: uchar, hide: char, easing: char, back: float,
    // amplitude: float, period: float, auto_handle_type: char

    override fun toString(): String {
        return "BezTriple { $handle1Point $controlPoint $handle2Point }"
    }
}