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

import org.joml.Vector3f
import java.io.OutputStream
import java.nio.ByteOrder

abstract class DetourWriter {

    protected fun write(stream: OutputStream, value: Float, order: ByteOrder) {
        write(stream, java.lang.Float.floatToIntBits(value), order)
    }

    protected fun write(stream: OutputStream, value: Vector3f, order: ByteOrder) {
        write(stream, java.lang.Float.floatToIntBits(value.x), order)
        write(stream, java.lang.Float.floatToIntBits(value.y), order)
        write(stream, java.lang.Float.floatToIntBits(value.z), order)
    }

    protected fun write(stream: OutputStream, value: Short, order: ByteOrder) {
        if (order == ByteOrder.BIG_ENDIAN) {
            stream.write(value.toInt() shr 8 and 0xFF)
            stream.write(value.toInt() and 0xFF)
        } else {
            stream.write(value.toInt() and 0xFF)
            stream.write(value.toInt() shr 8 and 0xFF)
        }
    }

    protected fun write(stream: OutputStream, value: Long, order: ByteOrder) {
        if (order == ByteOrder.BIG_ENDIAN) {
            write(stream, (value ushr 32).toInt(), order)
            write(stream, value.toInt(), order)
        } else {
            write(stream, value.toInt(), order)
            write(stream, (value ushr 32).toInt(), order)
        }
    }

    protected fun write(stream: OutputStream, value: Int, order: ByteOrder) {
        if (order == ByteOrder.BIG_ENDIAN) {
            stream.write(value shr 24 and 0xFF)
            stream.write(value shr 16 and 0xFF)
            stream.write(value shr 8 and 0xFF)
            stream.write(value and 0xFF)
        } else {
            stream.write(value and 0xFF)
            stream.write(value shr 8 and 0xFF)
            stream.write(value shr 16 and 0xFF)
            stream.write(value shr 24 and 0xFF)
        }
    }

    protected fun write(stream: OutputStream, bool: Boolean) {
        write(stream, (if (bool) 1 else 0).toByte())
    }

    protected fun write(stream: OutputStream, value: Byte) {
        stream.write(value.toInt())
    }
}