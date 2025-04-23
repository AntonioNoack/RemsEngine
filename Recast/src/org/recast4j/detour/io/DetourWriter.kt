/*
Recast4J Copyright (c) 2015 Piotr Piastucki piotr@jtilia.org

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:
1. The origin of this software must not be misrepresented; you must not
 claim that you wrote the original software. If you use this software
 in a product, an acknowledgment in the product documentation would be
 appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be
 misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.recast4j.detour.io

import me.anno.io.Streams.writeBE16
import me.anno.io.Streams.writeBE32
import me.anno.io.Streams.writeBE64
import me.anno.io.Streams.writeLE16
import me.anno.io.Streams.writeLE32
import me.anno.io.Streams.writeLE64
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBf
import org.joml.Vector3f
import java.io.OutputStream
import java.nio.ByteOrder

abstract class DetourWriter {

    protected fun writeF32(stream: OutputStream, value: Float, order: ByteOrder) {
        writeI32(stream, value.toRawBits(), order)
    }

    protected fun write(stream: OutputStream, value: Vector3f, order: ByteOrder) {
        writeF32(stream, value.x, order)
        writeF32(stream, value.y, order)
        writeF32(stream, value.z, order)
    }

    protected fun write(stream: OutputStream, value: AABBf, order: ByteOrder) {
        writeF32(stream, value.minX, order)
        writeF32(stream, value.minY, order)
        writeF32(stream, value.minZ, order)
        writeF32(stream, value.maxX, order)
        writeF32(stream, value.maxY, order)
        writeF32(stream, value.maxZ, order)
    }

    protected fun writeI16(stream: OutputStream, value: Short, order: ByteOrder) {
        if (order == ByteOrder.BIG_ENDIAN) {
            stream.writeBE16(value.toInt())
        } else {
            stream.writeLE16(value.toInt())
        }
    }

    protected fun writeI64(stream: OutputStream, value: Long, order: ByteOrder) {
        if (order == ByteOrder.BIG_ENDIAN) {
            stream.writeBE64(value)
        } else {
            stream.writeLE64(value)
        }
    }

    protected fun writeI32(stream: OutputStream, value: Int, order: ByteOrder) {
        if (order == ByteOrder.BIG_ENDIAN) {
            stream.writeBE32(value)
        } else {
            stream.writeLE32(value)
        }
    }

    protected fun write(stream: OutputStream, bool: Boolean) {
        stream.write(bool.toInt())
    }
}