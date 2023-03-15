package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.image.raw.ByteImage
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.xml.XMLNode
import me.anno.io.xml.XMLReader
import me.anno.io.xml.XMLWriter
import me.anno.io.zip.InnerFile
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.InnerTmpFile
import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.mesh.assimp.io.AIFileIOImpl
import me.anno.mesh.gltf.GLTFMaterialExtractor
import me.anno.utils.Color.rgba
import me.anno.utils.LOGGER
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Triangles.crossDot
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

open class StaticMeshesLoader {

    companion object {

        const val defaultFlags = aiProcess_GenSmoothNormals or // if the normals are unavailable, generate smooth ones
                aiProcess_Triangulate or // we don't want to triangulate ourselves
                aiProcess_JoinIdenticalVertices or // is required to load indexed geometry
                // aiProcess_FixInfacingNormals or // is recommended, may be incorrect... is incorrect for the Sponza sample from Intel
                aiProcess_GlobalScale

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

        // or aiProcess_PreTransformVertices // <- disables animations

    }

    fun loadFile(file: FileReference, flags: Int): AIScene {
        // obj files should use our custom importer
        // if (file.lcExtension == "obj") throw IllegalArgumentException()
        val sign = Signature.findNameSync(file)
        if (sign == "dae" && aiGetVersionMajor() < 5) {
            // Assimp 4.1 is extremely picky when parsing Collada XML for no valid reason
            // Assimp 5.2 fixes that (but also breaks my animation code)
            val xml = XMLReader().parse(file.inputStreamSync())!!
            fun clean(xml: Any): Any {
                return if (xml is XMLNode) {
                    for (i in xml.children.indices) {
                        xml.children[i] = clean(xml.children[i])
                    }
                    xml
                } else xml.toString().trim()
            }
            val better = XMLWriter.write(clean(xml) as XMLNode, null, false)
            val tmpFile = InnerTmpFile.InnerTmpTextFile(better)
            return loadFile(tmpFile, flags)
        }

        // we could load in parallel,
        // but we'd need to keep track of the scale factor;
        // it only is allowed to be set, if the file is a fbx file
        return synchronized(StaticMeshesLoader) {
            val obj = if (file is FileFileRef /*&&/|| file.absolutePath.count { it == '.' } <= 1*/) {
                val store = aiCreatePropertyStore()!!
                aiSetImportPropertyFloat(store, AI_CONFIG_GLOBAL_SCALE_FACTOR_KEY, 1f)
                aiImportFileExWithProperties(file.absolutePath, flags, null, store)
            } else {
                val store = aiCreatePropertyStore()!!
                aiSetImportPropertyFloat(store, AI_CONFIG_GLOBAL_SCALE_FACTOR_KEY, 1f)
                val fileIO = AIFileIOImpl.create(file, file.getParent()!!)
                aiImportFileExWithProperties(file.name, flags, fileIO, store)
                    ?: aiImportFileFromMemoryWithProperties( // the first method threw "bad allocation" somehow 🤷‍♂️
                        file.readByteBufferSync(true), flags, null as ByteBuffer?, store
                    )
            }
            // should be sync as well
            obj ?: throw IOException("Error loading model $file, ${aiGetErrorString()}")
        }
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
            prefab.setUnsafe(Path.ROOT_PATH, "name", name)
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
            prefab.setUnsafe(path, "position", localPosition)

        val localRotation = transform.localRotation
        if (localRotation.w != 1.0)
            prefab.setUnsafe(path, "rotation", localRotation)

        val localScale = transform.localScale
        if (localScale.x != 1.0 || localScale.y != 1.0 || localScale.z != 1.0)
            prefab.setUnsafe(path, "scale", localScale)

        val meshCount = aiNode.mNumMeshes()
        if (meshCount > 0) {

            val rendererClass = if (hasSkeleton) "AnimRenderer" else "MeshComponent"
            val meshIndices = aiNode.mMeshes()!!
            for (i in 0 until meshCount) {
                val mesh = sceneMeshes[meshIndices[i]]
                val meshComponent = prefab.add(path, 'c', rendererClass, mesh.name)
                prefab.setUnsafe(meshComponent, "mesh", mesh)
            }

        }

        val childCount = aiNode.mNumChildren()
        if (childCount > 0) {
            val children = aiNode.mChildren()!!
            for (i in 0 until childCount) {
                val childNode = AINode.createSafe(children[i]) ?: continue
                val childName = childNode.mName().dataString()
                val childPath = prefab.add(path, 'e', "Entity", childName)
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
        original: FileReference,
        missingFilesLookup: Map<String, FileReference>,
    ): Array<Prefab> {
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        val gltfMaterials = try {
            GLTFMaterialExtractor.extract(original)
        } catch (e: IOException) {
            null
        }
        return Array(numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![it])
            processMaterialPrefab(aiScene, aiMaterial, loadedTextures, texturesDir, gltfMaterials, missingFilesLookup)
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
            @Suppress("SpellCheckingInspection")
            when (buffer.remaining()) {
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
                    // a triangle, as it should be by the triangulation flag
                    indices[i + 0] = buffer.get()
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
        extraDataMap: Map<String, GLTFMaterialExtractor.PBRMaterialData>?,
        missingFilesLookup: Map<String, FileReference>,
    ): Prefab {

        val prefab = Prefab("Material")
        val color = AIColor4D.create()
        // val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        // val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        // get the name...
        val nameStr = AIString.calloc()
        aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, nameStr)
        val name = nameStr.dataString()
        prefab.setProperty("name", name)

        val diffuseMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DIFFUSE, texturesDir, missingFilesLookup)
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
        if (emissive != null) {
            emissive.mul(20f) // for brighter colors; 5.0 is our default because of Reinhard tonemapping
            // 4x, because we want it to be impressive ^^, and to actually feel like glowing;
            // the original 1 should be 100%, so I think it's kind of appropriate
            prefab.setProperty("emissiveBase", Vector3f(emissive.x, emissive.y, emissive.z))
        }

        val emissiveMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_EMISSIVE, texturesDir, missingFilesLookup)
        if (emissiveMap != InvalidRef) prefab.setProperty("emissiveMap", emissiveMap)

        // normal
        val normalMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_NORMALS, texturesDir, missingFilesLookup)
        if (normalMap != InvalidRef) prefab.setProperty("normalMap", normalMap)

