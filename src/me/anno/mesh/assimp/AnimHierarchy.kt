package me.anno.mesh.assimp

import org.joml.Matrix4x3f
import org.lwjgl.assimp.AIAnimation
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AIScene

object AnimHierarchy {

    fun readBoneAnimHierarchy(

        aiNode: AINode,
        boneList: MutableList<Bone>,
        boneMap: MutableMap<String, Bone>,
        lastBoneId: Int,
        animNodeCache: Set<String>,

        nodeTransformParent: Matrix4x3f?

    ) {

        val name = aiNode.mName().dataString()
        val localTransform = AssimpTree.convert(aiNode.mTransformation())
        val animNodeExists = name in animNodeCache

        val bone = if (animNodeExists) {
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
        nodeCache: Map<String, AINode>,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>
    ) {
        val animNodeCache = HashSet<String>()
        val animations = aiScene.mAnimations()!!
        for (i in 0 until aiScene.mNumAnimations()) {// collect all animated bones
            val aiAnimation = AIAnimation.create(animations[i])
            animNodeCache.addAll(AnimatedMeshesLoader.createAnimationCache(aiAnimation, nodeCache).keys)
        }
        readBoneAnimHierarchy(
            rootNode, boneList, boneMap, -1, animNodeCache,
            null
        )
    }

}