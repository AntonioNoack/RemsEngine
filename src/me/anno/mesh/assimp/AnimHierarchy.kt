package me.anno.mesh.assimp

import org.joml.Matrix4f
import org.lwjgl.assimp.AIAnimation
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AINodeAnim
import org.lwjgl.assimp.AIScene

object AnimHierarchy {

    fun readBoneAnimHierarchy(

        aiNode: AINode,
        boneList: MutableList<Bone>,
        boneMap: MutableMap<String, Bone>,
        lastBoneId: Int,
        animNodeCache: Map<String, AINodeAnim>,

        nodeTransformParent: Matrix4f?

    ) {

        val name = aiNode.mName().dataString()
        val localTransform = AssimpTree.convert(aiNode.mTransformation())
        val aiNodeAnim = animNodeCache[name]

        val bone = if (aiNodeAnim != null) {
            val bone = Bone(boneList.size, lastBoneId, name)
            boneList.add(bone)
            boneMap[name] = bone
            bone
        } else null

        nodeTransformParent?.mul(localTransform, localTransform)

        val boneId = if (bone != null) {
            bone.setBindPose(localTransform)
            bone.id
        } else lastBoneId

        val children = aiNode.mChildren()
        if (children != null) {
            for (i in 0 until aiNode.mNumChildren()) {
                readBoneAnimHierarchy(
                    AINode.create(children[i]),
                    boneList, boneMap, boneId,
                    animNodeCache,
                    localTransform
                )
            }
        }
    }

    fun loadSkeletonFromAnimations(
        aiScene: AIScene,
        rootNode: AINode,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>
    ) {
        val animNodeCache = HashMap<String, AINodeAnim>()
        val animations = aiScene.mAnimations()!!
        for (i in 0 until aiScene.mNumAnimations()) {// collect all animated bones
            val aiAnimation = AIAnimation.create(animations[i])
            animNodeCache.putAll(AnimatedMeshesLoader.createAnimationCache(aiAnimation))
        }
        readBoneAnimHierarchy(
            rootNode, boneList, boneMap, -1, animNodeCache,
            null
        )
    }

}