package me.anno.io

import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

@Suppress("unused")
object Writing {

    fun DataInputStream.readNValues(array: ByteArray) {
        var i = 0
        val l = array.size
        while (i < l) {
            val dl = read(array, i, l - i)
            if (dl < 0) throw EOFException()
            i += dl
        }
    }

    fun DataInputStream.readNValues(array: FloatArray) {
        for (i in array.indices) {
            array[i] = readFloat()
        }
    }

    fun DataOutputStream.writeNValues(array: ByteArray) {
        write(array)
    }

    fun DataOutputStream.writeNValues(array: FloatArray) {
        for (f in array) writeFloat(f)
    }

    fun DataOutputStream.writeVec3(v: Vector3f) {
        writeFloat(v.x)
        writeFloat(v.y)
        writeFloat(v.z)
    }

    fun DataOutputStream.writeQuat(v: Quaternionf) {
        writeFloat(v.x)
        writeFloat(v.y)
        writeFloat(v.z)
        writeFloat(v.w)
    }

    fun DataInputStream.readVec3(dst: Vector3f): Vector3f {
        return dst.set(readFloat(), readFloat(), readFloat())
    }

    fun DataInputStream.readQuat(dst: Quaternionf): Quaternionf {
        return dst.set(readFloat(), readFloat(), readFloat(), readFloat())
    }
}