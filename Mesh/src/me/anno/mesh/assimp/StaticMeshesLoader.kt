package me.anno.mesh.assimp

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.GLASS_PASS
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImageFormat
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.files.Reference.sanitizePath
import me.anno.io.files.Signature
import me.anno.io.files.SignatureCache
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.temporary.InnerTmpTextFile
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.io.xml.generic.XMLWriter
import me.anno.maths.EquationSolver.solveQuadratic
import me.anno.utils.Color.rgba
import me.anno.utils.algorithms.ForLoop.forLoop
import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.async.Callback
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.files.Files.nextName
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Strings.distance
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.isNotBlank2
import me.anno.utils.types.Triangles.crossDot
import org.apache.logging.log4j.LogManager
import org.hsluv.HSLuvColorSpace.toSRGB
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.assimp.AIAnimMesh
import org.lwjgl.assimp.AICamera
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIFace
import org.lwjgl.assimp.AILight
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMatrix4x4
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AINode
import org.lwjgl.assimp.AIScene
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.AITexel
import org.lwjgl.assimp.AITexture
import org.lwjgl.assimp.AIVector3D
import org.lwjgl.assimp.Assimp.AI_CONFIG_GLOBAL_SCALE_FACTOR_KEY
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_AMBIENT
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_DIFFUSE
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_EMISSIVE
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_REFLECTIVE
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_SPECULAR
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_TRANSPARENT
import org.lwjgl.assimp.Assimp.AI_MATKEY_GLTF_PBRMETALLICROUGHNESS_METALLICROUGHNESS_TEXTURE
import org.lwjgl.assimp.Assimp.AI_MATKEY_NAME
import org.lwjgl.assimp.Assimp.AI_MATKEY_OPACITY
import org.lwjgl.assimp.Assimp.AI_MATKEY_REFLECTIVITY
import org.lwjgl.assimp.Assimp.AI_MATKEY_REFRACTI
import org.lwjgl.assimp.Assimp.AI_MATKEY_SHININESS
import org.lwjgl.assimp.Assimp.AI_MATKEY_TRANSPARENCYFACTOR
import org.lwjgl.assimp.Assimp.aiCreatePropertyStore
import org.lwjgl.assimp.Assimp.aiGetErrorString
import org.lwjgl.assimp.Assimp.aiGetMaterialColor
import org.lwjgl.assimp.Assimp.aiGetMaterialFloatArray
import org.lwjgl.assimp.Assimp.aiGetMaterialString
import org.lwjgl.assimp.Assimp.aiGetMaterialTexture
import org.lwjgl.assimp.Assimp.aiGetVersionMajor
import org.lwjgl.assimp.Assimp.aiImportFileExWithProperties
import org.lwjgl.assimp.Assimp.aiImportFileFromMemoryWithProperties
import org.lwjgl.assimp.Assimp.aiProcess_GenSmoothNormals
import org.lwjgl.assimp.Assimp.aiProcess_GlobalScale
import org.lwjgl.assimp.Assimp.aiProcess_JoinIdenticalVertices
import org.lwjgl.assimp.Assimp.aiProcess_Triangulate
import org.lwjgl.assimp.Assimp.aiSetImportPropertyFloat
import org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE
import org.lwjgl.assimp.Assimp.aiTextureType_DISPLACEMENT
import org.lwjgl.assimp.Assimp.aiTextureType_EMISSIVE
import org.lwjgl.assimp.Assimp.aiTextureType_LIGHTMAP
import org.lwjgl.assimp.Assimp.aiTextureType_NONE
import org.lwjgl.assimp.Assimp.aiTextureType_NORMALS
import org.lwjgl.system.MemoryUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.tan

// done build a flattened-json -> FlatScene.json
// done discard any nodes without meshes
object StaticMeshesLoader {

    private val LOGGER = LogManager.getLogger(StaticMeshesLoader::class)

    const val DEFAULT_ASSIMP_FLAGS =
        aiProcess_GenSmoothNormals or // if the normals are unavailable, generate smooth ones
                aiProcess_Triangulate or // we don't want to triangulate ourselves
                aiProcess_JoinIdenticalVertices or // is required to load indexed geometry
                // aiProcess_FixInfacingNormals or // is recommended, may be incorrect... is incorrect for the Sponza sample from Intel
                aiProcess_GlobalScale

    val aiPropertyStore = aiCreatePropertyStore()!!

    @JvmStatic
    fun shininessToRoughness(shininessExponent: Float): Float {
        // an approximation, which maps the exponent to roughness;
        // just roughly...
        //   0: 1.00
        // 100: 0.50
        // 200: 0.34
        // 600: 0.14
        // 900: 0.10
        // 1e3: 0.09
        return 1f / (shininessExponent * 0.01f + 1f)
    }

    fun loadFile(file: FileReference, flags: Int, callback: Callback<Pair<AIScene, Boolean>>) {
        SignatureCache[file].waitFor { signature ->
            loadFile(file, flags, signature?.name, callback)
        }
    }

