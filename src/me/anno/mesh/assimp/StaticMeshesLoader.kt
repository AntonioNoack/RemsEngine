package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.Prefab
import me.anno.image.raw.ByteImage
import me.anno.image.raw.ByteImage.Companion.hasAlpha
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.zip.InnerFile
import me.anno.io.zip.InnerFolder
import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.mesh.assimp.io.AIFileIOImpl
import me.anno.utils.Color.rgba
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer


open class StaticMeshesLoader {

    companion object {

        /*val defaultFlags = aiProcess_GenSmoothNormals or aiProcess_JoinIdenticalVertices or aiProcess_Triangulate or
                aiProcess_FixInfacingNormals or aiProcess_GlobalScale*/

        val defaultFlags = aiProcess_GenSmoothNormals or // if the normals are unavailable, generate smooth ones
                aiProcess_Triangulate or // we don't want to triangulate ourselves
                aiProcess_JoinIdenticalVertices or // is required to load indexed geometry
                aiProcess_FixInfacingNormals or // is recommended, may be incorrect...
                aiProcess_GlobalScale

        // or aiProcess_PreTransformVertices // <- disables animations
        private val LOGGER = LogManager.getLogger(StaticMeshesLoader::class)

    }

    /*fun fileToBuffer(name: String, termination: Boolean): ByteBuffer {
        val bytes = name.toByteArray()
        var size = bytes.size
        if (termination) size++
        val byteBuffer = MemoryUtil.memAlloc(size)
        byteBuffer.put(bytes)
        if (termination) byteBuffer.put(0)
        byteBuffer.flip()
        return byteBuffer
    }

    fun fileToBuffer(resourcePath: FileReference, termination: Boolean): ByteBuffer {
        val bytes = resourcePath.inputStream().readBytes()
        var size = bytes.size
        if (termination) size++
        val byteBuffer = MemoryUtil.memAlloc(size)
        byteBuffer.put(bytes)
        if (termination) byteBuffer.put(0)
        byteBuffer.flip()
        return byteBuffer
    }*/

    fun loadFile(file: FileReference, flags: Int): AIScene {
        return if (file is FileFileRef || file.absolutePath.count { it == '.' } <= 1) {
            aiImportFile(file.absolutePath, flags)
        } else {
            val fileIO = AIFileIOImpl.create(file, file.getParent()!!)
            aiImportFileEx(file.name, flags, fileIO)
        } ?: throw Exception("Error loading model $file, ${aiGetErrorString()}")
    }

    fun load(file: FileReference): AnimGameItem = read(file, file.getParent() ?: InvalidRef, defaultFlags)

    open fun read(file: FileReference, resources: FileReference, flags: Int = defaultFlags): AnimGameItem {
        val asFolder = AnimatedMeshesLoader.readAsFolder2(file, resources, flags)
        val prefab = asFolder.second
        val instance = prefab.createInstance() as Entity
        return AnimGameItem(instance)
    }

    private fun buildScene(
        aiScene: AIScene,
        sceneMeshes: List<FileReference>,
        hasSkeleton: Boolean,
        aiNode: AINode
    ): Prefab {
        val prefab = Prefab("Entity")
        val name = aiNode.mName().dataString()
        if (!name.isBlank2())
            prefab.add(CSet(Path.ROOT_PATH, "name", name))
        buildScene(aiScene, sceneMeshes, hasSkeleton, aiNode, prefab, Path.ROOT_PATH)
        return prefab
    }

