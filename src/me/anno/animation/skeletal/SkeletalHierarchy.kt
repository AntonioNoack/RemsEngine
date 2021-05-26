package me.anno.animation.skeletal

import java.util.*

abstract class SkeletalHierarchy(
    val names: Array<String>,
    val parentIndices: IntArray
) {

    abstract fun removeBones(kept: SortedSet<Int>): SkeletalHierarchy

    open fun cutByNames(kept: Set<String>): SkeletalHierarchy = removeBones(kept.map { getBone(it) }.toSortedSet())

    fun getBone(name: String) = names.indexOf(name)
    fun getName(index: Int) = names[index]
    fun getParentName(index: Int) = names[parentIndices[index]]

    abstract fun updateLocalBindPoses(animation: SkeletalAnimation)

}