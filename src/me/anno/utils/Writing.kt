package me.anno.utils

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException

object Writing {

    fun DataInputStream.readNValues(array: ByteArray){
        var i = 0
        val l = array.size
        while(i < l){
            val dl = read(array, i, l-i)
            if(dl < 0) throw EOFException()
            i += dl
        }
    }

    fun DataInputStream.readNValues(array: FloatArray){
        for(i in array.indices){
            array[i] = readFloat()
        }
    }

    fun DataOutputStream.writeNValues(array: ByteArray){
        write(array)
    }

    fun DataOutputStream.writeNValues(array: FloatArray){
        for(f in array) writeFloat(f)
    }

}