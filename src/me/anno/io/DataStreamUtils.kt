package me.anno.io

import me.anno.io.Streams.readBE32F
import me.anno.io.Streams.writeBE32F
import org.joml.Quaternionf
import org.joml.Vector3f
import java.io.InputStream
import java.io.OutputStream

@Suppress("unused")
object DataStreamUtils {

    fun OutputStream.writeVec3(value: Vector3f) {
        writeBE32F(value.x)
        writeBE32F(value.y)
        writeBE32F(value.z)
    }

    fun OutputStream.writeQuat(value: Quaternionf) {
        writeBE32F(value.x)
        writeBE32F(value.y)
        writeBE32F(value.z)
        writeBE32F(value.w)
    }

    fun InputStream.readVec3(dst: Vector3f): Vector3f {
        return dst.set(readBE32F(), readBE32F(), readBE32F())
    }

    fun InputStream.readQuat(dst: Quaternionf): Quaternionf {
        return dst.set(readBE32F(), readBE32F(), readBE32F(), readBE32F())
    }
}