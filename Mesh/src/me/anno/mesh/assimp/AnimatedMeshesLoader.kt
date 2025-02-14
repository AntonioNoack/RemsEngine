package me.anno.mesh.assimp

import me.anno.animation.LoopingState
import me.anno.ecs.Transform
import me.anno.ecs.components.anim.Animation
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.mesh.BoneWeights
import me.anno.ecs.components.mesh.Mesh.Companion.MAX_WEIGHTS
import me.anno.ecs.components.mesh.utils.MorphTarget
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCallback
import me.anno.io.files.inner.InnerPrefabFile
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.NamedSaveable
import me.anno.io.saveable.Saveable
import me.anno.mesh.assimp.AnimationLoader.getDuration
import me.anno.mesh.assimp.AnimationLoader.loadAnimationFrame
import me.anno.mesh.assimp.MissingBones.compareBoneWithNodeNames
import me.anno.mesh.assimp.StaticMeshesLoader.DEFAULT_ASSIMP_FLAGS
import me.anno.mesh.assimp.StaticMeshesLoader.assimpToJoml4x3f
import me.anno.mesh.assimp.StaticMeshesLoader.createScene
import me.anno.mesh.assimp.StaticMeshesLoader.loadFile
import me.anno.mesh.assimp.StaticMeshesLoader.loadMaterialPrefabs
import me.anno.mesh.assimp.StaticMeshesLoader.loadTextures
import me.anno.mesh.assimp.StaticMeshesLoader.processPositions
import me.anno.mesh.fbx.FBX6000
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.all2
import me.anno.utils.structures.lists.Lists.arrayListOfNulls
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.structures.lists.Lists.sortByParent
import org.apache.logging.log4j.LogManager
import org.joml.Matrix3f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.assimp.AIAnimMesh
import org.lwjgl.assimp.AIAnimation
import org.lwjgl.assimp.AIBone
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AINodeAnim
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIVertexWeight
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

object AnimatedMeshesLoader {

    private val LOGGER = LogManager.getLogger(AnimatedMeshesLoader::class)
    private const val MAX_BONE_ID = 255

    private fun matrixFix(metadata: Map<String, Any>, isFBX: Boolean): Matrix3f? {

        var unitScaleFactor = 1f

        val upAxis = (metadata["UpAxis"] as? Int) ?: 1
        val upAxisSign = (metadata["UpAxisSign"] as? Int) ?: 1

        val frontAxis = (metadata["FrontAxis"] as? Int) ?: 2
        val frontAxisSign = (metadata["FrontAxisSign"] as? Int) ?: 1

        val coordAxis = (metadata["CoordAxis"] as? Int) ?: 0
        val coordAxisSign = (metadata["CoordAxisSign"] as? Int) ?: 1

        if (!isFBX) unitScaleFactor = (metadata["UnitScaleFactor"] as? Double)?.toFloat() ?: unitScaleFactor

        // if (signature == "fbx") unitScaleFactor *= 0.01f // a test, works for the ghost...

        if (unitScaleFactor == 1f && upAxis == 1 && frontAxis == 2) return null

        val upVec = JomlPools.vec3f.create()
        val forwardVec = JomlPools.vec3f.create()
        val rightVec = JomlPools.vec3f.create()

        upVec.set(0f)
        forwardVec.set(0f)
        rightVec.set(0f)

        upVec[upAxis] = upAxisSign * unitScaleFactor
        forwardVec[frontAxis] = frontAxisSign * unitScaleFactor
        rightVec[coordAxis] = coordAxisSign * unitScaleFactor

        JomlPools.vec3f.sub(3)

        return Matrix3f(rightVec, upVec, forwardVec)
    }

    fun readAsFolder(file: FileReference, callback: InnerFolderCallback) {

        var name = file.nameWithoutExtension
        if (name.equals("scene", true)) name = file.getParent().name

        loadFile(file, DEFAULT_ASSIMP_FLAGS) { result, e ->
            if (result != null) {
                val (aiScene1, isFBX1) = result
                callback.ok(readAsFolder2(file, aiScene1, isFBX1, name))
            } else if (e?.message?.contains("FBX-DOM unsupported") == true) {
                FBX6000.readBinaryFBX6000AsFolder(file, callback)
            } else callback.err(e)
        }
    }

