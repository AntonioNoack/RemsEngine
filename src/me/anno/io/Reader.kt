package me.anno.io

import me.anno.maths.Packing.pack64
import me.anno.utils.InternalAPI
import java.io.InputStream
import java.nio.ByteBuffer

// Idea for getting rid of java.io.InputStream and java.nio.ByteBuffer in one go
// todo platform-independent reader:
//  - read in chunks
//  - reset???
//  - given
@InternalAPI
interface Reader {

    var position: Int

    fun readLE8(): Int
    fun readLE16(): Int = readLE8() or readLE8().shl(8)
    fun readLE32(): Int = readLE16() or readLE16().shl(16)
    fun readLE64(): Long {
        val low = readLE32()
        val high = readLE32()
        return pack64(high, low)
    }

    fun readBE8(): Int = readLE8()
    fun readBE16(): Int = readBE8().shl(8) or readBE8()
    fun readBE32(): Int = readBE16().shl(16) or readBE8()
    fun readBE64(): Long = pack64(readBE32(), readBE32())

    fun readUTFChar(): Int {
        // todo implement this properly
        return readLE8()
    }

    // only supported on some platforms
    fun asByteBuffer(): ByteBuffer

    // only supported on some platforms
    fun asStream(): InputStream
}