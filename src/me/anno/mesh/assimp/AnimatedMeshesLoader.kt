package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponent.Companion.MAX_WEIGHTS
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.AnimGameItem.Companion.maxBones
import me.anno.mesh.assimp.AnimatedMeshesLoader2.boneTransform2
import me.anno.mesh.assimp.AnimatedMeshesLoader2.getDuration
import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.assimp.*
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

object AnimatedMeshesLoader : StaticMeshesLoader() {

    // todo also load morph targets

    private val LOGGER = LogManager.getLogger(StaticMeshesLoader::class)

    fun matrixFix(metadata: Map<String, Any>): Matrix3f {

        var unitScaleFactor = 1f

        val upAxis = (metadata["UpAxis"] as? Int) ?: 1
        val upAxisSign = (metadata["UpAxisSign"] as? Int) ?: 1

        val frontAxis = (metadata["FrontAxis"] as? Int) ?: 2
        val frontAxisSign = (metadata["FrontAxisSign"] as? Int) ?: 1

        val coordAxis = (metadata["CoordAxis"] as? Int) ?: 0
        val coordAxisSign = (metadata["CoordAxisSign"] as? Int) ?: 1

        unitScaleFactor = (metadata["UnitScaleFactor"] as? Double)?.toFloat() ?: unitScaleFactor

        val upVec = Vector3f()
        val forwardVec = Vector3f()
        val rightVec = Vector3f()

        upVec[upAxis] = upAxisSign * unitScaleFactor
        forwardVec[frontAxis] = frontAxisSign * unitScaleFactor
        rightVec[coordAxis] = coordAxisSign * unitScaleFactor

        return Matrix3f(rightVec, upVec, forwardVec)

        // Scene->mRootNode->mTransformation = mat;

    }

