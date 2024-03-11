package me.anno.utils.structures.heap

class MaxHeap<Value>(initialSize: Int, comparator: Comparator<Value>) :
    MinHeap<Value>(initialSize, comparator.reversed())