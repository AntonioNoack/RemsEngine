package me.anno.mesh.assimp

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import java.util.stream.Collectors
import kotlin.math.max
import kotlin.math.min


class AnimatedMeshesLoader : StaticMeshesLoader() {

    class AnimGameItem(val meshes: Array<AssimpMesh>, val animations: Map<String, Animation>)

    fun loadAnimGameItem(resourcePath: String, texturesDir: String): AnimGameItem {
        return loadAnimGameItem(
            resourcePath, texturesDir,
            aiProcess_GenSmoothNormals or aiProcess_JoinIdenticalVertices or aiProcess_Triangulate
                    or aiProcess_FixInfacingNormals or aiProcess_LimitBoneWeights
        )
    }

    fun loadAnimGameItem(resourcePath: String, texturesDir: String, flags: Int): AnimGameItem {
        val aiScene: AIScene = aiImportFile(resourcePath, flags) ?: throw Exception("Error loading model")
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        val materials = ArrayList<Material>()
        for (i in 0 until numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![i])
            processMaterial(aiMaterial, materials, texturesDir)
        }
        val boneList = ArrayList<Bone>()
        val numMeshes = aiScene.mNumMeshes()
        val aiMeshes = aiScene.mMeshes()!!
        val meshes = Array(numMeshes) { i ->
            val aiMesh = AIMesh.create(aiMeshes[i])
            processMesh(aiMesh, materials, boneList)
        }
        val rootNode = buildNodesTree(aiScene.mRootNode()!!, null)
        val globalInverseTransformation: Matrix4f = toMatrix(aiScene.mRootNode()!!.mTransformation()).invert()
        val animations = processAnimations(
            aiScene, boneList, rootNode,
            globalInverseTransformation
        )
        return AnimGameItem(meshes, animations)
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
        for (i in 0 until numAnimations) {
            val aiAnimation = AIAnimation.create(aiAnimations!![i])
            val maxFrames = calcAnimationMaxFrames(aiAnimation)
            val frames: MutableList<AnimatedFrame> = ArrayList()
            val animation = Animation(aiAnimation.mName().dataString(), frames, aiAnimation.mDuration())
            animations[animation.name] = animation
            for (j in 0 until maxFrames) {
                val animatedFrame = AnimatedFrame()
                buildFrameMatrices(
                    aiAnimation, boneList, animatedFrame, j, rootNode,
                    rootNode.transformation, globalInverseTransformation
                )
                frames.add(animatedFrame)
            }
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
        val nodeGlobalTransform: Matrix4f = Matrix4f(parentTransformation).mul(nodeTransform)
        val affectedBones = boneList.stream().filter { it.name == nodeName }.collect(Collectors.toList())
        for (bone: Bone in affectedBones) {
            val boneTransform: Matrix4f =
                Matrix4f(globalInverseTransform).mul(nodeGlobalTransform).mul(bone.offsetMatrix)
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
        aiMesh: AIMesh, boneList: MutableList<Bone>, boneIds: MutableList<Int>,
        weights: MutableList<Float>
    ) {
        val weightSet: MutableMap<Int, MutableList<VertexWeight>> = HashMap()
        val numBones = aiMesh.mNumBones()
        val aiBones = aiMesh.mBones()
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
        for (i in 0 until numVertices) {
            val vertexWeightList: List<VertexWeight>? = weightSet[i]
            val size = vertexWeightList?.size ?: 0
            for (j in 0 until AssimpMesh.MAX_WEIGHTS) {
                if (j < size) {
                    val vw = vertexWeightList!![j]
                    weights.add(vw.weight)
                    boneIds.add(vw.boneId)
                } else {
                    weights.add(0.0f)
                    boneIds.add(0)
                }
            }
        }
    }

    fun processMesh(aiMesh: AIMesh, materials: List<Material>, boneList: MutableList<Bone>): AssimpMesh {

        val vertices = ArrayList<Float>()
        val uvs = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val indices = ArrayList<Int>()
        val boneIds = ArrayList<Int>()
        val weights = ArrayList<Float>()

        processVertices(aiMesh, vertices)
        processNormals(aiMesh, normals)
        processUVs(aiMesh, uvs)
        processIndices(aiMesh, indices)
        processBones(aiMesh, boneList, boneIds, weights)

        // Texture coordinates may not have been populated. We need at least the empty slots
        if (uvs.size == 0) {
            val numElements = (vertices.size / 3) * 2
            for (i in 0 until numElements) {
                uvs.add(0f)
            }
        }

        val mesh = AssimpMesh(
            vertices.toFloatArray(), uvs.toFloatArray(),
            normals.toFloatArray(), indices.toIntArray(),
            boneIds.toIntArray(), weights.toFloatArray()
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