    fun loadFile(file: FileReference, flags: Int, signature: String?, callback: Callback<Pair<AIScene, Boolean>>) {

        // obj files should use our custom importer
        if ((signature == "dae" || signature == "xml") && aiGetVersionMajor() < 5) {
            // Assimp 4.1 is extremely picky when parsing Collada XML for no valid reason
            // Assimp 5.2 fixes that (but also breaks my animation code)
            return file.inputStream { str, err ->
                if (str != null) {
                    val xml = XMLReader(str.reader()).read()
                    fun clean(xml: Any): Any {
                        return if (xml is XMLNode) {
                            for (i in xml.children.indices) {
                                xml.children[i] = clean(xml.children[i])
                            }
                            xml
                        } else xml.toString().trim()
                    }

                    val better = XMLWriter.write(
                        clean(xml) as XMLNode, indentation = null,
                        closeEmptyTypes = false, withHeader = true
                    )
                    val tmp = InnerTmpTextFile(better)
                    loadAIScene(file, tmp, flags, signature, callback)
                } else callback.err(err)
            }
        }

        // glb cannot be loaded from memory properly... (a bug in Assimp)
        val file = file.resolved()
        if (file !is FileFileRef && (signature == "gltf" || signature == "json")) {
            val tmp = FileFileRef.createTempFile(file.nameWithoutExtension, file.extension)
            tmp.deleteOnExit()
            file.copyTo(tmp) {
                loadAIScene(file, tmp, flags, signature, callback)
            }
            return
        }

        loadAIScene(file, file, flags, signature, callback)
    }

    private fun loadAIScene(
        srcFile: FileReference,
        dataFile: FileReference, flags: Int, signature: String?,
        callback: Callback<Pair<AIScene, Boolean>>
    ) {
        // we could load in parallel,
        // but we'd need to keep track of the scale factor;
        // it only is allowed to be set, if the file is a fbx file
        val isFBXFile = signature == "fbx"
        val scale = if (isFBXFile && aiGetVersionMajor() == 4) 0.01f else 1f
        val dataFile = dataFile.resolved()
        if (dataFile is FileFileRef) {
            val aiScene = synchronized(StaticMeshesLoader) {
                aiSetImportPropertyFloat(aiPropertyStore, AI_CONFIG_GLOBAL_SCALE_FACTOR_KEY, scale)
                aiImportFileExWithProperties(dataFile.absolutePath, flags, null, aiPropertyStore)
            }
            onGotAIScene(srcFile, aiScene, isFBXFile, callback)
        } else {
            dataFile.readByteBuffer(true) { byteBuffer, err ->
                if (byteBuffer != null) loadAIScene(srcFile, isFBXFile, byteBuffer, scale, flags, callback)
                else callback.err(err)
            }
        }
    }

    private fun loadAIScene(
        file0: FileReference, isFBXFile: Boolean, byteBuffer: ByteBuffer, scale: Float, flags: Int,
        callback: Callback<Pair<AIScene, Boolean>>
    ) {
        val aiScene = synchronized(StaticMeshesLoader) {
            aiSetImportPropertyFloat(aiPropertyStore, AI_CONFIG_GLOBAL_SCALE_FACTOR_KEY, scale)
            // the first method threw "bad allocation" somehow 🤷‍♂️
            aiImportFileFromMemoryWithProperties(byteBuffer, flags, null as ByteBuffer?, aiPropertyStore)
        }
        onGotAIScene(file0, aiScene, isFBXFile, callback)
    }

    private fun onGotAIScene(
        srcFile: FileReference, aiScene: AIScene?,
        isFBXFile: Boolean, callback: Callback<Pair<AIScene, Boolean>>
    ) {
        var error: String? = null
        if (aiScene == null) {
            error = aiGetErrorString()
        }

        // should be sync as well
        if (aiScene != null) callback.ok(Pair(aiScene, isFBXFile))
        else callback.err(IOException("Error loading model $srcFile, $error"))
    }

    fun createScene(aiScene: AIScene, sceneMeshes: List<FileReference>, hasSkeleton: Boolean): Pair<Prefab, Prefab> {

        val aiRoot = aiScene.mRootNode()!!
        val deepPrefab = Prefab("Entity")
        val flatPrefab = Prefab("Entity")

        val name = aiRoot.mName().dataString()
        if (!name.isBlank2()) {
            deepPrefab["name"] = name
            flatPrefab["name"] = name
        }

        val root = createSceneTree(aiRoot, name)
        root.countMeshes()

        val meshesByTransform = HashMap<Matrix4x3d, ArrayList<CreateSceneNode>>()
        createDeepPrefab(deepPrefab, root, meshesByTransform, hasSkeleton, sceneMeshes)
        createFlatPrefab(flatPrefab, meshesByTransform, hasSkeleton, sceneMeshes)

        loadLights(aiScene, deepPrefab) // todo test loading lights
        loadCameras(aiScene, deepPrefab) // todo test loading cameras
        return deepPrefab to flatPrefab
    }

