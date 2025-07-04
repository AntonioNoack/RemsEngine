package me.anno.mesh.assimp

import me.anno.ecs.components.anim.Bone
import me.anno.mesh.assimp.StaticMeshesLoader.assimpToJoml4x3f
import org.joml.Matrix4x3f
import org.lwjgl.assimp.AIAnimation
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AINodeAnim
import org.lwjgl.assimp.AIScene

fun findAllBones(
    aiScene: AIScene,
    rootNode: AINode,
    boneList: ArrayList<Bone>,
    boneMap: HashMap<String, Bone>
) {
    val boneNames = HashSet<String>()
    // load all bone names from the meshes
    val numMeshes = aiScene.mNumMeshes()
    if (numMeshes > 0) {
        val meshes = aiScene.mMeshes()!!
        for (i in 0 until numMeshes) {
            val mesh = AIMesh.createSafe(meshes[i]) ?: continue
            val numBones = mesh.mNumBones()
            if (numBones > 0) {
                val bones = mesh.mBones()!!
                for (j in 0 until numBones) {
                    val bone = AIBone.createSafe(bones[j]) ?: continue
                    boneNames.add(bone.mName().dataString())
                }
            }
        }
    }
    // get all names of the animated nodes
    val numAnimations = aiScene.mNumAnimations()
    if (numAnimations > 0) {
        val animations = aiScene.mAnimations()!!
        for (i in 0 until numAnimations) {// collect all animated bones
            val aiAnimation = AIAnimation.createSafe(animations[i]) ?: continue
            val channelCount = aiAnimation.mNumChannels()
            if (channelCount > 0) {
                val channels = aiAnimation.mChannels()!!
                for (j in 0 until channelCount) {
                    val aiNodeAnim = AINodeAnim.createSafe(channels[j])
                    if (aiNodeAnim != null) {
                        val name = aiNodeAnim.mNodeName().dataString()
                        boneNames.add(name)
                    }
                }
            }
        }
    }
    // works for adjusting the skeleton to the mesh, but does not work yet for the animation correction
    // todo apply it when loading animations
    val nodeTransformParent: Matrix4x3f? = null // AssimpTree.convert(rootNode.mTransformation()).invert()
    findAllBones1(rootNode, boneNames, boneList, boneMap, -1, nodeTransformParent)
}

/**
 * loads the bones, even if not present in anim nodes or meshes
 * base for animation
 * */
private fun findAllBones1(

    aiNode: AINode,
    allowedNodes: Set<String>,
    boneList: MutableList<Bone>,
    boneMap: MutableMap<String, Bone>,
    lastBoneIndex: Int,

    nodeTransformParent: Matrix4x3f?

) {

    val name = aiNode.mName().dataString()
    val localTransform = assimpToJoml4x3f(aiNode.mTransformation())

    // multiply with the parent transform, if it exists
    nodeTransformParent?.mul(localTransform, localTransform)

    val boneId = if (name in allowedNodes) {
        val bone = boneMap.getOrPut(name) {
            val bone = Bone(boneList.size, lastBoneIndex, name)
            boneList.add(bone)
            bone
        }
        bone.setBindPose(localTransform)
        assimpToJoml4x3f(aiNode.mTransformation(), bone.relativeTransform)
        bone.parentIndex = lastBoneIndex
        bone.index
    } else {
        // loading all bones is no longer showing the skeleton. why? is there alien antenna bones?
        // the root transforms may scale the whole thing, and can have a large effect -> large/huge bones
        // LOGGER.info("missing bone: $name,\n${localTransform.print()}")
        lastBoneIndex
    }

    if (aiNode.mNumChildren() > 0) {
        val children = aiNode.mChildren()!!
        for (i in 0 until aiNode.mNumChildren()) {
            findAllBones1(
                AINode.create(children[i]),
                allowedNodes,
                boneList, boneMap, boneId,
                localTransform
            )
        }
    }
}
