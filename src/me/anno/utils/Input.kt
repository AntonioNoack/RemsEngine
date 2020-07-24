package me.anno.utils

import java.io.InputStream

fun InputStream.readNBytes(n: Int): ByteArray {
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