    private fun buildScene(
        aiScene: AIScene,
        sceneMeshes: List<FileReference>,
        hasSkeleton: Boolean,
        aiNode: AINode,
        prefab: Prefab,
        path: Path
    ) {

        val transform = Transform()
        transform.setLocal(convert(aiNode.mTransformation()))

        val localPosition = transform.localPosition
        if (localPosition.length() != 0.0)
            prefab.add(CSet(path, "position", localPosition))

        val localRotation = transform.localRotation
        if (localRotation.w != 1.0)
            prefab.add(CSet(path, "rotation", localRotation))

        val localScale = transform.localScale
        if (localScale.x != 1.0 || localScale.y != 1.0 || localScale.z != 1.0)
            prefab.add(CSet(path, "scale", localScale))

        val meshCount = aiNode.mNumMeshes()
        if (meshCount > 0) {

            val meshIndices = aiNode.mMeshes()!!
            for (i in 0 until meshCount) {
                val mesh = sceneMeshes[meshIndices[i]]
                val meshComponent = CAdd(path, 'c', "MeshComponent", mesh.name)
                prefab.add(meshComponent)
                prefab.add(CSet(meshComponent.getChildPath(i), "mesh", mesh))
            }

            val rendererClass = if (hasSkeleton) "AnimRenderer" else "MeshRenderer"
            prefab.add(CAdd(path, 'c', rendererClass, "Renderer"))

        }

        val childCount = aiNode.mNumChildren()
        if (childCount > 0) {
            val children = aiNode.mChildren()!!
            for (i in 0 until childCount) {
                val childNode = AINode.create(children[i])
                val childName = childNode.mName().dataString()
                val add = CAdd(path, 'e', "Entity", childName)
                prefab.add(add)
                val childPath = add.getChildPath(i)
                buildScene(aiScene, sceneMeshes, hasSkeleton, childNode, prefab, childPath)
            }
        }

    }

    fun buildScene(aiScene: AIScene, sceneMeshes: List<FileReference>, hasSkeleton: Boolean): Prefab {
        return buildScene(aiScene, sceneMeshes, hasSkeleton, aiScene.mRootNode()!!)
    }

    fun loadTextures(
        aiScene: AIScene,
        parentFolder: InnerFolder
    ): List<FileReference> {
        val numTextures = aiScene.mNumTextures()
        return if (numTextures > 0) {
            val textures = aiScene.mTextures()!!
            val list = ArrayList<FileReference>(numTextures)
            for (it in 0 until numTextures) {
                list += loadTexture(parentFolder, AITexture.create(textures[it]), it)
            }
            list
        } else emptyList()
    }