    private fun readAsFolder2(file: FileReference, aiScene: AIScene, isFBX: Boolean, name: String): InnerFolder {

        val resources = file.getParent()
        val root = InnerFolder(file)
        val rootNode = aiScene.mRootNode()!!
        val loadedTextures = if (aiScene.mNumTextures() > 0) {
            val texFolder = root.createChild("textures", null) as InnerFolder
            loadTextures(aiScene, texFolder)
        } else emptyList()
        val missingFilesLookup: Map<String, FileReference> = when (file) {
            is InnerFolder -> {
                var innerFolder: InnerFolder = file
                while (true) {
                    innerFolder = file.getParent() as? InnerFolder ?: break
                }
                innerFolder.lookup ?: emptyMap() // good?
            }
            else -> {
                // todo fill this by folder, /tex, /textures
                emptyMap()
            }
        }

        val materialList = loadMaterialPrefabs(aiScene, resources, loadedTextures, file, missingFilesLookup).toList()
        val materials = createReferences(root, "materials", materialList)

        val boneList = ArrayList<Bone>()
        val boneMap = HashMap<String, Bone>()

        val meshList = loadMeshPrefabs(aiScene, materials, boneList, boneMap).toList()
        val meshes = createReferences(root, "meshes", meshList)

        val hasAnimations = aiScene.mNumAnimations() > 0
        if (hasAnimations || boneList.isNotEmpty()) {
            compareBoneWithNodeNames(rootNode, boneMap)
            findAllBones(aiScene, rootNode, boneList, boneMap)
            fixBoneOrder(boneList, meshList)
        }

        val hasSkeleton = boneList.isNotEmpty()
        val (deepPrefab, flatPrefab) = createScene(aiScene, meshes, hasSkeleton)

        LOGGER.debug("{}, #anims: {}, #bones: {}", file, aiScene.mNumAnimations(), boneList.size)

        val metadata = loadMetadata(aiScene)
        val matrixFix = matrixFix(metadata, isFBX) // fbx is already 100x
        if (matrixFix != null) {
            applyMatrixFix(deepPrefab, matrixFix)
        }

        LOGGER.debug("[ScaleDebug] {} -> {} -> {}", file, metadata, matrixFix)

        // for (change in hierarchy.changes!!) LOGGER.info(change)

        var animationReferences = emptyList<FileReference>()
        if (hasSkeleton || hasAnimations) {

            val skeleton = Prefab("Skeleton")
            skeleton["bones"] = boneList
            val skeletonsFolder = root.createChild("skeletons", null) as InnerFolder
            val skeletonPath = skeletonsFolder.createPrefabChild("Skeleton.json", skeleton)
            for (mesh in meshList) {
                if (mesh["boneWeights"] is FloatArray) {
                    mesh["skeleton"] = skeletonPath
                }
            }

            val nodeCache = createNodeCache(rootNode)
            val (globalTransform, globalInverseTransform, animMap) =
                loadAnimations(name, aiScene, nodeCache, boneMap, skeletonPath)
            if (animMap.isNotEmpty()) {
                val animations = root.createChild("animations", null) as InnerFolder
                for ((animName, animation) in animMap) {
                    val folder = animations.createChild(animName, null) as InnerFolder
                    folder.createPrefabChild("Imported.json", animation)
                    folder.createLazyPrefabChild("BoneByBone.json", lazy {
                        createBoneByBone(
                            animation.getSampleInstance() as ImportedAnimation,
                            boneList.size, globalTransform, globalInverseTransform,
                        )
                    })
                }
            }

            // must be applied after all animations have been loaded
            correctBonePositions(name, rootNode, boneList, boneMap)
            shortedBoneNames(boneList)

            val sampleAnimations = if (animMap.isNotEmpty()) {
                arrayListOf(
                    AnimationState(
                        animMap.values.first().source, 1f,
                        0f, 1f, LoopingState.PLAY_LOOP
                    )
                )
            } else null // must be ArrayList

            addSkeleton(deepPrefab, skeleton, skeletonPath, sampleAnimations)
            addSkeleton(flatPrefab, skeleton, skeletonPath, sampleAnimations)

            // create an animation node to show the first animation
            if (meshes.isEmpty() && animMap.isNotEmpty()) {
                val deepAnimPath = deepPrefab.add(ROOT_PATH, 'c', "AnimMeshComponent", "AnimMeshComponent")
                val flatAnimPath = flatPrefab.add(ROOT_PATH, 'c', "AnimMeshComponent", "AnimMeshComponent")
                deepPrefab.setUnsafe(deepAnimPath, "skeleton", skeletonPath)
                flatPrefab.setUnsafe(flatAnimPath, "skeleton", skeletonPath)
                if (sampleAnimations != null) {
                    deepPrefab.setUnsafe(deepAnimPath, "animations", sampleAnimations)
                    flatPrefab.setUnsafe(flatAnimPath, "animations", sampleAnimations)
                }
            }

            val animRefs = animMap.values.map { it.source }
            skeleton.setUnsafe(ROOT_PATH, "animations", animRefs.associateBy { it.getParent().name })
            animationReferences = animRefs
        }

        // override the empty scene with an animation
        // LOGGER.debug("Loaded $file, ${meshes.size} meshes + ${animationReferences.size} animations")
        if (meshes.isEmpty() && animationReferences.isNotEmpty()) {
            val sample = animationReferences.maxByOrNull {
                ((it as InnerPrefabFile).prefab.getSampleInstance() as Animation).numFrames
            }!!
            root.createPrefabChild("Scene.json", Prefab("Animation", sample))
        } else {
            // todo flatScene is incorrect for azeria
            root.createPrefabChild("Scene.json", deepPrefab)
            root.createPrefabChild("FlatScene.json", flatPrefab)
        }

        root.sealPrefabs()

        return root
    }

