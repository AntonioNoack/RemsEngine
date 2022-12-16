package me.anno.mesh.assimp

import me.anno.utils.types.Matrices.sampleDistanceSquared
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.lwjgl.assimp.AINode
import kotlin.math.sqrt

object MissingBones {

    private val LOGGER = LogManager.getLogger(MissingBones::class)

    fun compareBoneWithNodeNames(aiRoot: AINode, bones: HashMap<String, Bone>) {

        if (bones.isEmpty()) return

        // collect names of all nodes
        val sceneNodes = HashSet<String>()
        val sceneNodeList = ArrayList<Pair<String, AINode>>()
        val todoStack = ArrayList<AINode>()
        todoStack.add(aiRoot)
        while (todoStack.isNotEmpty()) {
            val node = todoStack.removeAt(todoStack.lastIndex)
            val nodeName = node.mName().dataString()
            sceneNodes.add(nodeName)
            sceneNodeList.add(nodeName to node)
            val childCount = node.mNumChildren()
            if (childCount > 0) {
                val children = node.mChildren()!!
                for (i in 0 until childCount) {
                    todoStack.add(AINode.create(children[i]))
                }
            }
        }

        val bonesWithIssue = ArrayList<String>()
        // may be required, if the bones only have partially matching names
        // also we may want to override all bones then...
        // val availableNodes = HashSet<String>(sceneNodes.keys)
        for (boneName in bones.keys) {
            if (!sceneNodes.contains(boneName)) {
                bonesWithIssue.add(boneName)
            }/* else {
                availableNodes.remove(boneName)
            }*/
        }

        if (bonesWithIssue.isNotEmpty()) {
            LOGGER.warn("Bone-Node-Mapping incomplete! Bones[${bones.size}]:")
            for ((key, value) in bones.entries) {
                LOGGER.warn("  $key: ${value.offsetVector}")
            }
            LOGGER.warn("Nodes[${sceneNodeList.size}]:")
            for ((key, value) in sceneNodeList) {
                val transform = value.mTransformation()
                val position = Vector3f(transform.a4(), transform.b4(), transform.c4())
                LOGGER.warn("  $key: $position")
            }
            // find the ideal mapping of all missing bones
            // for each bone find the closest node
            // at least in my sample it works, and there is a 1:1 mapping
            // also no two bones should have the same location
            // the only issue is two nodes being in the same place... (does happen)
            if (sceneNodeList.isNotEmpty()) {
                LOGGER.warn("Mapping ${bonesWithIssue.size} bones:")
                val nodeMatrices = sceneNodeList.map { (_, value) ->
                    // from parent bone to this bone
                    AssimpTree.convert(value.mTransformation())
                }
                for (boneNameWithIssue in bonesWithIssue) {
                    val bone = bones[boneNameWithIssue]!!
                    val boneMatrix = bone.originalTransform
                    var bestNode = 0
                    var bestDistance = Float.POSITIVE_INFINITY
                    for (i in nodeMatrices.indices) {
                        val distance = boneMatrix.sampleDistanceSquared(nodeMatrices[i])
                        if (distance < bestDistance) {
                            bestNode = i
                            bestDistance = distance
                        }
                    }
                    val nodeName = sceneNodeList[bestNode].first
                    bones[nodeName] = bone
                    LOGGER.warn("  Error ${sqrt(bestDistance)}, ${bone.name} to $nodeName")
                }
            }
        }

    }

}