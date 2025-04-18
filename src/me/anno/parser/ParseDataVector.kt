package me.anno.parser

class ParseDataVector {

    val data = ArrayList<Any>()
    var isClosed = false

    operator fun plusAssign(value: Any){
        data.add(value)
    }

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

    fun map(func: (Any) -> Any): ParseDataVector {
        val mapped = ParseDataVector()
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