    operator fun Vector3f.set(index: Int, value: Float) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
        }
    }

    override fun load(file: FileReference, resources: FileReference, flags: Int): AnimGameItem {
        // val store = aiCreatePropertyStore()!!
        // aiSetImportPropertyFloat(store, AI_CONFIG_GLOBAL_SCALE_FACTOR_KEY, 1f)
        // val aiScene: AIScene = aiImportFileExWithProperties(resourcePath, flags, null, store)
        //     ?: throw Exception("Error loading model '$resourcePath'")
        val aiScene = loadFile(file, flags)
        val metadata = loadMetadata(aiScene)
        val matrixFix = matrixFix(metadata)
        val materials = loadMaterials(aiScene, resources)
        val boneList = ArrayList<Bone>()
        val boneMap = HashMap<String, Bone>()
        val meshes = loadMeshes(aiScene, materials, boneList, boneMap)
        val animations = loadAnimations(aiScene, boneList, boneMap)
        var name = file.name
        if (name == "scene.gltf") name = file.getParent()!!.name
        val txt = "" +
                "metadata:\n$metadata\n" +
                "matrix fix:\n$matrixFix\n" +
                "bones:\n${boneList.map { "${it.name}: ${it.offsetMatrix}" }}\n" +
                "hierarchy:\n${meshes.toStringWithTransforms(0)}"
        val ref = getReference(OS.desktop, "$name-${file.hashCode()}.txt")
        LOGGER.info("$file.metadata: ")
        if (!ref.exists || ref.readText() != txt) ref.writeText(txt)
        // LOGGER.info("Found ${meshes.size} meshes and ${animations.size} animations on ${boneList.size} bones, in $resourcePath")
        // println(animations)
        return AnimGameItem(meshes, boneList, animations)
    }

    private fun loadMetadata(aiScene: AIScene): Map<String, Any> {
        val metadata = aiScene.mMetaData()
        return if (metadata != null) {
            val map = HashMap<String, Any>()
            // UnitScaleFactor
            val keys = metadata.mKeys()
            val values = metadata.mValues()
            for (i in 0 until metadata.mNumProperties()) {
                val key = keys[i].dataString()
                val valueRaw = values[i]
                val value = when (valueRaw.mType()) {
                    0 -> valueRaw.mData(1)[0] // bool
                    1 -> valueRaw.mData(4).int // int
                    2 -> valueRaw.mData(8).long // long, unsigned
                    3 -> valueRaw.mData(4).float // float
                    4 -> {// string
                        val capacity = 2048
                        val buff = valueRaw.mData(capacity)
                        val length = max(0, min(capacity - 4, buff.int))
                        buff.limit(buff.position() + length)
                        "$length: '${StandardCharsets.UTF_8.decode(buff)}'"
                    }
                    5 -> {
                        // aivector3d
                        // todo doubles or floats?
                        val buffer = valueRaw.mData(12 * 8).asDoubleBuffer()
                        Vector3d(buffer[0], buffer[1], buffer[2])
                    }
                    else -> continue
                }
                // LOGGER.info("Metadata $key: $valueType, $value")
                map[key] = value
            }
            return map
        } else emptyMap()
    }

    private fun loadMeshes(
        aiScene: AIScene,
        materials: Array<Material>,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>
    ): Entity {

        val numMeshes = aiScene.mNumMeshes()
        val meshes = if (numMeshes > 0) {
            val aiMeshes = aiScene.mMeshes()!!
            Array(numMeshes) {
                val aiMesh = AIMesh.create(aiMeshes[it])
                processMesh(aiMesh, materials, boneList, boneMap)
            }
        } else emptyArray()
        return buildScene(aiScene, meshes)

    }

    private fun loadAnimations(
        aiScene: AIScene,
        boneList: List<Bone>,
        boneMap: HashMap<String, Bone>
    ): Map<String, Animation> {
        val root = aiScene.mRootNode()!!
        val globalInverseTransformation = convert(root.mTransformation()).invert()
        return processAnimations2(
            aiScene, boneList, boneMap, root,
            globalInverseTransformation
        )
    }

    private fun processAnimations2(
        aiScene: AIScene, boneList: List<Bone>, boneMap: HashMap<String, Bone>,
        rootNode: AINode, globalInverseTransformation: Matrix4f
    ): Map<String, Animation> {
        // Process all animations
        val numAnimations = aiScene.mNumAnimations()
        val animations = HashMap<String, Animation>(numAnimations)
        val aiAnimations = aiScene.mAnimations()
        LOGGER.info("Loading animations: $numAnimations")
        for (i in 0 until numAnimations) {
            val aiAnimation = AIAnimation.create(aiAnimations!![i])
            val animNodeCache = createAnimationCache(aiAnimation)
            val maxFrames = calcAnimationMaxFrames(aiAnimation)
            val interpolation = if (maxFrames == 1) 1 else max(1, 30 / maxFrames)
            val maxFramesV2 = maxFrames * interpolation
            val duration0 = getDuration(animNodeCache)
            val timeScale = duration0 / (maxFramesV2 - 1.0)
            val frames = Array(maxFramesV2) { frameIndex ->
                val animatedFrame = AnimatedFrame()
                boneTransform2(
                    aiScene, rootNode, frameIndex * timeScale,
                    animatedFrame.matrices, globalInverseTransformation, boneList, boneMap, animNodeCache
                )
                animatedFrame
            }
            var tps = aiAnimation.mTicksPerSecond()
            if (tps < 1e-16) tps = 1000.0
            val duration = aiAnimation.mDuration() / tps
            val animation = Animation(aiAnimation.mName().dataString(), frames, duration)
            animations[animation.name] = animation
        }
        return animations
    }

    private fun processAnimations(
        aiScene: AIScene, boneList: List<Bone>,
        rootNode: Node, globalInverseTransformation: Matrix4f
    ): Map<String, Animation> {
        // Process all animations
        val numAnimations = aiScene.mNumAnimations()
        val animations = HashMap<String, Animation>(numAnimations)
        val aiAnimations = aiScene.mAnimations()
        LOGGER.info("Loading animations: $numAnimations")
        for (i in 0 until numAnimations) {
            val aiAnimation = AIAnimation.create(aiAnimations!![i])
            val animNodeCache = createAnimationCache(aiAnimation)
            val maxFrames = calcAnimationMaxFrames(aiAnimation)
            val frames = Array(maxFrames) { frameIndex ->
                val animatedFrame = AnimatedFrame()
                buildFrameMatrices(
                    aiAnimation, animNodeCache, boneList, animatedFrame, frameIndex, rootNode,
                    rootNode.transformation, globalInverseTransformation
                )
                animatedFrame
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
        aiAnimation: AIAnimation,
        animNodeCache: Map<String, AINodeAnim>,
        bones: List<Bone>,
        animatedFrame: AnimatedFrame,
        frame: Int,
        node: Node,
        parentTransformation: Matrix4f,
        globalInverseTransform: Matrix4f
    ) {
        val nodeName = node.name
        val aiNodeAnim = animNodeCache[nodeName]
        var nodeTransform = node.transformation
        if (aiNodeAnim != null) {
            nodeTransform = buildNodeTransformationMatrix(aiNodeAnim, frame)
        }
        val nodeGlobalTransform = Matrix4f(parentTransformation).mul(nodeTransform)
        // todo there probably shouldn't be multiple bones... or can they?...
        // todo if not, we can use a hashmap, which is n times as fast
        // todo -> there is, because there is multiple meshes, one for each material...
        val affectedBones = bones.filter { it.name == nodeName }
        // if(affectedBones.size > 1) println("filtering nodes for name $nodeName, found ${affectedBones.size} bones, bones: ${bones.map { it.name }}")
        for (bone in affectedBones) {
            val boneTransform = Matrix4f(globalInverseTransform)
                .mul(nodeGlobalTransform)
                .mul(bone.offsetMatrix)
            animatedFrame.setMatrix(bone.id, boneTransform)
        }
        for (childNode in node.children) {
            buildFrameMatrices(
                aiAnimation, animNodeCache, bones, animatedFrame, frame, childNode, nodeGlobalTransform,
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

    fun createAnimationCache(aiAnimation: AIAnimation): HashMap<String, AINodeAnim> {
        val numAnimNodes = aiAnimation.mNumChannels()
        val aiChannels = aiAnimation.mChannels()
        val map = HashMap<String, AINodeAnim>(numAnimNodes)
        for (i in 0 until numAnimNodes) {
            val aiNodeAnim = AINodeAnim.create(aiChannels!![i])
            val name = aiNodeAnim.mNodeName().dataString()
            map[name] = aiNodeAnim
        }
        return map
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
        aiMesh: AIMesh,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>,
        boneIds: ByteArray, weights: FloatArray
    ) {

        val weightSet: MutableMap<Int, MutableList<VertexWeight>> = HashMap()
        val numBones = aiMesh.mNumBones()

        if (numBones > 0) {

            val aiBones = aiMesh.mBones()!!
            boneList.ensureCapacity(boneList.size + numBones)

            for (i in 0 until numBones) {

                val aiBone = AIBone.create(aiBones[i])
                val boneName = aiBone.mName().dataString()
                val boneTransform = convert(aiBone.mOffsetMatrix())

                var bone = boneMap[boneName]
                if (bone == null) {
                    bone = Bone(boneList.size, boneName, boneTransform)
                    boneList.add(bone)
                    boneMap[boneName] = bone
                }

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
                        vertexWeightList = ArrayList(MAX_WEIGHTS)
                        weightSet[vw.vertexId] = vertexWeightList
                    }
                    vertexWeightList.add(vw)
                }

            }
        }

        val numVertices = aiMesh.mNumVertices()
        val maxBoneId = maxBones - 1
        for (i in 0 until numVertices) {
            val vertexWeightList = weightSet[i]
            if (vertexWeightList != null) {
                vertexWeightList.sortByDescending { it.weight }
                val size = min(vertexWeightList.size, MAX_WEIGHTS)
                val startIndex = i * MAX_WEIGHTS
                weights[startIndex] = 1f
                for (j in 0 until size) {
                    val vw = vertexWeightList[j]
                    weights[startIndex + j] = vw.weight
                    boneIds[startIndex + j] = min(vw.boneId, maxBoneId).toByte()
                }
            } else weights[i * MAX_WEIGHTS] = 1f
        }

    }

    fun processMesh(
        aiMesh: AIMesh,
        materials: Array<Material>,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>
    ): MeshComponent {

        val vertexCount = aiMesh.mNumVertices()
        val vertices = FloatArray(vertexCount * 3)
        val uvs = FloatArray(vertexCount * 2)
        val normals = FloatArray(vertexCount * 3)
        val boneIds = ByteArray(vertexCount * MAX_WEIGHTS)
        val boneWeights = FloatArray(vertexCount * MAX_WEIGHTS)
        val colors = FloatArray(vertexCount * 4)

        // todo directly use an array
        //  - we can force triangles, and know the count that way, or quickly walk through it...
        val indices = ArrayList<Int>()

        processVertices(aiMesh, vertices)
        processNormals(aiMesh, normals)
        processUVs(aiMesh, uvs)
        processIndices(aiMesh, indices)
        processVertexColors(aiMesh, colors)
        processBones(aiMesh, boneList, boneMap, boneIds, boneWeights)

        val mesh = MeshComponent()
        mesh.positions = vertices
        mesh.normals = normals
        mesh.uvs = uvs
        mesh.color0 = colors
        mesh.indices = indices.toIntArray()
        mesh.boneIndices = boneIds
        mesh.boneWeights = boneWeights
        /*
        AssimpMesh( vertices, uvs,
            normals, colors,
            indices.toIntArray(),
            boneIds, weights)
        * */
        // todo calculate the transform and set it
        // todo use it for correct rendering
        // mesh.transform.set(convert())

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx in materials.indices) {
            mesh.material = materials[materialIdx]
        }

        return mesh

    }


}