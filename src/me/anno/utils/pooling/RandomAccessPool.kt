package me.anno.utils.pooling

abstract class RandomAccessPool<V>(val capacity: Int) {
    
    abstract fun create(): V
    
    val elements = ArrayList<V>(capacity)
    
    fun ret(v: V) {
        synchronized(elements) {
            if (elements.size < capacity) {
                elements.add(v)
            }
        }
    }

    fun get(): V {
        synchronized(this) {
            if (elements.isNotEmpty()) {
                return elements.removeLast()
            }
        }
        return create()
    }
}