package me.anno.io.numpy

import java.io.Serializable

data class NumPyData(val descriptor: String, val shape: IntArray, val columnMajor: Boolean, val data: Serializable) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NumPyData) return false

        if (descriptor != other.descriptor) return false
        if (!shape.contentEquals(other.shape)) return false
        if (columnMajor != other.columnMajor) return false
        return data == other.data
    }

    override fun hashCode(): Int {
        var result = descriptor.hashCode()
        result = 31 * result + shape.contentHashCode()
        result = 31 * result + columnMajor.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}