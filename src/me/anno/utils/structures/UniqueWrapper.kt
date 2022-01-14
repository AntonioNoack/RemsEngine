package me.anno.utils.structures

class UniqueWrapper(val v: Any) {
    override fun hashCode(): Int = v.hashCode()
    override fun equals(other: Any?) = other is UniqueWrapper && v === other.v
    override fun toString(): String = v.toString()
}