    private fun createDeepPrefab(
        deepPrefab: Prefab, root: CreateSceneNode,
        meshesByTransform: HashMap<Matrix4x3d, ArrayList<CreateSceneNode>>,
        hasSkeleton: Boolean, sceneMeshes: List<FileReference>,
    ) {
        Recursion.processRecursive(root) { node, remaining ->
            val aiNode = node.aiNode
            val path = node.path

            val transform = Matrix4x3d()
            createSceneSetTransform(aiNode, deepPrefab, path, transform)
            createSceneAddMeshes(sceneMeshes, hasSkeleton, aiNode, deepPrefab, path)
            node.parent?.transform?.mul(transform, transform)
            node.transform = transform

            if (aiNode.mNumMeshes() > 0) {
                meshesByTransform.getOrPut(transform, ::ArrayList).add(node)
            }

            val childCount = aiNode.mNumChildren()
            if (childCount <= 0) return@processRecursive
            for (i in 0 until childCount) {
                val childNode = node.children[i]
                if (childNode.totalMeshes == 0) continue // skipped

                val originalName = childNode.originalName
                val pathName = childNode.pathName
                val childPath = deepPrefab.add(path, 'e', "Entity", pathName)
                if (originalName != pathName && originalName.isNotBlank2()) {
                    deepPrefab[childPath, "name"] = originalName
                }
                childNode.path = childPath
                remaining.add(childNode)
            }
        }
    }

    private fun createFlatPrefab(
        prefab: Prefab, meshesByTransform: Map<Matrix4x3d, List<CreateSceneNode>>,
        hasSkeleton: Boolean, sceneMeshes: List<FileReference>
    ) {
        var i = 0
        val usedNames0 = HashSet<String>()
        val isRoot = meshesByTransform.size == 1
        for ((transform, nodes) in meshesByTransform) {
            var name = nodes.map { it.originalName }
                .firstOrNull { it.isNotBlank2() && it !in usedNames0 }
                ?: "Node${i++}"
            name = nextName(name, usedNames0)

            val path = if (isRoot) ROOT_PATH
            else prefab.add(ROOT_PATH, 'e', "Entity", name)
            prefab[path, "name"] = name

            setTransform(prefab, path, transform)

            val usedNames = HashSet<String>()
            for (j in nodes.indices) {
                val node = nodes[j]
                val aiNode = node.aiNode
                val meshCount = aiNode.mNumMeshes()
                val rendererClass = if (hasSkeleton) "AnimMeshComponent" else "MeshComponent"
                val meshIndices = assertNotNull(aiNode.mMeshes())
                for (k in 0 until meshCount) {
                    val mesh = sceneMeshes[meshIndices[k]]
                    val compName = nextName(mesh.name, usedNames)
                    val meshComponent = prefab.add(path, 'c', rendererClass, compName)
                    prefab.setUnsafe(meshComponent, "meshFile", mesh)
                }
            }
        }
    }

    private fun createSceneTree(aiRoot: AINode, name: String): CreateSceneNode {
        val root = CreateSceneNode(null, aiRoot, "", name)
        Recursion.processRecursive(aiRoot to root) { (aiNode, node), remaining ->
            val childCount = aiNode.mNumChildren()
            if (childCount <= 0) return@processRecursive
            val children = assertNotNull(aiNode.mChildren())
            val usedNames = HashSet<String>(max(childCount, 16))

            if (aiRoot === aiNode) {
                usedNames.add("Cameras")
                usedNames.add("Lights")
            }

            for (i in 0 until childCount) {
                val aiChildNode = AINode.createSafe(children[i]) ?: continue
                val childNode = node.add(aiChildNode, usedNames)
                remaining.add(aiChildNode to childNode)
            }
        }
        return root
    }

    private fun createSceneSetTransform(aiNode: AINode, prefab: Prefab, path: Path, dst: Matrix4x3d) {
        assimpToJoml4x3d(aiNode.mTransformation(), dst)
        setTransform(prefab, path, dst)
    }

    private fun setTransform(prefab: Prefab, path: Path, transform: Matrix4x3d) {
        val localPosition = transform.getTranslation(Vector3d())
        if (localPosition.length() != 0.0) {
            prefab.setUnsafe(path, "position", localPosition)
        }

        val localRotation = transform.getUnnormalizedRotation(Quaterniond())
        if (localRotation.w != 1.0) {
            prefab.setUnsafe(path, "rotation", localRotation)
        }

        val localScale = transform.getScale(Vector3d())
        if (localScale.x != 1.0 || localScale.y != 1.0 || localScale.z != 1.0) {
            prefab.setUnsafe(path, "scale", localScale)
        }
    }

    private fun createSceneAddMeshes(
        sceneMeshes: List<FileReference>, hasSkeleton: Boolean,
        aiNode: AINode, prefab: Prefab, path: Path
    ) {
        val meshCount = aiNode.mNumMeshes()
        if (meshCount > 0) {
            val rendererClass = if (hasSkeleton) "AnimMeshComponent" else "MeshComponent"
            val meshIndices = assertNotNull(aiNode.mMeshes())
            val usedNames = HashSet<String>(max(meshCount, 16))
            for (i in 0 until meshCount) {
                val mesh = sceneMeshes[meshIndices[i]]
                val name = nextName(mesh.name, usedNames)
                val meshComponent = prefab.add(path, 'c', rendererClass, name)
                prefab.setUnsafe(meshComponent, "meshFile", mesh)
            }
        }
    }

