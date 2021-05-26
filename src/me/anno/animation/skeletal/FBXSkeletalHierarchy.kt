package me.anno.animation.skeletal

import me.anno.mesh.fbx.model.FBXDeformer
import org.joml.Matrix4f
import java.util.*

class FBXSkeletalHierarchy(
    names: Array<String>,
    parentIndices: IntArray,
    val bones: List<FBXDeformer>
) : SkeletalHierarchy(names, parentIndices) {

    override fun removeBones(kept: SortedSet<Int>): SkeletalHierarchy {
        throw NotImplementedError()
    }

    override fun updateLocalBindPoses(animation: SkeletalAnimation) {

        // write all matrices / bone positions by bone.transformLink / bone.transform
        for (i in bones.indices) {
            val fbxDeformer = bones[i]
            val animBone = animation.bones[i]
            animBone.localBindPose.set(fbxDeformer.transformLink)
        }

        // todo maybe this should be done once only? idk...

    }

}