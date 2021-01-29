package me.anno.utils.input

import java.io.InputStream

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
