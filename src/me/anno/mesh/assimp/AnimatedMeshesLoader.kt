package me.anno.mesh.assimp

import me.anno.mesh.assimp.AnimGameItem.Companion.maxBones
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.aiImportFile
import java.util.stream.Collectors
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList


class AnimatedMeshesLoader : StaticMeshesLoader() {

    companion object {
        private val LOGGER = LogManager.getLogger(StaticMeshesLoader::class)
    }

    override fun load(resourcePath: String, texturesDir: String?, flags: Int): AnimGameItem {
        val aiScene: AIScene = aiImportFile(resourcePath, flags) ?: throw Exception("Error loading model")
        val materials = loadMaterials(aiScene, texturesDir)
        val boneList = ArrayList<Bone>()
        val meshes = loadMeshes(aiScene, materials, boneList)
        val animations = loadAnimations(aiScene, boneList)
        LOGGER.info("Found ${meshes.size} meshes and ${animations.size} animations in $resourcePath")
        return AnimGameItem(meshes, animations)
    }

    fun loadMeshes(aiScene: AIScene, materials: Array<Material>, boneList: ArrayList<Bone>): Array<AssimpMesh> {
        val numMeshes = aiScene.mNumMeshes()
        val aiMeshes = aiScene.mMeshes()!!
        return Array(numMeshes) {
            val aiMesh = AIMesh.create(aiMeshes[it])
            processMesh(aiMesh, materials, boneList)
        }
    }

    fun loadAnimations(aiScene: AIScene, boneList: List<Bone>): Map<String, Animation> {
        val root = aiScene.mRootNode()!!
        val rootNode = buildNodesTree(root, null)
        val globalInverseTransformation = toMatrix(root.mTransformation()).invert()
        return processAnimations(
            aiScene, boneList, rootNode,
            globalInverseTransformation
        )
    }

    fun buildNodesTree(aiNode: AINode, parentNode: Node?): Node {
        val nodeName = aiNode.mName().dataString()
        val node = Node(nodeName, parentNode, toMatrix(aiNode.mTransformation()))
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

    fun processAnimations(
        aiScene: AIScene, boneList: List<Bone>,
        rootNode: Node, globalInverseTransformation: Matrix4f
    ): Map<String, Animation> {
        // Process all animations
        val numAnimations = aiScene.mNumAnimations()
        val animations = HashMap<String, Animation>(numAnimations)
        val aiAnimations = aiScene.mAnimations()
        LOGGER.info("loading animations: $numAnimations")
        for (i in 0 until numAnimations) {
            val aiAnimation = AIAnimation.create(aiAnimations!![i])
            val maxFrames = calcAnimationMaxFrames(aiAnimation)
            val frames: MutableList<AnimatedFrame> = ArrayList()
            for (j in 0 until maxFrames) {
                val animatedFrame = AnimatedFrame()
                buildFrameMatrices(
                    aiAnimation, boneList, animatedFrame, j, rootNode,
                    rootNode.transformation, globalInverseTransformation
                )
                frames.add(animatedFrame)
            }
            var tps = aiAnimation.mTicksPerSecond()
            if (tps < 1e-16) tps = 1000.0
            val duration = aiAnimation.mDuration() / tps
            val animation = Animation(aiAnimation.mName().dataString(), frames, duration)
            animations[animation.name] = animation
        }
        return animations
    }

    fun buildFrameMatrices(
        aiAnimation: AIAnimation, boneList: List<Bone>, animatedFrame: AnimatedFrame, frame: Int,
        node: Node, parentTransformation: Matrix4f, globalInverseTransform: Matrix4f
    ) {
        val nodeName = node.name
        val aiNodeAnim = findAIAnimNode(aiAnimation, nodeName)
        var nodeTransform: Matrix4f = node.transformation
        if (aiNodeAnim != null) {
            nodeTransform = buildNodeTransformationMatrix(aiNodeAnim, frame)
        }
        val nodeGlobalTransform = Matrix4f(parentTransformation).mul(nodeTransform)
        val affectedBones = boneList.filter { it.name == nodeName }
        for (bone: Bone in affectedBones) {
            val boneTransform = Matrix4f(globalInverseTransform)
                .mul(nodeGlobalTransform)
                .mul(bone.offsetMatrix)
            animatedFrame.setMatrix(bone.id, boneTransform)
        }
        for (childNode: Node in node.children) {
            buildFrameMatrices(
                aiAnimation, boneList, animatedFrame, frame, childNode, nodeGlobalTransform,
                globalInverseTransform
            )
        }
    }

    fun buildNodeTransformationMatrix(aiNodeAnim: AINodeAnim, frame: Int): Matrix4f {
        val positionKeys = aiNodeAnim.mPositionKeys()
        val scalingKeys = aiNodeAnim.mScalingKeys()
        val rotationKeys = aiNodeAnim.mRotationKeys()
        var aiVecKey: AIVectorKey
        var vec: AIVector3D
        val nodeTransform = Matrix4f()
        val numPositions = aiNodeAnim.mNumPositionKeys()
        if (numPositions > 0) {
            aiVecKey = positionKeys!![min(numPositions - 1, frame)]
            vec = aiVecKey.mValue()
            nodeTransform.translate(vec.x(), vec.y(), vec.z())
        }
        val numRotations = aiNodeAnim.mNumRotationKeys()
        if (numRotations > 0) {
            val quatKey = rotationKeys!![min(numRotations - 1, frame)]
            val aiQuat = quatKey.mValue()
            val quat = Quaternionf(aiQuat.x(), aiQuat.y(), aiQuat.z(), aiQuat.w())
            nodeTransform.rotate(quat)
        }
        val numScalingKeys = aiNodeAnim.mNumScalingKeys()
        if (numScalingKeys > 0) {
            aiVecKey = scalingKeys!![min(numScalingKeys - 1, frame)]
            vec = aiVecKey.mValue()
            nodeTransform.scale(vec.x(), vec.y(), vec.z())
        }
        return nodeTransform
    }

    fun findAIAnimNode(aiAnimation: AIAnimation, nodeName: String): AINodeAnim? {
        var result: AINodeAnim? = null
        val numAnimNodes = aiAnimation.mNumChannels()
        val aiChannels = aiAnimation.mChannels()
        for (i in 0 until numAnimNodes) {
            val aiNodeAnim = AINodeAnim.create(aiChannels!![i])
            if ((nodeName == aiNodeAnim.mNodeName().dataString())) {
                result = aiNodeAnim
                break
            }
        }
        return result
    }

    fun calcAnimationMaxFrames(aiAnimation: AIAnimation): Int {
        var maxFrames = 0
        val numNodeAnimations = aiAnimation.mNumChannels()
        val aiChannels = aiAnimation.mChannels()
        for (i in 0 until numNodeAnimations) {
            val aiNodeAnim = AINodeAnim.create(aiChannels!![i])
            val numFrames = max(
                max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys()),
                aiNodeAnim.mNumRotationKeys()
            )
            maxFrames = max(maxFrames, numFrames)
        }
        return maxFrames
    }

