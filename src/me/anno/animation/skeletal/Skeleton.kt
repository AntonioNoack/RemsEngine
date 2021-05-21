package me.anno.animation.skeletal

import java.util.*

class Skeleton(val hierarchy: SkeletalHierarchy, val weights: SkeletalWeights){

    val boneCount = hierarchy.names.size

    fun removeBones(kept: SortedSet<Int>): Skeleton {
        return Skeleton(hierarchy.removeBones(kept), weights.removeBonesCenter(kept, hierarchy))
    }

    /*fun joinBones(joint: Pair<Int, Int>): Skeleton {
        return Skeleton(hierarchy.removeBones(joint), weights.removeBonesCenter(joint, hierarchy))
    }*/

    fun removeBones(kept: Set<String>): Skeleton = removeBones(kept.map { hierarchy.getBone(it) }.toSortedSet())
    //fun joinBones(joint: Pair<String, String>): Skeleton = joinBones()

}