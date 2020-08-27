package me.anno.parser

import java.lang.RuntimeException

class Vector {

    val data = ArrayList<Any>()
    var isClosed = false

    operator fun get(index: Double): Any? {
        if(data.isEmpty()) return null
        if(index < -1) return null
        val i0 = index.toInt()
        val f = (index-i0)
        val a = this[i0] ?: return null
        val b = this[i0+1] ?: a
        return lerpAny(a, b, f)
    }

    operator fun get(index: Int): Any? {
        return data.getOrNull(index)
    }

    fun map(func: (Any) -> Any): Vector {
        val mapped = Vector()
        mapped.isClosed = isClosed
        mapped.data.addAll(data.map { func(it) })
        return mapped
    }

    fun close(){
        if(isClosed) throw RuntimeException("Vector was already closed")
        isClosed = true
    }

    override fun toString() = data.joinToString()

}