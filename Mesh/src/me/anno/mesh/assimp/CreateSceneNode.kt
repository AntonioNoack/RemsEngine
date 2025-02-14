package me.anno.mesh.assimp

import me.anno.ecs.prefab.change.Path
import me.anno.utils.files.Files.nextName
import me.anno.utils.types.Strings.ifBlank2
import org.joml.Matrix4x3d
import org.lwjgl.assimp.AINode

class CreateSceneNode(
    val parent: CreateSceneNode?,
    val aiNode: AINode,
    val pathName: String,
    val originalName: String
) {

    var path = Path.ROOT_PATH
    val children = ArrayList<CreateSceneNode>(aiNode.mNumChildren())

    lateinit var transform: Matrix4x3d

    // todo for children with duplicate name, we could skip them if they have no meshes
    fun add(aiNode: AINode, usedNames: HashSet<String>): CreateSceneNode {
        val originalName = aiNode.mName().dataString()
        val pathName = nextName(originalName.ifBlank2("Node"), usedNames)
        val child = CreateSceneNode(this, aiNode, pathName, originalName)
        children.add(child)
        return child
    }

    var totalMeshes = 0
    fun countMeshes(): Int {
        var sum = aiNode.mNumMeshes()
        for (i in children.indices) {
            sum += children[i].countMeshes()
        }
        totalMeshes = sum
        return sum
    }
}