    fun loadTextures(aiScene: AIScene, parentFolder: InnerFolder): List<FileReference> {
        val numTextures = aiScene.mNumTextures()
        return if (numTextures > 0) {
            val textures = aiScene.mTextures()!!
            val list = ArrayList<FileReference>(numTextures)
            for (it in 0 until numTextures) {
                val texture = AITexture.createSafe(textures[it]) ?: continue
                list += loadTexture(parentFolder, texture, it)
            }
            list
        } else emptyList()
    }

    fun loadMaterialPrefabs(
        aiScene: AIScene,
        texturesDir: FileReference,
        loadedTextures: List<FileReference>,
        missingFilesLookup: Map<String, FileReference>
    ): List<Prefab> {
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        val textureLookup = createTextureLookup(missingFilesLookup)
        return List(numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![it])
            processMaterialPrefab(
                aiScene, aiMaterial, loadedTextures, texturesDir,
                missingFilesLookup, textureLookup
            )
        }
    }

    private fun processIndices(aiMesh: AIMesh, indices: IntArray) {
        val numFaces = aiMesh.mNumFaces()
        val aiFaces = aiMesh.mFaces()
        val aiFace = AIFace.malloc()
        for (j in 0 until numFaces) {
            aiFaces.get(j, aiFace)
            val buffer = aiFace.mIndices()
            val i = j * 3
            when (buffer.remaining()) {
                1 -> {
                    // a point
                    indices[i] = buffer.get()
                    indices[i + 1] = indices[i + 0]
                    indices[i + 2] = indices[i + 0]
                }
                2 -> {
                    // a line
                    indices[i] = buffer.get()
                    indices[i + 1] = buffer.get()
                    indices[i + 2] = indices[i + 0]
                }
                3 -> {
                    // a triangle, as it should be by the triangulation flag
                    indices[i] = buffer.get()
                    indices[i + 1] = buffer.get()
                    indices[i + 2] = buffer.get()
                }
            }
        }
        aiFace.free()
    }

    private fun processMaterialPrefab(
        aiScene: AIScene,
        aiMaterial: AIMaterial,
        loadedTextures: List<FileReference>,
        texturesDir: FileReference,
        missingFilesLookup: Map<String, FileReference>,
        textureLookup: List<FileReference>,
    ): Prefab {

        val prefab = Prefab("Material")
        val color = AIColor4D.create()
        // val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        // val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        // get the name...
        val nameStr = AIString.calloc()
        aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, nameStr)
        val name = nameStr.dataString()
        prefab["name"] = name

        LOGGER.debug("Material $name")

        var opacity = getFloat(aiMaterial, AI_MATKEY_OPACITY, 1f)
        if (opacity == 0f) opacity = 1f // completely transparent makes no sense

        val diffuseMap = findTexture(
            aiScene, aiMaterial, loadedTextures, aiTextureType_DIFFUSE,
            texturesDir, missingFilesLookup, textureLookup
        )
        if (diffuseMap != InvalidRef) {
            prefab["diffuseMap"] = diffuseMap
            // I wish there was a better way; opacity isn't guaranteed to be set to < 1
            // (tested with source files for SyntyStore "Ancient Empire Temple" wall meshes)
            if (name.contains("glass", true)) {
                prefab["pipelineStage"] = GLASS_PASS
            }
            if (opacity != 1f) prefab["diffuseBase"] = Vector4f(1f, 1f, 1f, opacity)
        } else {
            // I think the else-if is the correct thing here; the storm-trooper is too dark otherwise
            // diffuse
            val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
            val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)
            val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
            val transparent = getColor(aiMaterial, color, AI_MATKEY_COLOR_TRANSPARENT)
            val reflective = getColor(aiMaterial, color, AI_MATKEY_COLOR_REFLECTIVE)
            val transparency = getFloat(aiMaterial, AI_MATKEY_TRANSPARENCYFACTOR, 1f)
            LOGGER.debug("  opacity: {}", opacity)
            LOGGER.debug(
                "  diffuse: {}, specular: {}, ambient: {}, map: {}",
                diffuse, specular, ambient, diffuseMap
            )
            LOGGER.debug(
                "  transparent: {}, reflective: {}, trans-factor: {}",
                transparent, reflective, transparency
            )
            if (diffuse != null) {
                diffuse.w = opacity
                prefab["diffuseBase"] = diffuse
            } else if (opacity != 1f) {
                prefab["diffuseBase"] = Vector4f(1f, 1f, 1f, opacity)
            }
        }

        // emissive
        val emissive = getColor(aiMaterial, color, AI_MATKEY_COLOR_EMISSIVE)
        LOGGER.debug("  emissive: {}", emissive)
        if (emissive != null) {
            emissive.mul(20f) // for brighter colors; 5.0 is our default because of Reinhard tonemapping
            // 4x, because we want it to be impressive ^^, and to actually feel like glowing;
            // the original 1 should be 100%, so I think it's kind of appropriate
            prefab["emissiveBase"] = Vector3f(emissive.x, emissive.y, emissive.z)
        }

        val emissiveMap = findTexture(
            aiScene, aiMaterial, loadedTextures, aiTextureType_EMISSIVE,
            texturesDir, missingFilesLookup, textureLookup
        )
        if (emissiveMap != InvalidRef) prefab["emissiveMap"] = emissiveMap

        // normal
        val normalMap = findTexture(
            aiScene, aiMaterial, loadedTextures, aiTextureType_NORMALS,
            texturesDir, missingFilesLookup, textureLookup
        )
        if (normalMap != InvalidRef) prefab["normalMap"] = normalMap

        // metallic / roughness
        val metallicRoughness = findTexture(
            aiScene, aiMaterial, loadedTextures,
            AI_MATKEY_GLTF_PBRMETALLICROUGHNESS_METALLICROUGHNESS_TEXTURE, texturesDir,
            missingFilesLookup, textureLookup
        )

        val ior = getFloat(aiMaterial, AI_MATKEY_REFRACTI, 1f)
        if (ior > 1f) prefab["indexOfRefraction"] = ior

        if (metallicRoughness != InvalidRef) {
            prefab["metallicMap"] = metallicRoughness.getChild("b.png")
            prefab["roughnessMap"] = metallicRoughness.getChild("g.png")
            prefab["roughnessMinMax"] = Vector2f(0.1f, 1f)
            prefab["metallicMinMax"] = Vector2f(0f, 1f)
        } else {

            // assimp only supports a single roughness/metallic property :/
            // done: read materials manually for fbx -> looking at OpenFBX, FBX only has the specular/shininess/reflective-workflow

            // roughness
            // AI_MATKEY_SHININESS as color, .r: 360, 500, so the exponent?
            val shininessExponent = getFloat(aiMaterial, AI_MATKEY_SHININESS, 1f)
            // val shininessStrength = getFloat(aiMaterial, AI_MATKEY_SHININESS_STRENGTH) // always 0.0
            // LOGGER.info("roughness: $shininess x $shininessStrength")
            val roughnessBase = shininessToRoughness(shininessExponent)
            prefab["roughnessMinMax"] = Vector2f(0f, roughnessBase)

            val metallic = getFloat(aiMaterial, AI_MATKEY_REFLECTIVITY, 0f) // 0.0, rarely 0.5
            if (metallic != 0f) prefab["metallicMinMax"] = Vector2f(0f, metallic)
        }

        // other stuff
        val displacementMap = findTexture(
            aiScene, aiMaterial, loadedTextures, aiTextureType_DISPLACEMENT,
            texturesDir, missingFilesLookup, textureLookup
        )
        val occlusionMap = findTexture(
            aiScene, aiMaterial, loadedTextures, aiTextureType_LIGHTMAP,
            texturesDir, missingFilesLookup, textureLookup
        )
        if (displacementMap != InvalidRef) prefab["displacementMap"] = displacementMap
        if (occlusionMap != InvalidRef) prefab["occlusionMap"] = occlusionMap

        return prefab
    }

    private val pMax = intArrayOf(1)

    fun getFloat(aiMaterial: AIMaterial, key: String, default: Float): Float {
        val a = FloatArray(1)
        return if (aiGetMaterialFloatArray(aiMaterial, key, aiTextureType_NONE, 0, a, pMax) != 1) default
        else a[0]
    }

    private fun findTexture(
        aiScene: AIScene,
        aiMaterial: AIMaterial,
        loadedTextures: List<FileReference>,
        type: Int,
        parentFolder: FileReference,
        missingFilesLookup: Map<String, FileReference>,
        textureLookup: List<FileReference>
    ): FileReference {
        val path = AIString.calloc()
        aiGetMaterialTexture(
            aiMaterial, type, 0, path, null as IntBuffer?,
            null, null, null, null, null
        )

        var path0 = path.dataString() ?: return InvalidRef
        if (path0.isBlank2()) return InvalidRef

        var parentFolder = parentFolder
        while (path0.startsWith("../") || path0.startsWith("..\\")) {
            path0 = path0.substring(3)
            parentFolder = parentFolder.getParent().nullIfUndefined() ?: parentFolder
        }

        path0 = sanitizePath(path0)
        if (path0.startsWith('*')) {
            return resolveStarFile(path0, aiScene, loadedTextures)
        }

        // check whether it may be a global path, not a local one
        // todo does this work properly on Linux???
        val maybePath =
            if (':' in path0) getReference(path0)
            else parentFolder.getChild(path0)

        // if the path does not exist, check whether the name matches with any internal texture
        if (maybePath == InvalidRef) return InvalidRef
        if (maybePath.exists) return maybePath

        return loadedTextures.firstOrNull { it.name == maybePath.name }
            ?: loadedTextures.firstOrNull { it.name.equals(maybePath.name, true) }
            ?: loadedTextures.firstOrNull { it.nameWithoutExtension == maybePath.nameWithoutExtension }
            ?: loadedTextures.firstOrNull {
                it.nameWithoutExtension.equals(maybePath.nameWithoutExtension, true)
            } ?: missingFilesLookup[maybePath.name]
            ?: missingFilesLookup[maybePath.nameWithoutExtension + "_A"] // for files from Synty Store
            ?: findSyntyStoreCandidate(path0, textureLookup)
            ?: maybePath
    }

    /**
     * more specialized code for Synthy Store assets ^^
     * */
    private fun findSyntyStoreCandidate(path0: String, textureLookup: List<FileReference>): FileReference? {
        val prefix = "_Texture_"
        val i0 = path0.lastIndexOf(prefix) + prefix.length
        if (i0 < prefix.length) return null
        val i1 = path0.lastIndexOf('.')
        if (i1 <= i0) return null

        val sub = path0.substring(i0, i1)
        val isNormal = path0.contains("normal", true)
        val candidate1 = textureLookup
            .minByOrNull {
                val sub1 = it.nameWithoutExtension
                val idx = sub1.indexOf("_Texture_")
                val sub2 = sub1.substring(idx + "_Texture_".length)
                val isLikelyNormal = sub1.contains("normal", true)
                sub.distance(sub2) + if (isNormal == isLikelyNormal) 0
                else 10 // penalty for being different type probably
            }
        if (candidate1 != null) {
            LOGGER.debug("Resolved {} to {}", path0, candidate1)
        }
        return candidate1
    }

    private fun resolveStarFile(path0: String, aiScene: AIScene, loadedTextures: List<FileReference>): FileReference {
        val index = path0.substring(1).toIntOrNull() ?: return InvalidRef
        if (index !in 0 until aiScene.mNumTextures()) return InvalidRef
        return loadedTextures.getOrNull(index) ?: InvalidRef
    }

    private fun createTextureLookup(missingFilesLookup: Map<String, FileReference>): List<FileReference> {
        return missingFilesLookup.values.filter { it.lcExtension in InnerFolderCache.imageFormats1 && "_Texture_" in it.nameWithoutExtension }
    }

    private fun loadTexture(parentFolder: InnerFolder, texture: AITexture, index: Int): InnerFile {
        // ("file name: ${texture.mFilename().dataString()}")
        // val hintBuffer = texture.achFormatHint()
        // ("format hints: ${hintBuffer.toByteArray().joinToString()}, ${texture.achFormatHintString()}")
        // ("${texture.mWidth()} x ${texture.mHeight()}")

        val width = texture.mWidth()
        val height = texture.mHeight()
        val isCompressed = height == 0

        val size = if (isCompressed) width else width * height * 4

        // lwjgl 3.3.1
        /*private fun bufferToBytes(buffer: ByteBuffer, size: Int): ByteArray {
            val bytes = ByteArray(size)
            buffer.get(bytes, 0, min(buffer.remaining(), size))
            return bytes
        }
        val data = if (isCompressed) {
            bufferToBytes(texture.pcDataCompressed(), size)
        } else {
            bufferToBytes(texture.pcData(), size)
        }*/

        // lwjgl 3.2.3
        val data = bufferToBytes(texture.pcData(size / 4), size)

        // ensure uniqueness of file name
        var fileExt0 = ""
        val fileName0i = texture.mFilename().dataString()
        val fileName0 = fileName0i.run {
            if (isEmpty()) {
                fileExt0 = if (isCompressed) Signature.findName(data) ?: "png" else "bmp"
                index.toString()
            } else {
                val di = lastIndexOf('.')
                if (di < 0) this
                else {
                    fileExt0 = substring(di + 1)
                    substring(0, di)
                }
            }
        }

        val fileName = findNextFileName(parentFolder, fileName0, fileExt0, 3, '-')

        LOGGER.debug("Creating texture $fileName0, $fileExt0 -> $fileName, $isCompressed")
        return if (isCompressed) {
            // width is the buffer size in bytes
            // the last bytes will be filled automatically with zeros :)
            parentFolder.createByteChild(fileName, data)
        } else {
            // if not compressed, get data as raw, and save it to bmp or sth like that
            // best possible format: raw
            // ARGB8888
            // check whether image actually has alpha channel
            parentFolder.createImageChild(fileName, ByteImage(width, height, ByteImageFormat.ARGB, data))
        }
    }

    private fun bufferToBytes(buffer: AITexel.Buffer, size: Int): ByteArray {
        val bytes = ByteArray(size)
        if (buffer.remaining() != size / 4) {
            LOGGER.warn("Size doesn't match, ${buffer.position()}, ${buffer.capacity()}, ${buffer.remaining()} vs ${size / 4}")
        }
        // ~10x faster than doing a Java-like copy
        val unsafeBuffer = MemoryUtil.memByteBuffer(buffer.address(), size)
        unsafeBuffer.get(bytes)
        return bytes
    }

    fun getColor(aiMaterial: AIMaterial, color: AIColor4D, flag: String): Vector4f? {
        val result = aiGetMaterialColor(aiMaterial, flag, aiTextureType_NONE, 0, color)
        return if (result == 0) {
            // colors are linear in Assimp, sRGB in Rem's Engine
            Vector4f(toSRGB(color.r()), toSRGB(color.g()), toSRGB(color.b()), color.a())
        } else null
    }

    fun createMeshPrefab(aiMesh: AIMesh, materials: List<FileReference>): Prefab {

        val vertexCount = aiMesh.mNumVertices()

        val positions = FloatArray(vertexCount * 3)
        val indices = IntArray(aiMesh.mNumFaces() * 3)
        processPositions(aiMesh, positions)
        processIndices(aiMesh, indices)

        val prefab = Prefab("Mesh")
        val name = aiMesh.mName().dataString()
        if (name.isNotEmpty()) {
            prefab["name"] = name
        }

        prefab["positions"] = positions
        prefab["indices"] = indices

        val normals = processNormals(aiMesh, vertexCount)
        if (normals != null) {
            prefab["normals"] = normals
            val tangents = processTangents(aiMesh, vertexCount, normals)
            if (tangents != null) {
                prefab["tangents"] = tangents
            }
        }

        val uvs = processUVs(aiMesh, vertexCount)
        if (uvs != null && uvs.any { it != 0f }) {
            prefab["uvs"] = uvs
        }

        for (i in 0 until aiMesh.mColors().remaining()) {
            val colorI = processVertexColors(aiMesh, i, vertexCount)
            if (colorI != null && colorI.any { it != -1 }) {
                prefab[if (i == 0) "colors0" else "colors$i"] = colorI
            }
        }

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx in materials.indices) {
            val ref = materials[materialIdx]
            prefab["materials"] = listOf(ref)
        }

        return prefab
    }

    private fun processTangents(aiMesh: AIMesh, vertexCount: Int, normals: FloatArray): FloatArray? {
        val src = aiMesh.mTangents()
        val src2 = aiMesh.mBitangents()
        return if (src != null && src2 != null && vertexCount > 0)
            processTangents(normals, src, src2, FloatArray(vertexCount * 4)) else null
    }

    private fun processNormals(aiMesh: AIMesh, vertexCount: Int): FloatArray? {
        val src = aiMesh.mNormals()
        return if (src != null && vertexCount > 0)
            processVec3(src, FloatArray(vertexCount * 3)) else null
    }

    private fun processUVs(aiMesh: AIMesh, vertexCount: Int): FloatArray? {
        val src = aiMesh.mTextureCoords(0)
        return if (src != null) {
            val vec = AIVector3D.malloc()
            val dst = FloatArray(vertexCount * 2)
            forLoop(0, dst.size, 2) { j ->
                src.get(vec)
                dst[j] = vec.x()
                dst[j + 1] = vec.y()
            }
            vec.free()
            dst
        } else null
    }

    private fun processPositions(aiMesh: AIMesh, dst: FloatArray) =
        processVec3(aiMesh.mVertices(), dst)

    fun processPositions(aiMesh: AIAnimMesh, dst: FloatArray) =
        processVec3(aiMesh.mVertices()!!, dst)

    private fun processVec3(src: AIVector3D.Buffer, dst: FloatArray): FloatArray {
        val vec = AIVector3D.malloc()
        forLoop(0, dst.size, 3) { j ->
            src.get(vec)
            dst[j] = vec.x()
            dst[j + 1] = vec.y()
            dst[j + 2] = vec.z()
        }
        vec.free()
        return dst
    }

    private fun processTangents(
        normals: FloatArray,
        tangents: AIVector3D.Buffer,
        bitangents: AIVector3D.Buffer,
        dst: FloatArray
    ): FloatArray {
        var i = 0
        val vec = AIVector3D.malloc()
        forLoop(0, dst.size, 4) { j ->
            tangents.get(vec)
            val tx = vec.x()
            val ty = vec.y()
            val tz = vec.z()
            dst[j] = tx
            dst[j + 1] = ty
            dst[j + 2] = tz
            val nx = normals[i++]
            val ny = normals[i++]
            val nz = normals[i++]
            bitangents.get(vec)
            dst[j + 3] = sign(crossDot(nx, ny, nz, tx, ty, tz, vec.x(), vec.y(), vec.z()))
        }
        vec.free()
        return dst
    }

    // custom function, because there may be NaNs
    // (NewSponza_Main_Blender_glTF.gltf from Intel contains NaNs)
    private fun f2i(v: Float): Int {
        return if (v <= 0f) 0
        else if (v < 1f) (v * 255).roundToInt() // is NaN-safe
        else 255
    }

    private fun processVertexColors(aiMesh: AIMesh, index: Int, vertexCount: Int): IntArray? {
        val src = aiMesh.mColors(index)
        return if (src != null) {
            val vec = AIColor4D.malloc()
            val dst = IntArray(vertexCount)
            for (j in 0 until vertexCount) {
                // since all colors are linear in assimp, we probably need to convert them here, too, don't we???
                //  test that somehow... maybe with orange-color in Blender -> fbx -> see result here
                // -> idk, looks fine for now; I'm not too sure though...
                src.get(vec)
                val r = f2i(vec.r())
                val g = f2i(vec.g())
                val b = f2i(vec.b())
                val a = f2i(vec.a())
                dst[j] = rgba(r, g, b, a)
            }
            vec.free()
            // when every pixel is black or white, it doesn't actually have data
            if (dst.all { it == -1 } || dst.all { it == 0 }) return null
            dst
        } else null
    }

    fun assimpToJoml4x3f(m: AIMatrix4x4, dst: Matrix4x3f = Matrix4x3f()): Matrix4x3f {
        return dst.set(
            m.a1(), m.b1(), m.c1(),
            m.a2(), m.b2(), m.c2(),
            m.a3(), m.b3(), m.c3(),
            m.a4(), m.b4(), m.c4(),
        )
    }

    fun assimpToJoml4x3d(m: AIMatrix4x4, dst: Matrix4x3d = Matrix4x3d()): Matrix4x3d {
        return dst.set(
            m.a1().toDouble(), m.b1().toDouble(), m.c1().toDouble(),
            m.a2().toDouble(), m.b2().toDouble(), m.c2().toDouble(),
            m.a3().toDouble(), m.b3().toDouble(), m.c3().toDouble(),
            m.a4().toDouble(), m.b4().toDouble(), m.c4().toDouble(),
        )
    }

    private fun loadCameras(aiScene: AIScene, prefab: Prefab) {
        if (aiScene.mNumCameras() <= 0) return
        // todo cameras might already exist!!!
        val cameras = prefab.add(ROOT_PATH, 'e', "Entity", "Cameras")
        val aiCameras = aiScene.mCameras()!!
        for (i in 0 until aiScene.mNumCameras()) {
            createCameraPrefab(AICamera.create(aiCameras[i]), prefab, cameras, i)
        }
    }

    private fun createCameraPrefab(aiCamera: AICamera, prefab: Prefab, path: Path, i: Int) {
        // is there really no orthographic support???
        val entity = prefab.add(path, 'e', "Entity", "Camera[$i]")
        val camera = prefab.add(entity, 'c', "Camera", "Camera")
        entity["name"] = aiCamera.mName().dataString()
        camera["far"] = aiCamera.mClipPlaneFar().toDouble()
        camera["near"] = aiCamera.mClipPlaneNear().toDouble()
        val pos = aiCamera.mPosition()
        entity["position"] = Vector3d(pos.x(), pos.y(), pos.z())
        val halfR = aiCamera.mHorizontalFOV() // half angle in radians
        val aspect = aiCamera.mAspect()
        camera["fovY"] = (atan(tan(halfR) / aspect) * 2.0).toDegrees()
        // use these two to calculate the direction
        val up = aiCamera.mUp()
        val lookAt = aiCamera.mLookAt()
        entity["rotation"] = Quaterniond().lookAlong(
            Vector3d(lookAt.x() - pos.x(), lookAt.y() - pos.y(), lookAt.z() - pos.z()),
            Vector3d(up.x(), up.y(), up.z())
        )
    }

    private fun loadLights(aiScene: AIScene, prefab: Prefab) {
        if (aiScene.mNumLights() <= 0) return
        // todo lights node might already exist!
        val lights = prefab.add(ROOT_PATH, 'e', "Entity", "Lights")
        val aiLights = aiScene.mLights()!!
        for (i in 0 until aiScene.mNumLights()) {
            createLightPrefab(AILight.create(aiLights[i]), prefab, lights, i)
        }
    }

    private fun createLightPrefab(aiLight: AILight, prefab: Prefab, path: Path, i: Int) {
        val entity = prefab.add(path, 'e', "Entity", "Light[$i]")
        val clazz = when (aiLight.mType()) {
            1 -> "DirectionalLight"
            2 -> "PointLight"
            3 -> "SpotLight"
            // 4 -> "AmbientLight" -> not really supported
            5 -> "RectangleLight"
            else -> "PointLight" // undefined
        }
        val light = prefab.add(entity, 'c', clazz, "Light")
        entity["name"] = aiLight.mName().dataString()
        if (clazz == "SpotLight") {
            // correct?
            // aiLight.mAngleInnerCone() // - not yet supported
            light["coneAngle"] = atan(aiLight.mAngleOuterCone())
        }
        var maxBrightness = 1f
        if (clazz != "DirectionalLight") {
            // brightness = 1/(constant + linear*x + quadratic*x²)
            // we solve for 0.1 as a cutoff distance...
            val cutoff = 0.1f
            // 0.1 = 1/(constant + linear*x + quadratic*x²)
            // -> 10 = (constant + linear*x + quadratic*x²)
            // -> constant + linear*x + quadratic*x² - 10 = 0
            maxBrightness = 1f / max(aiLight.mAttenuationConstant(), 1e3f) // ok so?
            // aiLight.mAttenuationLinear()
            // aiLight.mAttenuationConstant()
            // aiLight.mAttenuationQuadratic()
            val solution = FloatArray(2)
            val numSolutions = solveQuadratic(
                solution, aiLight.mAttenuationQuadratic(), aiLight.mAttenuationLinear(),
                aiLight.mAttenuationConstant() + 1f / cutoff
            )
            if (numSolutions > 0) {
                val dist = max(solution[0], solution[1])
                entity["scale"] = Vector3d(dist.toDouble())
            }
        }
        // so we choose diffuse?
        // aiLight.mColorAmbient()
        val color = aiLight.mColorDiffuse()
        // aiLight.mColorSpecular()
        light["color"] = Vector3f(color.r(), color.g(), color.b()).mul(maxBrightness)
        val pos = aiLight.mPosition()
        entity["position"] = Vector3d(pos.x(), pos.y(), pos.z())
        // transform this into a rotation
        if (clazz != "PointLight") { // undefined for point lights
            val up = aiLight.mUp()
            val dir = aiLight.mDirection()
            entity["rotation"] = Quaterniond().lookAlong(
                Vector3d(dir.x(), dir.y(), dir.z()),
                Vector3d(up.x(), up.y(), up.z())
            )
        }
    }
}