package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent.Companion.MAX_WEIGHTS
import me.anno.ecs.components.mesh.MorphTarget
import me.anno.ecs.prefab.*
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
import kotlin.math.sqrt

object AnimatedMeshesLoader : StaticMeshesLoader() {

    // todo also load morph targets

    private val LOGGER = LogManager.getLogger(AnimatedMeshesLoader::class)

    var debugTransforms = false

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

    }

    operator fun Vector3f.set(index: Int, value: Float) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
        }
    }

    override fun load(file: FileReference, resources: FileReference, flags: Int): AnimGameItem {
        // todo load lights and cameras for the game engine
        val aiScene = loadFile(file, flags)
        val metadata = loadMetadata(aiScene)
        val matrixFix = matrixFix(metadata)
        val materials = loadMaterials(aiScene, resources)
        val boneList = ArrayList<Bone>()
        val boneMap = HashMap<String, Bone>()
        val meshes = loadMeshes(aiScene, materials, boneList, boneMap)
        val (hierarchy, hierarchyPrefab) = buildScene(aiScene, meshes)
        hierarchyPrefab.createInstance()
        val skeleton = if (boneList.isEmpty()) null else addSkeleton(hierarchy, hierarchyPrefab, boneList)
        val animations = if (boneList.isEmpty()) null else loadAnimations(aiScene, boneList, boneMap)
        skeleton?.animations?.putAll(animations!!)
        // todo save all animations and the skeleton into the hierarchy prefab
        if (debugTransforms) {
            var name = file.name
            if (name == "scene.gltf") name = file.getParent()!!.name
            val txt = "" +
                    "metadata:\n$metadata\n" +
                    "matrix fix:\n$matrixFix\n" +
                    "bones:\n${boneList.map { "${it.name}: ${it.offsetMatrix}" }}\n" +
                    "hierarchy:\n${hierarchy.toStringWithTransforms(0)}"
            val ref = getReference(OS.desktop, "$name-${file.hashCode()}.txt")
            LOGGER.info("$file.metadata: ")
            if (!ref.exists || ref.readText() != txt) ref.writeText(txt)
        }
        // LOGGER.info("Found ${meshes.size} meshes and ${animations.size} animations on ${boneList.size} bones, in $resourcePath")
        // println(animations)
        return AnimGameItem(hierarchy, hierarchyPrefab, meshes.toList(), boneList, animations ?: emptyMap())
    }

    private fun addSkeleton(hierarchy: Entity, hierarchyPrefab: Prefab, boneList: List<Bone>): Skeleton {
        val skeleton = Skeleton()
        skeleton.bones = boneList.toTypedArray()
        val changes = hierarchyPrefab.changes!!
        val skeletonAssignments = changes.mapNotNull { change ->
            if (change is CAdd && change.clazzName == "AnimRenderer") {
                val indexInEntity = changes.filter { it is CAdd && it.path == change.path }
                    .indexOfFirst { it === change }
                CSet(change.path!!.add(indexInEntity, 'c'), "skeleton", skeleton)
            } else null
        }
        changes as MutableList<Change>
        changes.addAll(skeletonAssignments)
        for (renderer in hierarchy.getComponentsInChildren<AnimRenderer>(true)) {
            renderer.skeleton = skeleton
        }
        return skeleton
    }

    private fun loadMorphTargets(aiMesh: AIMesh): List<MorphTarget> {
        // alias vertex animations
        // alias shape keys
        val result = ArrayList<MorphTarget>()
        val num = aiMesh.mNumAnimMeshes()
        if (num > 0) {
            val buffer = aiMesh.mAnimMeshes()!!
            for (i in 0 until num) {
                val animMesh = AIAnimMesh.create(buffer.get())
                val name = animMesh.mName().dataString()
                // there is multiple stuff that can be animated?
                // typically we only need the positions, so that should be fine
                // load the animations, and somehow add them to the MeshComponent
                val positions = FloatArray(3 * animMesh.mNumVertices())
                processPositions(animMesh, positions)
                // (we have a sample in our Sims-like test game)
                // LOGGER.info("Found Vertex Animation '$name'")
                result.add(MorphTarget(name, positions))
            }
        }
        return result
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
        aiScene: AIScene, materials: Array<Material>,
        boneList: ArrayList<Bone>, boneMap: HashMap<String, Bone>
    ): Array<Mesh> {
        val numMeshes = aiScene.mNumMeshes()
        return if (numMeshes > 0) {
            val aiMeshes = aiScene.mMeshes()!!
            Array(numMeshes) {
                val aiMesh = AIMesh.create(aiMeshes[it])
                createMesh(aiMesh, materials, boneList, boneMap)
            }
        } else emptyArray()
    }

    private fun loadAnimations(
        aiScene: AIScene,
        boneList: List<Bone>,
        boneMap: HashMap<String, Bone>
    ): Map<String, me.anno.ecs.components.anim.Animation> {
        val root = aiScene.mRootNode()!!
        val globalInverseTransformation = convert(root.mTransformation()).invert()
        // LOGGER.info("global inverse transform: ${convert(root.mTransformation()).invert()}")
        return processAnimations2(
            aiScene, boneList, boneMap, root,
            globalInverseTransformation
        )
    }

    private fun processAnimations2(
        aiScene: AIScene, boneList: List<Bone>, boneMap: HashMap<String, Bone>,
        rootNode: AINode, globalInverseTransformation: Matrix4f
    ): Map<String, me.anno.ecs.components.anim.Animation> {
        // Process all animations
        val numAnimations = aiScene.mNumAnimations()
        val animations = HashMap<String, me.anno.ecs.components.anim.Animation>(numAnimations)
        if (numAnimations > 0) {
            compareBonesAndAnimations(aiScene.mRootNode()!!, boneMap)
            val aiAnimations = aiScene.mAnimations()!!
            // LOGGER.info("Loading animations: $numAnimations")
            for (i in 0 until numAnimations) {
                val aiAnimation = AIAnimation.create(aiAnimations[i])
                val animNodeCache = createAnimationCache(aiAnimation)
                val maxFrames = calcAnimationMaxFrames(aiAnimation)
                val interpolation = if (maxFrames == 1) 1 else max(1, 30 / maxFrames)
                val maxFramesV2 = maxFrames * interpolation
                val duration0 = getDuration(animNodeCache)
                val timeScale = duration0 / (maxFramesV2 - 1.0)
                val frames = Array(maxFramesV2) { frameIndex ->
                    val animatedFrame = AnimationFrame(boneList.size)
                    boneTransform2(
                        aiScene, rootNode, frameIndex * timeScale,
                        animatedFrame.matrices, globalInverseTransformation, boneList, boneMap, animNodeCache
                    )
                    animatedFrame
                }
                var tps = aiAnimation.mTicksPerSecond()
                if (tps < 1e-16) tps = 1000.0
                val duration = aiAnimation.mDuration() / tps
                val animation =
                    ImportedAnimation(aiAnimation.mName().dataString().ifBlank { "Anim[$i]" }, frames, duration)
                animations[animation.name] = animation
            }
        }
        return animations
    }

    fun compareBonesAndAnimations(aiRoot: AINode, bones: HashMap<String, Bone>) {

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
            bones.entries.forEach { (key, value) ->
                LOGGER.warn("  $key: ${value.offsetVector}")
            }
            LOGGER.warn("Nodes[${sceneNodeList.size}]:")
            sceneNodeList.forEach { (key, value) ->
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
                    convert(value.mTransformation())
                }
                bonesWithIssue.forEach { boneNameWithIssue ->
                    val bone = bones[boneNameWithIssue]!!
                    val boneMatrix = bone.offsetMatrix
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

    fun Matrix4x3f.sampleDistanceSquared(other: Matrix4f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

    fun Matrix4f.sampleDistanceSquared(other: Matrix4f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

    /*private fun processAnimations(
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
    }*/

    /*fun buildFrameMatrices(
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
    }*/

    /*fun buildNodeTransformationMatrix(aiNodeAnim: AINodeAnim, frame: Int): Matrix4f {
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
    }*/

    fun createAnimationCache(aiAnimation: AIAnimation): HashMap<String, AINodeAnim> {
        val channelCount = aiAnimation.mNumChannels()
        val result = HashMap<String, AINodeAnim>(channelCount)
        if (channelCount > 0) {
            val channels = aiAnimation.mChannels()!!
            for (i in 0 until channelCount) {
                val aiNodeAnim = AINodeAnim.create(channels[i])
                val name = aiNodeAnim.mNodeName().dataString()
                result[name] = aiNodeAnim
            }
        }
        return result
    }

    fun calcAnimationMaxFrames(aiAnimation: AIAnimation): Int {
        var maxFrames = 0
        val channelCount = aiAnimation.mNumChannels()
        if (channelCount > 0) {
            val channels = aiAnimation.mChannels()!!
            for (i in 0 until channelCount) {
                val aiNodeAnim = AINodeAnim.create(channels[i])
                val numFrames = max(
                    max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys()),
                    aiNodeAnim.mNumRotationKeys()
                )
                maxFrames = max(maxFrames, numFrames)
            }
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
                    bone = Bone(boneList.size, boneName, Matrix4x3f().set(boneTransform))
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

    private fun createMesh(
        aiMesh: AIMesh,
        materials: Array<Material>,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>
    ): Mesh {

        val vertexCount = aiMesh.mNumVertices()
        val vertices = FloatArray(vertexCount * 3)
        val uvs = FloatArray(vertexCount * 2)
        val normals = FloatArray(vertexCount * 3)
        val boneIds = ByteArray(vertexCount * MAX_WEIGHTS)
        val boneWeights = FloatArray(vertexCount * MAX_WEIGHTS)
        val color0 = IntArray(vertexCount)
        val indices = IntArray(aiMesh.mNumFaces() * 3)

        processPositions(aiMesh, vertices)
        processNormals(aiMesh, normals)
        processUVs(aiMesh, uvs)
        processIndices(aiMesh, indices)
        processVertexColors(aiMesh, color0)
        processBones(aiMesh, boneList, boneMap, boneIds, boneWeights)

        val mesh = Mesh()
        mesh.positions = vertices
        mesh.normals = normals
        mesh.uvs = uvs
        mesh.color0 = color0
        mesh.indices = indices
        mesh.boneIndices = boneIds
        mesh.boneWeights = boneWeights

        val morphTargets = loadMorphTargets(aiMesh)
        mesh.morphTargets.addAll(morphTargets)

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx in materials.indices) {
            mesh.material = materials[materialIdx]
        }

        return mesh

    }


}