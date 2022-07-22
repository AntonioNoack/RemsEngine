package me.anno.utils.types

object Arrays {

    fun ByteArray?.resize(size: Int) =
        if (this == null || this.size != size) ByteArray(size) else this

    fun IntArray?.resize(size: Int) =
        if (this == null || this.size != size) IntArray(size) else this

    fun FloatArray?.resize(size: Int) =
        if (this == null || this.size != size) FloatArray(size) else this

}