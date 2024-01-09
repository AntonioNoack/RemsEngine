package me.anno.mesh.assimp

import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AINodeAnim

class NodeAnim(aiNode: AINode, val aiNodeAnim: AINodeAnim) {
    val average = AnimationLoader.getTranslation(aiNode)
}