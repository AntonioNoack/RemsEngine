package me.anno.mesh.assimp

import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import org.lwjgl.assimp.*

object SkeletonAnimAndBones {

    private val LOGGER = LogManager.getLogger(SkeletonAnimAndBones::class)

    fun loadSkeletonFromAnimationsAndBones(
        aiScene: AIScene,
        rootNode: AINode,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>
    ) {
        val allowedNames = HashSet<String>()
        // load all bone names from the meshes
        val numMeshes = aiScene.mNumMeshes()
        if (numMeshes > 0) {
            val meshes = aiScene.mMeshes()!!
            for (i in 0 until numMeshes) {
                val mesh = AIMesh.create(meshes[i])
                val numBones = mesh.mNumBones()
                if (numBones > 0) {
                    val bones = mesh.mBones()!!
                    for (j in 0 until numBones) {
                        val bone = AIBone.create(bones[j])
                        val boneName = bone.mName().dataString()
                        allowedNames.add(boneName)
                    }
                }
            }
        }
        // get all names of the animated nodes
        val numAnimations = aiScene.mNumAnimations()
        if (numAnimations > 0) {
            val animations = aiScene.mAnimations()!!
            for (i in 0 until numAnimations) {// collect all animated bones
                val aiAnimation = AIAnimation.create(animations[i])
                AnimatedMeshesLoader.createAnimationCache2(aiAnimation, allowedNames)
            }
        }
        // works for adjusting the skeleton to the mesh, but does not work yet for the animation correction
        // todo apply it when loading animations
        val nodeTransformParent: Matrix4x3f? = null//AssimpTree.convert(rootNode.mTransformation()).invert()
        processTree(rootNode, allowedNames, boneList, boneMap, -1, nodeTransformParent)
    }

    /**
     * loads the bones, even if not present in anim nodes or meshes
     * base for animation
     * */
    private fun processTree(

        aiNode: AINode,
        allowedNodes: Set<String>,
        boneList: MutableList<Bone>,
        boneMap: MutableMap<String, Bone>,
        lastBoneId: Int,

        nodeTransformParent: Matrix4x3f?

    ) {

        val name = aiNode.mName().dataString()
        val localTransform = AssimpTree.convert(aiNode.mTransformation())

        // multiply with the parent transform, if it exists
        nodeTransformParent?.mul(localTransform, localTransform)

        val boneId = if (name in allowedNodes) {
            val bone = boneMap.getOrPut(name) {
                val bone = Bone(boneList.size, lastBoneId, name)
                boneList.add(bone)
                bone
            }
            bone.setBindPose(localTransform)
            AssimpTree.convert(aiNode.mTransformation(), bone.relativeTransform)
            bone.parentId = lastBoneId
            bone.id
        } else {
            // loading all bones is no longer showing the skeleton. why? is there alien antenna bones?
            // the root transforms may scale the whole thing, and can have a large effect -> large/huge bones
            // LOGGER.info("missing bone: $name,\n${localTransform.print()}")
            lastBoneId
        }

        val children = aiNode.mChildren()
        if (children != null) {
            for (i in 0 until aiNode.mNumChildren()) {
                processTree(
                    AINode.create(children[i]),
                    allowedNodes,
                    boneList,
                    boneMap, boneId,
                    localTransform
                )
            }
        }

    }

}