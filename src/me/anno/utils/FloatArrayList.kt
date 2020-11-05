package me.anno.utils

class FloatArrayList(val capacity: Int){
    private val buffers = ArrayList<FloatArray>()
    var size = 0
    operator fun get(index: Int) = buffers[index / capacity][index % capacity]
    operator fun plusAssign(value: Int) = plusAssign(value.toFloat())
    operator fun plusAssign(value: Float){
        val index = size % capacity
        if(index == 0) buffers.add(FloatArray(capacity))
        buffers.last()[index] = value
        size++
    }
}