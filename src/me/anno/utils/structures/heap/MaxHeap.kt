package me.anno.utils.structures.heap

/**
 * fast data structure for inserting elements, and extracting the maximum element
 * */
@Suppress("unused")
class MaxHeap<Value>(initialSize: Int, comparator: Comparator<Value>) :
    MinHeap<Value>(initialSize, comparator.reversed())