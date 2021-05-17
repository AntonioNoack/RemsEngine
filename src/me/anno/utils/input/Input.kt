package me.anno.utils.input

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer

// defined with a 2, if already present (newer Java versions)
fun InputStream.readNBytes2(n: Int, throwEOF: Boolean): ByteArray {
    return readNBytes2(n, ByteArray(n), throwEOF)
}

@Throws(EOFException::class)
fun InputStream.readNBytes2(n: Int, bytes: ByteArray, throwEOF: Boolean): ByteArray {
    var i = 0
    while(i < n){
        val l = read(bytes, i, n-i)
        if(l < 0){
           if(throwEOF){
               throw EOFException()
           } else {
               // end :/ -> return sub array
               val sub = ByteArray(i)
               // src, dst
               System.arraycopy(bytes, 0, sub, 0, i)
               return sub
           }
        }
        i += l
    }
    return bytes
}

@Throws(EOFException::class)
fun InputStream.readNBytes2(n: Int, bytes: ByteBuffer, throwEOF: Boolean): ByteBuffer {
    val buffered = buffered()
    bytes.position(0)
    for(i in 0 until n){
        val c = buffered.read()
        if(c < 0) {
            if(throwEOF) throw EOFException()
            else throw IllegalArgumentException("throwEOF must be true")
        }
        bytes.put(c.toByte())
    }
    bytes.position(0)
    return bytes
}