        // metallic / roughness
        val metallicRoughness = getPath(
            aiScene, aiMaterial, loadedTextures,
            AI_MATKEY_GLTF_PBRMETALLICROUGHNESS_METALLICROUGHNESS_TEXTURE, texturesDir,
            missingFilesLookup
        )

        if (metallicRoughness != InvalidRef) {
            prefab.setProperty("metallicMap", getReference(metallicRoughness, "b.png"))
            prefab.setProperty("roughnessMap", getReference(metallicRoughness, "g.png"))
            prefab.setProperty("roughnessMinMax", Vector2f(0.1f, 1f))
            prefab.setProperty("metallicMinMax", Vector2f(0f, 1f))
        } else {

            // assimp only supports a single roughness/metallic property :/
            // done: read materials manually for fbx -> looking at OpenFBX, FBX only has the specular/shininess/reflective-workflow

            // roughness
            // AI_MATKEY_SHININESS as color, .r: 360, 500, so the exponent?
            val shininessExponent = getFloat(aiMaterial, AI_MATKEY_SHININESS)
            // val shininessStrength = getFloat(aiMaterial, AI_MATKEY_SHININESS_STRENGTH) // always 0.0
            // LOGGER.info("roughness: $shininess x $shininessStrength")
            val roughnessBase = shininessToRoughness(shininessExponent)
            prefab.setProperty("roughnessMinMax", Vector2f(0f, roughnessBase))

            val metallic = getFloat(aiMaterial, AI_MATKEY_REFLECTIVITY) // 0.0, rarely 0.5
            if (metallic != 0f) prefab.setProperty("metallicMinMax", Vector2f(0f, metallic))

        }

        val extraData = extraDataMap?.get(name)
        if (extraData != null) {
            prefab.setProperty("metallicMinMax", Vector2f(0f, extraData.metallic))
            prefab.setProperty("roughnessMinMax", Vector2f(0f, extraData.roughness))
        }

