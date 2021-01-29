package me.anno.utils.io

import org.joml.Vector3f
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

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

    fun DataOutputStream.writeVec3(vector: Vector3f) {
        writeFloat(vector.x)
        writeFloat(vector.y)
        writeFloat(vector.z)
    }

    fun DataInputStream.readVec3(): Vector3f {
        return Vector3f(readFloat(), readFloat(), readFloat())
    }

}