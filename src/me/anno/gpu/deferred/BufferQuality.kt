package me.anno.gpu.deferred

enum class BufferQuality: Comparable<BufferQuality> {
    LOW_8,
    MEDIUM_12,
    HIGH_16,
    HIGH_32;
}