        // other stuff
        val displacementMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DISPLACEMENT, texturesDir, missingFilesLookup)
        val occlusionMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_LIGHTMAP, texturesDir, missingFilesLookup)
        if (displacementMap != InvalidRef) prefab.setProperty("displacementMap", displacementMap)
        if (occlusionMap != InvalidRef) prefab.setProperty("occlusionMap", occlusionMap)

        return prefab
    }

    private val pMax = IntArray(1)

    init {
        pMax[0] = 1
    }

    fun getFloat(aiMaterial: AIMaterial, key: String): Float {
        val a = FloatArray(1)
        aiGetMaterialFloatArray(aiMaterial, key, aiTextureType_NONE, 0, a, pMax)
        return a[0]
    }

    private fun getPath(
        aiScene: AIScene,
        aiMaterial: AIMaterial,
        loadedTextures: List<FileReference>,
        type: Int,
        parentFolder: FileReference,
        missingFilesLookup: Map<String, FileReference>,
    ): FileReference {
        val path = AIString.calloc()
        aiGetMaterialTexture(
            aiMaterial, type, 0, path, null as IntBuffer?,
            null, null, null, null, null
        )
        var path0 = path.dataString() ?: return InvalidRef
        if (path0.isBlank2()) return InvalidRef
        if (path0.startsWith('*')) {
            val index = path0.substring(1).toIntOrNull() ?: return InvalidRef
            if (index !in 0 until aiScene.mNumTextures()) return InvalidRef
            return loadedTextures.getOrNull(index) ?: InvalidRef
        }
        if (path0.startsWith("./")) path0 = path0.substring(2)
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
            ?: missingFilesLookup[maybePath.name] ?: maybePath
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

        return if (isCompressed) {
            // width is the buffer size in bytes
            // the last bytes will be filled automatically with zeros :)
            parentFolder.createByteChild(fileName, data)
        } else {
            // if not compressed, get data as raw, and save it to bmp or sth like that
            // best possible format: raw
            // ARGB8888
            // check whether image actually has alpha channel
            parentFolder.createImageChild(fileName, ByteImage(width, height, ByteImage.Format.ARGB, data))
        }
    }

    fun ByteBuffer.toByteArray(): ByteArray {
        return ByteArray(limit()) { get(it) }
    }

    fun ByteBuffer.toByteArray(dst: ByteArray): ByteArray {
        get(dst, 0, dst.size)
        return dst
    }

    private fun bufferToBytes(buffer: AITexel.Buffer, size: Int): ByteArray {
        val bytes = ByteArray(size)
        var j = 0
        if (buffer.remaining() != size / 4) {
            LOGGER.warn("Size doesn't match, ${buffer.position()}, ${buffer.capacity()}, ${buffer.remaining()} vs ${size / 4}")
        }
        for (i in 0 until buffer.remaining()) {
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
            val tangents = processTangents(aiMesh, vertexCount, normals)
            if (tangents != null) {
                prefab.setProperty("tangents", tangents)
            }
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
            var j = 0
            val vec = AIVector3D.malloc()
            val dst = FloatArray(vertexCount * 2)
            while (src.remaining() > 0) {
                src.get(vec)
                dst[j++] = vec.x()
                dst[j++] = vec.y()
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
        var j = 0
        val vec = AIVector3D.malloc()
        while (src.hasRemaining() && j < dst.size) {
            src.get(vec)
            dst[j++] = vec.x()
            dst[j++] = vec.y()
            dst[j++] = vec.z()
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
        var j = 0
        val vec = AIVector3D.malloc()
        while (tangents.hasRemaining() && bitangents.hasRemaining() && j < dst.size) {
            tangents.get(vec)
            val tx = vec.x()
            val ty = vec.y()
            val tz = vec.z()
            dst[j++] = tx
            dst[j++] = ty
            dst[j++] = tz
            val nx = normals[i++]
            val ny = normals[i++]
            val nz = normals[i++]
            bitangents.get(vec)
            dst[j++] = sign(crossDot(nx, ny, nz, tx, ty, tz, vec.x(), vec.y(), vec.z()))
        }
        vec.free()
        return dst
    }

    // custom function, because there may be NaNs
    // (NewSponza_Main_Blender_glTF.gltf from Intel contains NaNs)
    private fun f2i(v: Float): Int {
        return if (v <= 0f) 0
        else if (v < 1f) (v * 255).roundToInt()
        else 1
    }

    private fun processVertexColors(aiMesh: AIMesh, index: Int, vertexCount: Int): IntArray? {
        val src = aiMesh.mColors(index)
        return if (src != null) {
            var j = 0
            val vec = AIColor4D.malloc()
            val dst = IntArray(vertexCount)
            while (src.remaining() > 0 && j < vertexCount) {
                src.get(vec)
                val r = f2i(vec.r())
                val g = f2i(vec.g())
                val b = f2i(vec.b())
                val a = f2i(vec.a())
                dst[j++] = rgba(r, g, b, a)
            }
            vec.free()
            // when every pixel is black or white, it doesn't actually have data
            if (dst.all { it == -1 } || dst.all { it == 0 }) return null
            dst
        } else null
    }

}