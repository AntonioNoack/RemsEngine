package me.anno.mesh.assimp

import org.joml.Matrix4f
import org.lwjgl.assimp.AIMatrix4x4
import org.lwjgl.assimp.AINode

object AssimpTree {

    fun buildNodesTree(aiNode: AINode, parentNode: Node?): Node {
        val nodeName = aiNode.mName().dataString()
        val nodeTransform = convert(aiNode.mTransformation())
        val node = Node(nodeName, parentNode, nodeTransform)
        val numChildren = aiNode.mNumChildren()
        if (numChildren > 0) {
            val aiChildren = aiNode.mChildren()!!
            for (i in 0 until numChildren) {
                val aiChildNode = AINode.create(aiChildren[i])
                val childNode = buildNodesTree(aiChildNode, node)
                node.addChild(childNode)
            }
        }
        return node
    }

    fun convert(m: AIMatrix4x4): Matrix4f {
        return Matrix4f(
            m.a1(), m.b1(), m.c1(), m.d1(),
            m.a2(), m.b2(), m.c2(), m.d2(),
            m.a3(), m.b3(), m.c3(), m.d3(),
            m.a4(), m.b4(), m.c4(), m.d4()
        )
    }

    /*fun calculateAnimationPose(bones: List<Bone>, animMatrices: List<Matrix4f>, result: List<Matrix4f>) {

        val localTransform = Array(bones.size) { Matrix4f() }
        val modelTransform = Array(bones.size) { Matrix4f() }

        for (i in bones.indices) {
            localTransform[i].set(bones[i].localMatrix).mul(animMatrices[i])
        }

        modelTransform[0].set(localTransform[0])

        for (i in 1 until bones.size) {
            val parent = bones[i].parent.id
            modelTransform[i].set(modelTransform[parent]).mul(localTransform[i])
        }

        for (i in bones.indices) {
            result[i].set(modelTransform[i]).mul(bones[i].invModelMatrix)
        }

    }*/

}