package me.anno.utils.structures.arrays

class IntArrayList(val capacity: Int){
    private val buffers = ArrayList<IntArray>()
    var size = 0
    operator fun get(index: Int) = buffers[index / capacity][index % capacity]
    operator fun plusAssign(value: Int){
        val index = size % capacity
        if(index == 0) buffers.add(IntArray(capacity))
        buffers.last()[index] = value
        size++
    }
}