    @Suppress("SpellCheckingInspection")
    private fun shortedBoneNames(bones: List<Bone>) {
        val prefix = "mixamorig:"
        if (bones.all2 { it.name.startsWith(prefix) }) {
            for (bone in bones) bone.name = bone.name.substring(prefix.length)
        }
        for (bone in bones) bone.name = bone.name
            .replace("_\$AssimpFbx\$_Translation", "_T")
            .replace("_\$AssimpFbx\$_Rotation", "_R")
            .replace("_\$AssimpFbx\$_Scaling", "_S")
    }

    private fun applyMatrixFix(prefab: Prefab, matrix: Matrix3f) {

        // apply matrix fix:
        // - get the root transform
        // - apply our transform
        // - then write it back

        val transform0 = Transform()
        val localPosition = prefab.sets[ROOT_PATH, "position"] as? Vector3d
        val localRotation = prefab.sets[ROOT_PATH, "rotation"] as? Quaterniond
        val localScale = prefab.sets[ROOT_PATH, "scale"] as? Vector3d

        if (localPosition != null) transform0.localPosition = localPosition
        if (localRotation != null) transform0.localRotation = localRotation
        if (localScale != null) transform0.localScale = localScale

        val tmp = JomlPools.mat4x3d.create()
        val transform = transform0.getLocalTransform(tmp) // root, so global = local

        // correct order? at least the rotation is correct;
        // correct scale?
        transform.mul(Matrix4x3d(matrix.transpose()))

        prefab["position"] = transform.getTranslation(Vector3d())
        prefab["rotation"] = transform.getUnnormalizedRotation(Quaterniond())
        prefab["scale"] = transform.getScale(Vector3d())
        JomlPools.mat4x3d.sub(1)
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
        if (!needsFix) {
            return
        }
        // sort the bones based on their parent id
        val original = ArrayList(boneList)
        boneList.sortByParent { original.getOrNull(it.parentId) }
        // create the correcting id mapping
        val mapping = IntArray(size)
        for (i in 0 until size) {
            mapping[boneList[i].id] = i
        }
        // apply the change to all bones
        for (index in 0 until size) {
            val bone = boneList[index]
            bone.id = index
            bone.parentId = if (bone.parentId < 0) -1 else mapping[bone.parentId]
        }
        // apply the change to all meshes
        for (mesh in meshes) {
            val changeWithIndices = mesh.sets[ROOT_PATH, "boneIndices"]
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
        name: String, rootNode: AINode,
        boneList: List<Bone>, boneMap: HashMap<String, Bone>
    ) {
        val (_, globalInvTransform) = findRootTransform(name, rootNode, boneMap)
        if (globalInvTransform != null) {
            LOGGER.debug("Applying global transform {} to {}", globalInvTransform, name)
            for (bone in boneList) {
                bone.setBindPose(Matrix4x3f(globalInvTransform).mul(bone.bindPose))
            }
        }
    }

    private fun <V : Saveable> createReferences(
        root: InnerFolder, folderName: String, instances: List<V>
    ): List<FileReference> {
        return if (instances.isNotEmpty()) {
            val meshFolder = root.createChild(folderName, null) as InnerFolder
            val references = ArrayList<FileReference>(instances.size)
            for (index in instances.indices) {
                val instance = instances[index]
                val reference = if (instance is Prefab) {
                    val name = instance.sets[ROOT_PATH, "name"]?.toString() ?: ""
                    // .firstOrNull { it.path.isEmpty() && it.name == "name" }?.value?.toString() ?: ""
                    val nameOrIndex = name.ifEmpty { "$index" }
                    val fileName = findNextFileName(meshFolder, nameOrIndex, "json", 3, '-')
                    val reference = meshFolder.createPrefabChild(fileName, instance)
                    instance.source = reference
                    reference
                } else {
                    val name = (instance as? NamedSaveable)?.name ?: ""
                    val nameOrIndex = name.ifEmpty { "$index" }
                    val fileName = findNextFileName(meshFolder, nameOrIndex, "json", 3, '-')
                    meshFolder.createTextChild(fileName, JsonStringWriter.toText(instance, InvalidRef))
                }
                references += reference
            }
            references
        } else emptyList()
    }

    private fun addSkeleton(
        hierarchyPrefab: Prefab,
        skeleton: Prefab,
        skeletonPath: FileReference,
        sampleAnimations: ArrayList<AnimationState>?
    ): Prefab {
        val adds = hierarchyPrefab.adds
        for ((_, addsI) in adds) {
            for (i in addsI.indices) {
                val add = addsI[i]
                if (add.clazzName == "AnimMeshComponent") {
                    val path = add.path.added(add.nameId, i, 'c')
                    hierarchyPrefab.setUnsafe(path, "skeleton", skeletonPath)
                    if (sampleAnimations != null) hierarchyPrefab.setUnsafe(path, "animations", sampleAnimations)
                }
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
                val value: Any = when (valueRaw.mType()) {
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
                        StandardCharsets.UTF_8.decode(buff)
                    }
                    6 -> {
                        // ai vector3d, floats presumably
                        val buffer = valueRaw.mData(12 * 4).asFloatBuffer()
                        Vector3f(buffer[0], buffer[1], buffer[2])
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
    ): List<Prefab> {
        val numMeshes = aiScene.mNumMeshes()
        return if (numMeshes > 0) {
            val aiMeshes = aiScene.mMeshes()!!
            createList(numMeshes) {
                createMeshPrefab(AIMesh.create(aiMeshes[it]), materials, boneList, boneMap)
            }
        } else emptyList()
    }

    // using all root nodes together fixes the fox and the robot <3
    // however, there is still the pilot with the slight scale, and slight rotation...
    private fun complexRootTransform(
        name0: String, aiNode: AINode, boneMap: HashMap<String, Bone>, transform: Matrix4x3f
    ): Matrix4x3f {
        val name = aiNode.mName().dataString()
        if (name in boneMap) return transform
        val localTransform = assimpToJoml4x3f(aiNode.mTransformation())
        // LOGGER.debug("$name0/$name:\n$localTransform")
        transform.mul(localTransform)
        if (aiNode.mNumChildren() == 1) {
            complexRootTransform(name0, AINode.create(aiNode.mChildren()!![0]), boneMap, transform)
        }
        return transform
    }

    private fun findRootTransform(
        name: String, rootNode: AINode, boneMap: HashMap<String, Bone>
    ): Pair<Matrix4x3f?, Matrix4x3f?> {
        // printRoot(rootNode, boneMap)
        // convert(rootNode.mTransformation()) is incorrect: we need all root nodes to fix the transforms
        var globalTransform: Matrix4x3f? = complexRootTransform(name, rootNode, boneMap, Matrix4x3f())
        if (globalTransform!!.isIdentity()) globalTransform = null
        val globalInverseTransformation = globalTransform?.invert(Matrix4x3f())
        return globalTransform to globalInverseTransformation
    }

    private fun loadAnimations(
        name: String, aiScene: AIScene,
        nodeCache: Map<String, AINode>,
        boneMap: HashMap<String, Bone>,
        skeletonPath: FileReference
    ): Triple<Matrix4x3f?, Matrix4x3f?, Map<String, Prefab>> {

        val rootNode = aiScene.mRootNode()!!

        val (globalTransform, globalInverseTransform) = findRootTransform(name, rootNode, boneMap)
        // if we apply the bone correction first, these must be null/identity
        /*val globalTransform: Matrix4f? = null
        val globalInverseTransform: Matrix4f? = null*/

        // Process all animations
        val numAnimations = aiScene.mNumAnimations()
        val animations = HashMap<String, Prefab>(numAnimations)
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
                animations[animName] = createSkinning(
                    aiScene, rootNode, boneMap, animName, duration,
                    maxFramesV2, timeScale, animNodeCache,
                    globalTransform, globalInverseTransform, skeletonPath
                )
            }
        }
        return Triple(globalTransform, globalInverseTransform, animations)
    }

    fun createSkinning(
        aiScene: AIScene,
        rootNode: AINode,
        boneMap: HashMap<String, Bone>,
        animName: String,
        duration: Double,
        numFrames: Int,
        timeScale: Double,
        animNodeCache: Map<String, NodeAnim>,
        globalTransform: Matrix4x3f?,
        globalInverseTransform: Matrix4x3f?,
        skeletonPath: FileReference
    ): Prefab {
        val prefab = Prefab("ImportedAnimation")
        prefab["name"] = animName
        prefab["skeleton"] = skeletonPath
        prefab["duration"] = duration.toFloat()
        prefab["frames"] = createSkinningFrames(
            aiScene, rootNode, boneMap, numFrames, timeScale,
            animNodeCache, globalTransform, globalInverseTransform
        )
        return prefab
    }

    fun createSkinningFrames(
        aiScene: AIScene,
        rootNode: AINode,
        boneMap: HashMap<String, Bone>,
        numFrames: Int,
        timeScale: Double,
        animNodeCache: Map<String, NodeAnim>,
        globalTransform: Matrix4x3f?,
        globalInverseTransform: Matrix4x3f?
    ): List<List<Matrix4x3f>> {
        return createList(numFrames) { frameIndex ->
            val animatedFrame = createList(boneMap.size) { Matrix4x3f() }
            loadAnimationFrame(
                aiScene, rootNode, frameIndex * timeScale, animatedFrame,
                globalTransform, globalInverseTransform, boneMap, animNodeCache
            )
            animatedFrame
        }
    }

    fun createBoneByBone(
        imported: ImportedAnimation,
        numBones: Int,
        globalTransform: Matrix4x3f?,
        globalInverseTransform: Matrix4x3f?
    ): Prefab {
        val instance = BoneByBoneAnimation(imported)
        val prefab = Prefab("BoneByBoneAnimation")
        prefab._sampleInstance = instance
        prefab["name"] = imported.name
        prefab["duration"] = imported.duration
        prefab["skeleton"] = imported.skeleton
        prefab["frameCount"] = imported.numFrames
        prefab["boneCount"] = numBones
        prefab["translations"] = instance.translations
        prefab["rotations"] = instance.rotations
        prefab["scales"] = instance.scales
        if (globalTransform != null) prefab["globalTransform"] = globalTransform
        if (globalInverseTransform != null) prefab["globalInvTransform"] = globalInverseTransform
        return prefab
    }

    fun createNodeCache(rootNode: AINode): HashMap<String, AINode> {
        val result = HashMap<String, AINode>()
        createNodeCache(rootNode, result)
        return result
    }

    fun createNodeCache(node: AINode, dst: HashMap<String, AINode>) {
        dst[node.mName().dataString()] = node
        val numChildren = node.mNumChildren()
        if (numChildren > 0) {
            val children = node.mChildren()!!
            for (index in 0 until numChildren) {
                createNodeCache(AINode.create(children[index]), dst)
            }
        }
    }

    fun createAnimationCache(aiAnimation: AIAnimation, nodeCache: Map<String, AINode>): Map<String, NodeAnim> {
        val channelCount = aiAnimation.mNumChannels()
        return if (channelCount > 0) {
            val result = HashMap<String, NodeAnim>(channelCount)
            val channels = aiAnimation.mChannels()!!
            for (i in 0 until channelCount) {
                val aiNodeAnim = AINodeAnim.create(channels[i])
                val name = aiNodeAnim.mNodeName().dataString()
                val node = nodeCache[name]
                if (node != null) {
                    result[name] = NodeAnim(node, aiNodeAnim)
                } else LOGGER.warn("Missing node '$name'")
            }
            result
        } else emptyMap()
    }

    private fun calcAnimationMaxFrames(aiAnimation: AIAnimation): Int {
        var maxFrames = 0
        val channelCount = aiAnimation.mNumChannels()
        if (channelCount > 0) {
            val channels = aiAnimation.mChannels()!!
            for (i in 0 until channelCount) {
                val aiNodeAnim = AINodeAnim.create(channels[i])
                val numFrames = max(
                    max(aiNodeAnim.mNumPositionKeys(), aiNodeAnim.mNumScalingKeys()), aiNodeAnim.mNumRotationKeys()
                )
                maxFrames = max(maxFrames, numFrames)
            }
        }
        return maxFrames
    }

    private fun processBones(
        aiMesh: AIMesh, boneList: ArrayList<Bone>, boneMap: HashMap<String, Bone>, vertexCount: Int
    ): Pair<ByteArray, FloatArray>? {

        val numBones = aiMesh.mNumBones()
        if (numBones > 0) {

            val numVertices = aiMesh.mNumVertices()
            val weightSet =
                arrayListOfNulls<MutableList<BoneWeights.VertexWeight>>(numVertices)

            val boneIds = ByteArray(vertexCount * MAX_WEIGHTS)
            val boneWeights = FloatArray(vertexCount * MAX_WEIGHTS)

            val aiBones = aiMesh.mBones()!!
            boneList.ensureCapacity(boneList.size + numBones)

            val aiWeight = AIVertexWeight.calloc()
            for (i in 0 until numBones) {

                val aiBone = AIBone.create(aiBones[i])
                val boneName = aiBone.mName().dataString()
                val boneTransform = assimpToJoml4x3f(aiBone.mOffsetMatrix())

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
                    aiWeights.get(j, aiWeight)
                    val vertexId = aiWeight.mVertexId()
                    val weight = aiWeight.mWeight()
                    if (bone.id < 0 || bone.id > MAX_BONE_ID) continue
                    val vw = BoneWeights.VertexWeight(weight, bone.id.toByte())
                    var vertexWeightList = weightSet[vertexId]
                    if (vertexWeightList == null) {
                        vertexWeightList = ArrayList(MAX_WEIGHTS)
                        weightSet[vertexId] = vertexWeightList
                    }
                    vertexWeightList.add(vw)
                }
            }
            aiWeight.free()

            for (i in 0 until numVertices) {
                val vertexWeightList = weightSet[i]
                BoneWeights.joinBoneWeights(vertexWeightList, boneWeights, boneIds, i)
            }

            return boneIds to boneWeights
        }

        return null
    }

    private fun createMeshPrefab(
        aiMesh: AIMesh, materials: List<FileReference>, boneList: ArrayList<Bone>, boneMap: HashMap<String, Bone>
    ): Prefab {
        val prefab = StaticMeshesLoader.createMeshPrefab(aiMesh, materials)
        val boneData = processBones(aiMesh, boneList, boneMap, aiMesh.mNumVertices())
        if (boneData != null) {
            prefab["boneIndices"] = boneData.first
            prefab["boneWeights"] = boneData.second
        }
        val morphTargets = loadMorphTargets(aiMesh)
        if (morphTargets.isNotEmpty()) {
            prefab["morphTargets"] = morphTargets
        }
        return prefab
    }
}