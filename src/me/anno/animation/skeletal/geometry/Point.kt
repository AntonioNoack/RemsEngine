package me.anno.animation.skeletal.geometry

import me.anno.utils.Maths.clamp
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.roundToInt

class Point(x: Float, y: Float, z: Float, var u: Float, var v: Float): Vector3f(x,y,z) {

    /*fun clear() {
        position.set(ox, oy, oz)
    }*/

    fun write(dos: DataOutputStream) {
        dos.writeShort(mapXYZ(x))
        dos.writeShort(mapXYZ(y))
        dos.writeShort(mapXYZ(z))
        dos.writeShort(mapUV(u))
        dos.writeShort(mapUV(v))
    }

    companion object {

        fun mapUV(u: Float) = clamp((u / 65535f).roundToInt(), 0, 65535)
        fun unmapUV(i: Int) = i / 65535f
        fun mapXYZ(x: Float) = clamp(((x + 10f) * 65535f / 20f).roundToInt(), 0, 65535) // 0.3mm resolution
        fun unmapXYZ(i: Int) = i * 20f / 65535f - 10f

        fun read(dis: DataInputStream): Point {
            return Point(
                    unmapXYZ(dis.readUnsignedShort()),
                    unmapXYZ(dis.readUnsignedShort()),
                    unmapXYZ(dis.readUnsignedShort()),
                    unmapUV(dis.readUnsignedShort()),
                    unmapUV(dis.readUnsignedShort())
            )
        }

    }

}