package org.recast4j.recast

data class HeightRange(var minY: Float, var maxY: Float) {
    fun set(min: Float, max: Float): Boolean {
        this.minY = min
        this.maxY = max
        return true
    }

    fun set(src: HeightRange?): Boolean {
        if (src == null) return false
        return set(src.minY, src.maxY)
    }
}