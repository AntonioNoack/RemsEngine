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

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

object IOUtils {
    @Throws(IOException::class)
    fun toByteBuffer(inputStream: InputStream): ByteBuffer {
        return ByteBuffer.wrap(inputStream.readBytes())
    }

    fun swapEndianness(i: Int): Int {
        return i ushr 24 and 0xFF or
                (i shr 8 and 0xFF00) or
                (i shl 8 and 0xFF0000) or
                (i shl 24 and -0x1000000)
    }
}