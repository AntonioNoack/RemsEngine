package me.anno.gpu.deferred

enum class BufferQuality {
    LOW_8,
    MEDIUM_12,
    HIGH_16,
    HIGH_32;

    fun max(other: BufferQuality): BufferQuality {
        return if(other.ordinal > ordinal) other else this
    }

}