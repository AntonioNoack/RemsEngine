package org.recast4j

class FloatSubArray(val data: FloatArray, var size: Int) {
    constructor(size: Int) : this(FloatArray(size), size)
}