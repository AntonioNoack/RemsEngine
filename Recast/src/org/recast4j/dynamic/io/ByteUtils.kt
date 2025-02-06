/*
recast4j copyright (c) 2021 Piotr Piastucki piotr@jtilia.org

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
package org.recast4j.dynamic.io

import java.nio.ByteOrder

internal object ByteUtils {
    fun getIntBE(data: ByteArray, position: Int): Int {
        return (data[position].toInt() and 0xff shl 24 or (data[position + 1].toInt() and 0xff shl 16) or (data[position + 2].toInt() and 0xff shl 8)
                or (data[position + 3].toInt() and 0xff))
    }

    fun getIntLE(data: ByteArray, position: Int): Int {
        return (data[position + 3].toInt() and 0xff shl 24 or (data[position + 2].toInt() and 0xff shl 16) or (data[position + 1].toInt() and 0xff shl 8)
                or (data[position].toInt() and 0xff))
    }

    fun getShortBE(data: ByteArray, position: Int): Int {
        return data[position].toInt() and 0xff shl 8 or (data[position + 1].toInt() and 0xff)
    }

    fun getShortLE(data: ByteArray, position: Int): Int {
        return data[position + 1].toInt() and 0xff shl 8 or (data[position].toInt() and 0xff)
    }

    fun putInt(value: Int, data: ByteArray, position: Int, order: ByteOrder): Int {
        if (order == ByteOrder.BIG_ENDIAN) {
            data[position] = (value ushr 24).toByte()
            data[position + 1] = (value ushr 16).toByte()
            data[position + 2] = (value ushr 8).toByte()
            data[position + 3] = (value and 0xFF).toByte()
        } else {
            data[position] = (value and 0xFF).toByte()
            data[position + 1] = (value ushr 8).toByte()
            data[position + 2] = (value ushr 16).toByte()
            data[position + 3] = (value ushr 24).toByte()
        }
        return position + 4
    }

    fun putShort(value: Int, data: ByteArray, position: Int, order: ByteOrder): Int {
        if (order == ByteOrder.BIG_ENDIAN) {
            data[position] = (value ushr 8).toByte()
            data[position + 1] = (value and 0xFF).toByte()
        } else {
            data[position] = (value and 0xFF).toByte()
            data[position + 1] = (value ushr 8).toByte()
        }
        return position + 2
    }
}