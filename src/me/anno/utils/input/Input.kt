package me.anno.utils.input

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer

// defined with a 2, if already present (newer Java versions)
fun InputStream.readNBytes2(n: Int): ByteArray {
    val bytes = ByteArray(n)
    var i = 0
    while(i < n){
        val l = read(bytes, i, n-i)
        if(l < 0){
            // end :/ -> return sub array
            val sub = ByteArray(i)
            // src, dst
            System.arraycopy(bytes, 0, sub, 0, i)
            return sub
        }
        i += l
    }
    return bytes
}

fun InputStream.readNBytes2(n: Int, bytes: ByteArray): ByteArray {
    var i = 0
    while(i < n){
        val l = read(bytes, i, n-i)
        if(l < 0){
            // end :/ -> return sub array
            val sub = ByteArray(i)
            // src, dst
            System.arraycopy(bytes, 0, sub, 0, i)
            return sub
        }
        i += l
    }
    return bytes
}

fun InputStream.readNBytes2(n: Int, bytes: ByteBuffer): ByteBuffer {
    val buffered = buffered()
    bytes.position(0)
    for(i in 0 until n){
        val c = buffered.read()
        if(c < 0) throw EOFException()
        bytes.put(c.toByte())
    }
    bytes.position(0)
    return bytes
}