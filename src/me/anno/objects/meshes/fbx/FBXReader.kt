package me.anno.objects.meshes.fbx

import me.anno.utils.readNBytes
import java.io.EOFException
import java.io.InputStream
import java.lang.RuntimeException

// todo create a fbx reader to transform this Video Studio into our own game engine? :)
// todo this data is only a decoded representation -> get the data out of it, including animations <3

class FBXReader(input: InputStream): FBXNodeBase() {

    val input = input.buffered()

    init {
        readHeader()

        // the end is the empty node
        try {
            while(true){
                children += FBXNode(this)
            }
        } catch (e: EmptyNodeException){}

        for(node in children){
            println(node)
        }

    }

    var position = 0L

    fun read(): Int {
        position++
        val v = input.read()
        if(v < 0) throw EOFException()
        return v
    }
    fun readInt(): Int {
        val v = input.read() or input.read().shl(8) or
                input.read().shl(16) or read().shl(24)
        position += 3
        return v
    }

    fun readUInt() = readInt().toUInt().toLong()
    fun readLong() = (readInt().toULong() or readInt().toULong().shl(32)).toLong()
    fun readNBytes(n: Int): ByteArray {
        val v = input.readNBytes(n)
        position += n
        return v
    }

    fun readLength8String(): String {
        val length = input.read()
        val bytes = input.readNBytes(length)
        position += length + 1
        return String(bytes)
    }

    fun read0String(): String {
        val buffer = StringBuffer(10)
        while(true){
            val char = input.read()
            position++
            if(char < 1){
                // end reached
                // 0 = end, -1 = eof
                return buffer.toString()
            } else {
                buffer.append(char.toChar())
            }
        }
    }

    fun readHeader(){
        assert(read0String() == "Kaydara FBX Binary  ")
        assert(read() == 0x1a)
        assert(read() == 0x00)
        val version = readInt()
        val major = version / 1000
        val minor = version % 1000
        debug { "Version: $major.$minor" }
    }

    fun debug(msg: () -> String){
        println(msg())
    }

    fun assert(b: Boolean, m: String? = null){
        if(!b) throw RuntimeException(m ?: "")
    }

}