    fun loadMaterialPrefabs(
        aiScene: AIScene,
        texturesDir: FileReference,
        loadedTextures: List<FileReference>
    ): Array<Prefab> {
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        return Array(numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![it])
            processMaterialPrefab(aiScene, aiMaterial, loadedTextures, texturesDir)
        }
    }

    fun processIndices(aiMesh: AIMesh, indices: IntArray) {
        val numFaces = aiMesh.mNumFaces()
        val aiFaces = aiMesh.mFaces()
        for (j in 0 until numFaces) {
            val aiFace = aiFaces[j]
            val buffer = aiFace.mIndices()
            val i = j * 3
            when (val remaining = buffer.remaining()) {
                1 -> {
                    // a point
                    indices[i + 0] = buffer.get()
                    indices[i + 1] = indices[i + 0]
                    indices[i + 2] = indices[i + 0]
                }
                2 -> {
                    // a line
                    indices[i + 0] = buffer.get()
                    indices[i + 1] = buffer.get()
                    indices[i + 2] = indices[i + 0]
                }
                3 -> {
                    // a triangle, as it should be by the triangulate flag
                    indices[i + 0] = buffer.get()
                    indices[i + 1] = buffer.get()
                    indices[i + 2] = buffer.get()
                }
                else -> {
                    LOGGER.warn("Remaining number of vertices is awkward: $remaining")
                }
            }
        }
    }

    fun processMaterialPrefab(
        aiScene: AIScene,
        aiMaterial: AIMaterial,
        loadedTextures: List<FileReference>,
        texturesDir: FileReference
    ): Prefab {

        val prefab = Prefab("Material")
        val color = AIColor4D.create()
        // val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        // val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        // get the name...
        val name = AIString.calloc()
        aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, name)
        prefab.setProperty("name", name.dataString())

        val diffuseMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DIFFUSE, texturesDir)
        if (diffuseMap != InvalidRef) prefab.setProperty("diffuseMap", diffuseMap)
        else {// I think the else-if is the correct thing here; the storm-trooper is too dark otherwise

            var opacity = getFloat(aiMaterial, AI_MATKEY_OPACITY)
            if (opacity == 0f) opacity = 1f // completely transparent makes no sense
            // todo can we check whether this key is actually set? or change the default value?
            // LOGGER.info("opacity: $opacity")

            // diffuse
            val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
            if (diffuse != null) {
                diffuse.w = opacity
                prefab.setProperty("diffuseBase", diffuse)
            } else if (opacity != 1f) {
                prefab.setProperty("diffuseBase", Vector4f(1f, 1f, 1f, opacity))
            }
        }

        // emissive
        val emissive = getColor(aiMaterial, color, AI_MATKEY_COLOR_EMISSIVE)
        if (emissive != null) prefab.setProperty("emissiveBase", Vector3f(emissive.x, emissive.y, emissive.z))
        val emissiveMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_EMISSIVE, texturesDir)
        if (emissiveMap != InvalidRef) prefab.setProperty("emissiveMap", emissiveMap)

        // normal
        val normalMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_NORMALS, texturesDir)
        if (normalMap != InvalidRef) prefab.setProperty("normalMap", normalMap)

        /*val baseColor2 = getPath(
            aiScene, aiMaterial, loadedTextures,
            AI_MATKEY_GLTF_PBRMETALLICROUGHNESS_BASE_COLOR_TEXTURE, texturesDir
        )*/
        // println("base color 2: $baseColor2")

        val metallicRoughness = getPath(
            aiScene, aiMaterial, loadedTextures,
            AI_MATKEY_GLTF_PBRMETALLICROUGHNESS_METALLICROUGHNESS_TEXTURE, texturesDir
        )

        if (metallicRoughness != InvalidRef) {
            prefab.setProperty("metallicMap", getReference(metallicRoughness, "b.png"))
            prefab.setProperty("roughnessMap", getReference(metallicRoughness, "g.png"))
            prefab.setProperty("roughnessMinMax", Vector2f(0.1f, 1f))
            prefab.setProperty("metallicMinMax", Vector2f(0f, 1f))
        }

        if (metallicRoughness == InvalidRef) {

            // todo these settings seem wrong... what is actually metallic/roughness?

            // roughness
            // AI_MATKEY_SHININESS as color, .r: 360, 500, so the exponent?
            val shininessExponent = getFloat(aiMaterial, AI_MATKEY_SHININESS)
            // val shininessStrength = getFloat(aiMaterial, AI_MATKEY_SHININESS_STRENGTH) // always 0.0
            // LOGGER.info("roughness: $shininess x $shininessStrength")
            val roughnessBase = shininessToRoughness(shininessExponent)
            if (roughnessBase != 1f) prefab.setProperty("roughnessMinMax", Vector2f(0f, roughnessBase))

            // metallic
            // val metallic0 = getColor(aiMaterial, color, AI_MATKEY_COLOR_REFLECTIVE) // always null
            val metallic = getFloat(aiMaterial, AI_MATKEY_REFLECTIVITY) // 0.0, rarely 0.5
            if (metallic != 0f) prefab.setProperty("metallicMinMax", Vector2f(0f, metallic))
            // LOGGER.info("metallic: $metallic, roughness: $roughnessBase")

        }
        // LOGGER.info("metallic: $metallic0 x $metallic")

        /*for(i in 0 until aiMaterial.mNumProperties()){
            val property = AIMaterialProperty.create(aiMaterial.mProperties()[i])
            val key = property.mKey().dataString()
            val index = property.mIndex()
            val length = property.mDataLength()
            val data = property.mData()
            val semantic = property.mSemantic()
            val type = property.mType()
            println("$key,$index,$length,$semantic,$type")
        }*/

        // other stuff
        val displacementMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DISPLACEMENT, texturesDir)
        val occlusionMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_LIGHTMAP, texturesDir)
        if (displacementMap != InvalidRef) prefab.setProperty("displacementMap", displacementMap)
        if (occlusionMap != InvalidRef) prefab.setProperty("occlusionMap", occlusionMap)

        return prefab
    }

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

    fun getFloat(aiMaterial: AIMaterial, key: String): Float {
        val a = FloatArray(1)
        aiGetMaterialFloatArray(aiMaterial, key, aiTextureType_NONE, 0, a, IntArray(1) { 1 })
        return a[0]
    }

    fun getPath(
        aiScene: AIScene,
        aiMaterial: AIMaterial,
        loadedTextures: List<FileReference>,
        type: Int,
        parentFolder: FileReference
    ): FileReference {
        val path = AIString.calloc()
        aiGetMaterialTexture(
            aiMaterial, type, 0, path, null as IntBuffer?,
            null, null, null, null, null
        )
        val path0 = path.dataString() ?: return InvalidRef
        if (path0.isBlank2()) return InvalidRef
        if (path0.startsWith('*')) {
            val index = path0.substring(1).toIntOrNull() ?: return InvalidRef
            if (index !in 0 until aiScene.mNumTextures()) return InvalidRef
            return loadedTextures.getOrNull(index) ?: InvalidRef
        }
        // replace double slashes
        val path1 = path0.replace("//", "/")
        // check whether it may be a global path, not a local one
        val maybePath = if (':' in path1) getReference(path1) else parentFolder.getChild(path1)
        // if the path does not exist, check whether the name matches with any internal texture
        return if (maybePath.exists) maybePath
        else loadedTextures.firstOrNull { it.name == maybePath.name }
            ?: loadedTextures.firstOrNull { it.name.equals(maybePath.name, true) }
            ?: loadedTextures.firstOrNull { it.nameWithoutExtension == maybePath.nameWithoutExtension }
            ?: loadedTextures.firstOrNull { it.nameWithoutExtension.equals(maybePath.nameWithoutExtension, true) }
            ?: maybePath
    }

    fun loadTexture(parentFolder: InnerFolder, texture: AITexture, index: Int): InnerFile {
        // ("file name: ${texture.mFilename().dataString()}")
        // val hintBuffer = texture.achFormatHint()
        // ("format hints: ${hintBuffer.toByteArray().joinToString()}, ${texture.achFormatHintString()}")
        // ("${texture.mWidth()} x ${texture.mHeight()}")

        val width = texture.mWidth()
        val height = texture.mHeight()
        val isCompressed = height == 0

        val size = if (isCompressed) width else width * height * 4
        val data = bufferToBytes(texture, size)

        val fileName = texture.mFilename().dataString().ifEmpty {
            if (isCompressed) {
                // png file? check using signature
                val signature = Signature.find(data)
                val extension = signature?.name ?: "png"
                "$index.$extension"
            } else {
                "$index.bmp"
            }
        }
        // todo make unique

        return if (isCompressed) {
            // LOGGER.info("Loading compressed texture: $index, $width bytes")
            // width is the buffer size in bytes
            // the last bytes will be filled automatically with zeros :)
            parentFolder.createByteChild(fileName, data)
        } else {
            // LOGGER.info("Loading raw texture: $index, $width x $height")
            // if not compressed, get data as raw, and save it to bmp or sth like that
            // best possible format: raw
            // ARGB8888
            // check whether image actually has alpha channel
            parentFolder.createImageChild(fileName, ByteImage(width, height, 4, data, hasAlpha(data)))
        }
    }

    fun ByteBuffer.toByteArray(): ByteArray {
        return ByteArray(limit()) { get(it) }
    }

    fun bufferToBytes(texture: AITexture, size: Int): ByteArray {
        val buffer = texture.pcData(size / 4)
        return bufferToBytes(buffer, size)
    }

    fun bufferToBytes(buffer: AITexel.Buffer, size: Int): ByteArray {
        val bytes = ByteArray(size)
        var j = 0
        for (i in 0 until size / 4) {
            bytes[j++] = buffer.b()
            bytes[j++] = buffer.g()
            bytes[j++] = buffer.r()
            bytes[j++] = buffer.a()
            buffer.get()
        }
        return bytes
    }

    fun getColor(aiMaterial: AIMaterial, color: AIColor4D, flag: String): Vector4f? {
        val result = aiGetMaterialColor(aiMaterial, flag, aiTextureType_NONE, 0, color)
        return if (result == 0) {
            Vector4f(color.r(), color.g(), color.b(), color.a())
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
            prefab.setProperty("name", name)
        }

        prefab.setProperty("positions", positions)
        prefab.setProperty("indices", indices)

        val normals = processNormals(aiMesh, vertexCount)
        if (normals != null) {
            prefab.setProperty("normals", normals)
        }

        val tangents = processTangents(aiMesh, vertexCount)
        if (tangents != null) {
            prefab.setProperty("tangents", tangents)
        }

        val uvs = processUVs(aiMesh, vertexCount)
        if (uvs != null && uvs.any { it != 0f }) {
            prefab.setProperty("uvs", uvs)
        }

        for (i in 0 until 8) {
            val colorI = processVertexColors(aiMesh, i, vertexCount)
            if (colorI != null && colorI.any { it != -1 }) {
                prefab.setProperty(if (i == 0) "color0" else "color$i", colorI)
            }
        }

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx in materials.indices) {
            val ref = materials[materialIdx]
            prefab.setProperty("materials", listOf(ref))
        }

        return prefab

    }

    fun processTangents(aiMesh: AIMesh, vertexCount: Int): FloatArray? {
        val src = aiMesh.mTangents()
        return if (src != null) {
            val dst = FloatArray(vertexCount * 3)
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                dst[j++] = value.x()
                dst[j++] = value.y()
                dst[j++] = value.z()
            }
            dst
        } else null
    }

    fun processNormals(aiMesh: AIMesh, vertexCount: Int): FloatArray? {
        val src = aiMesh.mNormals()
        return if (src != null) {
            val dst = FloatArray(vertexCount * 3)
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                dst[j++] = value.x()
                dst[j++] = value.y()
                dst[j++] = value.z()
            }
            dst
        } else null
    }

    fun processUVs(aiMesh: AIMesh, vertexCount: Int): FloatArray? {
        val src = aiMesh.mTextureCoords(0)
        return if (src != null) {
            val dst = FloatArray(vertexCount * 2)
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                dst[j++] = value.x()
                dst[j++] = 1 - value.y()
            }
            dst
        } else null
    }

    fun processPositions(aiMesh: AIMesh, dst: FloatArray) {
        var j = 0
        val src = aiMesh.mVertices()
        while (src.hasRemaining()) {
            val value = src.get()
            dst[j++] = value.x()
            dst[j++] = value.y()
            dst[j++] = value.z()
        }
    }

    fun processPositions(aiMesh: AIAnimMesh, dst: FloatArray) {
        var j = 0
        val src = aiMesh.mVertices()!!
        while (src.hasRemaining()) {
            val value = src.get()
            dst[j++] = value.x()
            dst[j++] = value.y()
            dst[j++] = value.z()
        }
    }

    fun processVertexColors(aiMesh: AIMesh, index: Int, vertexCount: Int): IntArray? {
        val src = aiMesh.mColors(index)
        return if (src != null) {
            val dst = IntArray(vertexCount)
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                val rgba = rgba(value.r(), value.g(), value.b(), value.a())
                dst[j++] = rgba
            }
            // when every one is black or white, it doesn't actually have data
            if (dst.all { it == -1 } || dst.all { it == 0 }) return null
            dst
        } else null
    }

}