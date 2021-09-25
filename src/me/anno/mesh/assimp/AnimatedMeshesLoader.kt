package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.mesh.Mesh.Companion.MAX_WEIGHTS
import me.anno.ecs.components.mesh.MorphTarget
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.io.NamedSaveable
import me.anno.io.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.zip.InnerFolder
import me.anno.mesh.assimp.AnimGameItem.Companion.maxBones
import me.anno.mesh.assimp.AnimationLoader.getDuration
import me.anno.mesh.assimp.AnimationLoader.loadAnimationFrame
import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.mesh.assimp.MissingBones.compareBoneWithNodeNames
import me.anno.mesh.assimp.SkeletonAnimAndBones.loadSkeletonFromAnimationsAndBones
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.types.Matrices.isIdentity
import org.apache.logging.log4j.LogManager
import org.joml.*
import org.lwjgl.assimp.*
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

object AnimatedMeshesLoader : StaticMeshesLoader() {

    private val LOGGER = LogManager.getLogger(AnimatedMeshesLoader::class)

    private fun matrixFix(metadata: Map<String, Any>): Matrix3f? {

        var unitScaleFactor = 1f

        val upAxis = (metadata["UpAxis"] as? Int) ?: 1
        val upAxisSign = (metadata["UpAxisSign"] as? Int) ?: 1

        val frontAxis = (metadata["FrontAxis"] as? Int) ?: 2
        val frontAxisSign = (metadata["FrontAxisSign"] as? Int) ?: 1

        val coordAxis = (metadata["CoordAxis"] as? Int) ?: 0
        val coordAxisSign = (metadata["CoordAxisSign"] as? Int) ?: 1

        unitScaleFactor = (metadata["UnitScaleFactor"] as? Double)?.toFloat() ?: unitScaleFactor

        if (unitScaleFactor == 1f && upAxis == 1 && frontAxis == 2) return null

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

    override fun read(file: FileReference, resources: FileReference, flags: Int): AnimGameItem {

        val asFolder = readAsFolder2(file, resources, flags)

        val prefab = asFolder.second
        val instance = prefab.createInstance() as Entity
        val animations = asFolder.first.getChild("animations")
            .listChildren()?.associate {
                val text = it.readText()
                val animation = TextReader.read(text).first() as Animation
                it.nameWithoutExtension to animation
            } ?: emptyMap()
        return AnimGameItem(instance, animations)

        // todo load lights and cameras for the game engine
        // todo save animations, skeleton, textures and hierarchy into files
    }

    fun readAsFolder(
        file: FileReference,
        resources: FileReference = file.getParent() ?: InvalidRef,
        flags: Int = defaultFlags
    ): InnerFolder = readAsFolder2(file, resources, flags).first

    fun readAsFolder2(
        file: FileReference,
        resources: FileReference = file.getParent() ?: InvalidRef,
        flags: Int = defaultFlags
    ): Pair<InnerFolder, Prefab> {

        var name = file.nameWithoutExtension
        if (name.equals("scene", true)) name = file.getParent()!!.name

        // creates prefabs from the whole file content
        // so we can inherit from the materials, meshes, animations, ...
        // all and separately
        val aiScene = loadFile(file, flags)
        val root = InnerFolder(file)
        val rootNode = aiScene.mRootNode()!!
        val loadedTextures = if (aiScene.mNumTextures() > 0) {
            val texFolder = root.createChild("textures", null) as InnerFolder
            loadTextures(aiScene, texFolder)
        } else emptyList()
        val materialList = loadMaterialPrefabs(aiScene, resources, loadedTextures).toList()
        val materials = createReferences(root, "materials", materialList)
        val boneList = ArrayList<Bone>()
        val boneMap = HashMap<String, Bone>()
        val meshList = loadMeshPrefabs(aiScene, materials, boneList, boneMap).toList()
        val meshes = createReferences(root, "meshes", meshList)
        val hasAnimations = aiScene.mNumAnimations() > 0
        val hasSkeleton = boneList.isNotEmpty() || hasAnimations
        val hierarchy = buildScene(aiScene, meshes, hasSkeleton)

        val matrixFix = matrixFix(loadMetadata(aiScene))
        if (matrixFix != null) {
            applyMatrixFix(hierarchy, matrixFix)
        }

        // for (change in hierarchy.changes!!) LOGGER.info(change)

        val animationReferences = if (hasSkeleton) {

            val skeleton = Prefab("Skeleton")
            compareBoneWithNodeNames(rootNode, boneMap)
            loadSkeletonFromAnimationsAndBones(aiScene, rootNode, boneList, boneMap)
            fixBoneOrder(boneList, meshList)

            skeleton.setProperty("bones", boneList.toTypedArray())

            val skeletonPath = root.createPrefabChild("Skeleton.json", skeleton)
            addSkeleton(hierarchy, skeleton, skeletonPath)

            val nodeCache = createNodeCache(rootNode)
            val animMap = loadAnimations(name, aiScene, nodeCache, boneMap)
            for (animation in animMap.values) animation.skeleton = skeletonPath

            // create a animation node to show the first animation
            if (meshes.isEmpty() && animMap.isNotEmpty()) {
                val anim = CAdd(Path.ROOT_PATH, 'c', "AnimRenderer")
                val animPath = anim.getChildPath(0)
                hierarchy.add(anim)
                hierarchy.setUnsafe(animPath, "skeleton", skeletonPath)
                hierarchy.setUnsafe(animPath, "animationWeights", hashMapOf(animMap.values.first() to 1f))
            }

            correctBonePositions(name, rootNode, boneList, boneMap)

            val animList = animMap.values.toList()
            val animRefs = createReferences(root, "animations", animList)
            skeleton.setUnsafe(Path.ROOT_PATH, "animations", animRefs.associateBy { it.nameWithoutExtension })
            animRefs

        } else emptyList()

        // override the empty scene with an animation
        if (meshes.isEmpty() && hasAnimations) {
            val sample = animationReferences.first()
            root.createTextChild("Scene.json", sample.readText())
        } else root.createPrefabChild("Scene.json", hierarchy)

        root.sealPrefabs()

        return root to hierarchy
    }

    private fun applyMatrixFix(prefab: Prefab, matrix: Matrix3f) {

        // apply matrix fix:
        // - get the root transform
        // - apply our transform
        // - then write it back

        val sample = prefab.getSampleInstance() as? Entity ?: return

        val transform0 = sample.transform

        val transform = transform0.localTransform // root, so global = local

        // correct order? at least the rotation is correct
        // correct scale?
        transform.mul(Matrix4x3d(matrix.transpose()))

        prefab.setProperty("position", transform.getTranslation(Vector3d()))
        prefab.setProperty("rotation", transform.getUnnormalizedRotation(Quaterniond()))
        prefab.setProperty("scale", transform.getScale(Vector3d()))

        sample.invalidateChildTransforms()

    }

    private fun fixBoneOrder(boneList: ArrayList<Bone>, meshes: List<Prefab>) {
        // bones must be in order: the parent id must always be smaller than the own id
        var needsFix = false
        val size = boneList.size
        for (i in 0 until size) {
            if (i <= boneList[i].parentId) {
                needsFix = true
                break
            }
        }
        if (!needsFix) return
        // sort the bones based on their parent id
        boneList.sortBy { it.parentId }
        // create the correcting id mapping
        val mapping = IntArray(size)
        for (i in 0 until size) {
            mapping[boneList[i].id] = i
        }
        // apply the change to all bones
        for (index in 0 until size) {
            val bone = boneList[index]
            bone.id = mapping[bone.id]
            bone.parentId = if (bone.parentId < 0) -1 else mapping[bone.parentId]
        }
        // apply the change to all meshes
        for (mesh in meshes) {
            val changeWithIndices =
                mesh.sets[Path.ROOT_PATH, "boneIndices"] // !!.firstOrNull { it.name == "boneIndices" }
            if (changeWithIndices != null) {
                // correct order?
                val values = changeWithIndices as ByteArray
                for (i in values.indices) {
                    values[i] = mapping[values[i].toInt().and(255)].toByte()
                }
            }
        }
    }

    private fun correctBonePositions(
        name: String,
        rootNode: AINode,
        boneList: List<Bone>,
        boneMap: HashMap<String, Bone>
    ) {
        val (gt, git) = findRootTransform(name, rootNode, boneMap)
        if (gt != null && git != null) {
            for (bone in boneList) {
                bone.setBindPose(Matrix4f(git).mul(bone.bindPose))
            }
        }
    }

    private fun <V : Saveable> createReferences(
        root: InnerFolder,
        folderName: String,
        instances: List<V>
    ): List<FileReference> {
        return if (instances.isNotEmpty()) {
            val meshFolder = root.createChild(folderName, null) as InnerFolder
            val references = ArrayList<FileReference>(instances.size)
            for (index in instances.indices) {
                val instance = instances[index]
                references += if (instance is Prefab) {
                    val name = instance.sets[Path.ROOT_PATH, "name"]?.toString() ?: ""
                    // .firstOrNull { it.path.isEmpty() && it.name == "name" }?.value?.toString() ?: ""
                    val nameOrIndex = name.ifEmpty { "$index" }
                    val fileName = findNextFileName(meshFolder, nameOrIndex, "json", 3, '-')
                    meshFolder.createPrefabChild(fileName, instance)
                } else {
                    val name = (instance as? NamedSaveable)?.name ?: ""
                    val nameOrIndex = name.ifEmpty { "$index" }
                    val fileName = findNextFileName(meshFolder, nameOrIndex, "json", 3, '-')
                    meshFolder.createTextChild(fileName, TextWriter.toText(instance))
                }
            }
            references
        } else emptyList()
    }

    private fun addSkeleton(hierarchyPrefab: Prefab, skeleton: Prefab, skeletonPath: FileReference): Prefab {
        val adds = hierarchyPrefab.adds
        for (change in adds) {
            if (change.clazzName == "AnimRenderer") {
                val indexInEntity = adds
                    .filter { it.path == change.path }
                    .indexOfFirst { it === change }
                val name = "skeleton"
                hierarchyPrefab.setUnsafe(change.path.added(name, indexInEntity, 'c'), name, skeletonPath)
            }
        }
        return skeleton
    }

    private fun loadMorphTargets(aiMesh: AIMesh): ArrayList<MorphTarget> {
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
                    4 -> valueRaw.mData(8).double // double
                    5 -> {// string
                        val capacity = 2048
                        val buff = valueRaw.mData(capacity)
                        val length = max(0, min(capacity - 4, buff.int))
                        buff.limit(buff.position() + length)
                        "$length: '${StandardCharsets.UTF_8.decode(buff)}'"
                    }
                    6 -> {
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
            // LOGGER.info(map)
            return map
        } else emptyMap()
    }

    private fun loadMeshPrefabs(
        aiScene: AIScene, materials: List<FileReference>,
        boneList: ArrayList<Bone>, boneMap: HashMap<String, Bone>
    ): Array<Prefab> {
        val numMeshes = aiScene.mNumMeshes()
        return if (numMeshes > 0) {
            val aiMeshes = aiScene.mMeshes()!!
            Array(numMeshes) {
                val aiMesh = AIMesh.create(aiMeshes[it])
                createMeshPrefab(aiMesh, materials, boneList, boneMap)
            }
        } else emptyArray()
    }

    private fun printRoot(aiNode: AINode, boneMap: HashMap<String, Bone>) {
        val name = aiNode.mName().dataString()
        if (name in boneMap) return
        LOGGER.debug("$name: ${convert(aiNode.mTransformation())}")
        if (aiNode.mNumChildren() == 1) {
            printRoot(AINode.create(aiNode.mChildren()!![0]), boneMap)
        }
    }

    // using all root nodes together fixes the fox and the robot <3
    // however, there is still the pilot with the slight scale, and slight rotation...
    private fun complexRootTransform(
        name0: String,
        aiNode: AINode,
        boneMap: HashMap<String, Bone>,
        transform: Matrix4x3f
    ): Matrix4x3f {
        val name = aiNode.mName().dataString()
        if (name in boneMap) return transform
        val localTransform = convert(aiNode.mTransformation())
        // LOGGER.debug("$name0/$name:\n$localTransform")
        transform.mul(localTransform)
        if (aiNode.mNumChildren() == 1) {
            complexRootTransform(name0, AINode.create(aiNode.mChildren()!![0]), boneMap, transform)
        }
        return transform
    }

    private fun findRootTransform(
        name: String,
        rootNode: AINode,
        boneMap: HashMap<String, Bone>
    ): Pair<Matrix4x3f?, Matrix4x3f?> {
        // printRoot(rootNode, boneMap)
        // convert(rootNode.mTransformation()) is incorrect: we need all root nodes to fix the transforms
        var globalTransform: Matrix4x3f? = complexRootTransform(name, rootNode, boneMap, Matrix4x3f())
        if (globalTransform!!.isIdentity()) globalTransform = null
        val globalInverseTransformation = globalTransform?.invert(Matrix4x3f())
        return globalTransform to globalInverseTransformation
    }

    var importedAnimations = true

    private fun loadAnimations(
        name: String,
        aiScene: AIScene,
        nodeCache: Map<String, AINode>,
        boneMap: HashMap<String, Bone>
    ): Map<String, Animation> {

        val rootNode = aiScene.mRootNode()!!

        val (globalTransform, globalInverseTransform) = findRootTransform(name, rootNode, boneMap)
        // if we apply the bone correction first, these must be null/identity
        /*val globalTransform: Matrix4f? = null
        val globalInverseTransform: Matrix4f? = null*/

        // Process all animations
        val numAnimations = aiScene.mNumAnimations()
        val animations = HashMap<String, Animation>(numAnimations)
        if (numAnimations > 0) {
            val aiAnimations = aiScene.mAnimations()!!
            // LOGGER.info("Loading animations: $numAnimations")
            for (i in 0 until numAnimations) {
                val aiAnimation = AIAnimation.create(aiAnimations[i])
                val animNodeCache = createAnimationCache(aiAnimation, nodeCache)
                val maxFrames = calcAnimationMaxFrames(aiAnimation)
                val interpolation = if (maxFrames == 1) 1 else max(1, 30 / maxFrames)
                val maxFramesV2 = maxFrames * interpolation
                val duration0 = getDuration(animNodeCache)
                val timeScale = duration0 / (maxFramesV2 - 1.0)
                var tps = aiAnimation.mTicksPerSecond()
                if (tps < 1e-16) tps = 1000.0
                val duration = aiAnimation.mDuration() / tps
                val animName = aiAnimation.mName().dataString().ifBlank { "Anim[$i]" }
                val animation = if (importedAnimations) {
                    createSkinning(
                        aiScene, rootNode, boneMap, animName, duration, maxFramesV2, timeScale,
                        animNodeCache, globalTransform, globalInverseTransform
                    )
                } else {
                    // todo isn't correct yet...
                    createBoneByBone(
                        aiScene, rootNode, boneMap, animName, duration, maxFramesV2, timeScale,
                        animNodeCache, globalTransform, globalInverseTransform
                    )
                }
                animations[animName] = animation
            }
        }
        return animations
    }

    fun createSkinning(
        aiScene: AIScene, rootNode: AINode,
        boneMap: HashMap<String, Bone>, animName: String, duration: Double, maxFramesV2: Int, timeScale: Double,
        animNodeCache: Map<String, NodeAnim>, globalTransform: Matrix4x3f?, globalInverseTransform: Matrix4x3f?
    ): ImportedAnimation {
        val frames = Array(maxFramesV2) { frameIndex ->
            val animatedFrame = AnimationFrame(boneMap.size)
            loadAnimationFrame(
                aiScene, rootNode, frameIndex * timeScale,
                animatedFrame.skinningMatrices, globalTransform, globalInverseTransform,
                boneMap, animNodeCache
            )
            animatedFrame
        }
        return ImportedAnimation(animName, duration, frames)
    }

    fun createBoneByBone(
        aiScene: AIScene, rootNode: AINode,
        boneMap: HashMap<String, Bone>, animName: String, duration: Double, maxFramesV2: Int, timeScale: Double,
        animNodeCache: Map<String, NodeAnim>, globalTransform: Matrix4x3f?, globalInverseTransform: Matrix4x3f?
    ): BoneByBoneAnimation {
        val rootMotion = FloatArray(maxFramesV2 * 3)
        val rotations = FloatArray(maxFramesV2 * boneMap.size * 4)
        for (frameIndex in 0 until maxFramesV2) {
            loadAnimationFrame(
                aiScene, rootNode, frameIndex * timeScale, frameIndex,
                rootMotion, rotations, boneMap, animNodeCache
            )
        }
        return BoneByBoneAnimation(
            animName, duration, maxFramesV2, boneMap.size, rootMotion, rotations,
            globalTransform, globalInverseTransform
        )
    }

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
        // if(affectedBones.size > 1) LOGGER.info("filtering nodes for name $nodeName, found ${affectedBones.size} bones, bones: ${bones.map { it.name }}")
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

    fun createNodeCache(rootNode: AINode): HashMap<String, AINode> {
        val result = HashMap<String, AINode>()
        createNodeCache(rootNode, result)
        return result
    }

    fun createNodeCache(node: AINode, map: HashMap<String, AINode>) {
        map[node.mName().dataString()] = node
        val numChildren = node.mNumChildren()
        if (numChildren > 0) {
            val children = node.mChildren()!!
            for (index in 0 until numChildren) {
                createNodeCache(AINode.create(children[index]), map)
            }
        }
    }

    fun createAnimationCache(aiAnimation: AIAnimation, nodeCache: Map<String, AINode>): HashMap<String, NodeAnim> {
        val channelCount = aiAnimation.mNumChannels()
        val result = HashMap<String, NodeAnim>(channelCount)
        if (channelCount > 0) {
            val channels = aiAnimation.mChannels()!!
            for (i in 0 until channelCount) {
                val aiNodeAnim = AINodeAnim.create(channels[i])
                val name = aiNodeAnim.mNodeName().dataString()
                val node = nodeCache[name]
                if (node != null) {
                    result[name] = NodeAnim(node, aiNodeAnim)
                } else LOGGER.warn("Missing node '$name'")
            }
        }
        return result
    }

    fun createAnimationCache2(aiAnimation: AIAnimation, names: MutableCollection<String>) {
        val channelCount = aiAnimation.mNumChannels()
        if (channelCount > 0) {
            val channels = aiAnimation.mChannels()!!
            for (i in 0 until channelCount) {
                val aiNodeAnim = AINodeAnim.create(channels[i])
                val name = aiNodeAnim.mNodeName().dataString()
                names.add(name)
            }
        }
    }

    private fun calcAnimationMaxFrames(aiAnimation: AIAnimation): Int {
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

    private fun processBones(
        aiMesh: AIMesh,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>,
        vertexCount: Int
    ): Pair<ByteArray, FloatArray>? {

        val weightSet: MutableMap<Int, MutableList<VertexWeight>> = HashMap()
        val numBones = aiMesh.mNumBones()

        if (numBones > 0) {

            val boneIds = ByteArray(vertexCount * MAX_WEIGHTS)
            val boneWeights = FloatArray(vertexCount * MAX_WEIGHTS)

            val aiBones = aiMesh.mBones()!!
            boneList.ensureCapacity(boneList.size + numBones)

            for (i in 0 until numBones) {

                val aiBone = AIBone.create(aiBones[i])
                val boneName = aiBone.mName().dataString()
                val boneTransform = convert(aiBone.mOffsetMatrix())

                var bone = boneMap[boneName]
                if (bone == null) {
                    bone = Bone(boneList.size, -1, boneName)
                    // for finding the correct nodes
                    bone.originalTransform.set(boneTransform)
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

            val numVertices = aiMesh.mNumVertices()
            val maxBoneId = maxBones - 1
            for (i in 0 until numVertices) {
                val vertexWeightList = weightSet[i]
                if (vertexWeightList != null) {
                    vertexWeightList.sortByDescending { it.weight }
                    val size = min(vertexWeightList.size, MAX_WEIGHTS)
                    val startIndex = i * MAX_WEIGHTS
                    boneWeights[startIndex] = 1f
                    for (j in 0 until size) {
                        val vw = vertexWeightList[j]
                        boneWeights[startIndex + j] = vw.weight
                        boneIds[startIndex + j] = min(vw.boneId, maxBoneId).toByte()
                    }
                } else boneWeights[i * MAX_WEIGHTS] = 1f
            }

            return boneIds to boneWeights

        }

        return null

    }

    private fun createMeshPrefab(
        aiMesh: AIMesh,
        materials: List<FileReference>,
        boneList: ArrayList<Bone>,
        boneMap: HashMap<String, Bone>
    ): Prefab {

        val prefab = createMeshPrefab(aiMesh, materials)

        val vertexCount = aiMesh.mNumVertices()
        val boneData = processBones(aiMesh, boneList, boneMap, vertexCount)
        if (boneData != null) {
            prefab.setProperty("boneIndices", boneData.first)
            prefab.setProperty("boneWeights", boneData.second)
        }

        val morphTargets = loadMorphTargets(aiMesh)
        if (morphTargets.isNotEmpty()) {
            prefab.setProperty("morphTargets", morphTargets)
        }

        return prefab

    }


}