    fun processBones(
        aiMesh: AIMesh, boneList: ArrayList<Bone>,
        boneIds: IntArray, weights: FloatArray
    ) {
        val weightSet: MutableMap<Int, MutableList<VertexWeight>> = HashMap()
        val numBones = aiMesh.mNumBones()
        val aiBones = aiMesh.mBones()
        boneList.ensureCapacity(boneList.size + numBones)
        for (i in 0 until numBones) {
            val aiBone = AIBone.create(aiBones!![i])
            val id = boneList.size
            val bone = Bone(id, aiBone.mName().dataString(), toMatrix(aiBone.mOffsetMatrix()))
            boneList.add(bone)
            val numWeights = aiBone.mNumWeights()
            val aiWeights = aiBone.mWeights()
            for (j in 0 until numWeights) {
                val aiWeight = aiWeights[j]
                val vw = VertexWeight(
                    bone.id, aiWeight.mVertexId(),
                    aiWeight.mWeight()
                )
                var vertexWeightList = weightSet[vw.vertexId]
                if (vertexWeightList == null) {
                    vertexWeightList = ArrayList()
                    weightSet[vw.vertexId] = vertexWeightList
                }
                vertexWeightList.add(vw)
            }
        }
        val numVertices = aiMesh.mNumVertices()
        val maxBoneId = maxBones - 1
        for (i in 0 until numVertices) {
            val vertexWeightList: List<VertexWeight>? = weightSet[i]
            val size = vertexWeightList?.size ?: 0
            val i4 = i * 4
            weights[i4] = 1f
            for (j in 0 until size) {
                val vw = vertexWeightList!![j]
                weights[i4 + j] = vw.weight
                boneIds[i4 + j] = min(vw.boneId, maxBoneId)
            }
        }
    }

    fun processMesh(aiMesh: AIMesh, materials: Array<Material>, boneList: ArrayList<Bone>): AssimpMesh {

        // todo directly use arrays, as they are more efficient
        val vertexCount = aiMesh.mNumVertices()
        val vertices = FloatArray(vertexCount * 3)
        val uvs = FloatArray(vertexCount * 2)
        val normals = FloatArray(vertexCount * 3)
        val indices = ArrayList<Int>()
        val boneIds = IntArray(vertexCount * 4)
        val weights = FloatArray(vertexCount * 4)
        val colors = FloatArray(vertexCount * 4)

        processVertices(aiMesh, vertices)
        processNormals(aiMesh, normals)
        processUVs(aiMesh, uvs)
        processIndices(aiMesh, indices)
        processVertexColors(aiMesh, colors)
        processBones(aiMesh, boneList, boneIds, weights)

        val mesh = AssimpMesh(
            vertices, uvs,
            normals, colors,
            indices.toIntArray(),
            boneIds, weights
        )

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx in materials.indices) {
            mesh.material = materials[materialIdx]
        }

        return mesh

    }

    fun toMatrix(m: AIMatrix4x4): Matrix4f {
        return Matrix4f(
            m.a1(), m.b1(), m.c1(), m.d1(),
            m.a2(), m.b2(), m.c2(), m.d2(),
            m.a3(), m.b3(), m.c3(), m.d3(),
            m.a4(), m.b4(), m.c4(), m.d4()
        )
    }
}