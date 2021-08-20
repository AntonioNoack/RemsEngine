package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.Prefab
import me.anno.image.bmp.BMPWriter.createBMP
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.StaticRef
import me.anno.io.zip.InnerFolder
import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.utils.Color.rgba
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.system.MemoryUtil
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer


open class StaticMeshesLoader {

    companion object {

        val defaultFlags = aiProcess_GenSmoothNormals or aiProcess_JoinIdenticalVertices or aiProcess_Triangulate or
                aiProcess_FixInfacingNormals or aiProcess_GlobalScale

        // or aiProcess_PreTransformVertices // <- disables animations
        private val LOGGER = LogManager.getLogger(StaticMeshesLoader::class)

    }

    fun fileToBuffer(name: String, termination: Boolean): ByteBuffer {
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
    }

    class AIFileIOStream(val file: FileReference) {

        companion object {
            private const val SEEK_SET = 1
            private const val SEEK_CUR = 2
            private const val SEEK_END = 3
        }

        var input: InputStream? = null
        var position: Long = 0L

        val length = file.length()

        fun close() {
            input?.close()
            input = null
        }

        // whence = from where / where to add the offset
        fun seek(offset: Long, whence: Int): Int {
            ensureInput()
            val target = when (whence) {
                SEEK_SET -> 0L
                SEEK_CUR -> position
                SEEK_END -> file.length()
                else -> throw RuntimeException("Unknown mode $whence")
            } + offset
            val delta = target - position
            if (delta < 0L) throw RuntimeException("Skipping back hasn't yet been implemented")
            val input = input!!
            var done = 0L
            while (done < delta) {
                val skipped = input.skip(delta)
                if (skipped < 0L) return -1 // eof
                done += skipped
            }
            position = target
            // 0 = success
            return 0
        }

        fun ensureInput() {
            if (input == null) input = file.inputStream()
        }

        fun read(buffer: ByteBuffer, length: Long): Long {
            ensureInput()
            val input = input!!
            val position0 = position
            var position = position
            for (i in 0 until length) {
                val b = input.read()
                if (b < 0) break
                buffer.put(b.toByte())
                position++
            }
            this.position = position
            return position - position0
        }

    }

    fun loadFile(file: FileReference, flags: Int): AIScene {
        return if (file is FileFileRef || file.absolutePath.count { it == '.' } <= 1) {
            aiImportFile(file.absolutePath, flags)
        } else {
            val fileIO = AIFileIO.calloc()
            val map = HashMap<Long, AIFileIOStream>()
            fileIO.set({ _, fileNamePtr, openModePtr ->
                var fileName = MemoryUtil.memUTF8(fileNamePtr)
                val openMode = MemoryUtil.memUTF8(openModePtr)
                if (openMode != "rb") throw RuntimeException("Expected rb as mode")
                // println("name/mode: $fileName / $openMode")
                // if (fileName != file.name) throw RuntimeException()
                if (fileName.startsWith("/")) fileName = fileName.substring(1)
                if ('\\' in fileName) fileName = fileName.replace('\\', '/')
                val file1 = if (fileName == file.name) file
                else file.getParent()!!.getChild(fileName)
                // println("$fileName -> $file1")
                val callbacks = AIFile.create()
                map[callbacks.address()] = AIFileIOStream(file1)
                callbacks.set(
                    { aiFile, dstBufferPtr, size1, size2 ->
                        val totalSize = size1 * size2
                        val dstBuffer = MemoryUtil.memByteBuffer(dstBufferPtr, totalSize.toInt())
                        // println("reading $size1*$size2 bytes of ${map[aiFile]!!.length}")
                        map[aiFile]!!.read(dstBuffer, totalSize)
                    },
                    { _, charArray, size1, size2 ->
                        // write proc
                        // println("writing")
                        throw RuntimeException("Writing is not supported, $charArray, $size1*$size2")
                    },
                    { aiFile -> map[aiFile]!!.position },
                    { aiFile -> map[aiFile]!!.length },
                    { aiFile, offset, whence ->
                        // println("seek $offset $whence")
                        map[aiFile]!!.seek(offset, whence)
                    },
                    {
                        // flush
                        // println("flush")
                        throw RuntimeException("Flush is not supported")
                    },
                    0L
                )
                callbacks.address()
            }, { _, aiFile ->
                // close the stream
                map[aiFile]?.close()
            }, 0L)
            aiImportFileEx(fileToBuffer(file.name, true), flags, fileIO)
            // aiImportFileFromMemory(fileToBuffer(file), flags, file.extension)
        } ?: throw Exception("Error loading model $file, ${aiGetErrorString()}")
    }

    fun load(file: FileReference) = read(file, file.getParent() ?: InvalidRef, defaultFlags)

    open fun read(file: FileReference, resources: FileReference, flags: Int = defaultFlags): AnimGameItem {
        val aiScene = loadFile(file, flags or aiProcess_PreTransformVertices)
        val materials = loadMaterials(aiScene, file, resources)
        val meshes = loadMeshes(aiScene, materials)
        val hierarchy = buildScene(aiScene, meshes)
        return AnimGameItem(hierarchy, meshes.toList(), emptyList(), emptyMap())
    }

    private fun buildScene(aiScene: AIScene, sceneMeshes: Array<Mesh>, aiNode: AINode): Entity {
        val entity = Entity()
        buildScene(aiScene, sceneMeshes, aiNode, entity)
        return entity
    }

    private fun buildScene(aiScene: AIScene, sceneMeshes: List<FileReference>, aiNode: AINode): Prefab {
        val prefab = Prefab("Entity")
        buildScene(aiScene, sceneMeshes, aiNode, prefab, Path())
        return prefab
    }

    private fun buildScene(
        aiScene: AIScene,
        sceneMeshes: Array<Mesh>,
        aiNode: AINode,
        entity: Entity
    ) {

        entity.name = aiNode.mName().dataString()

        val transform = entity.transform
        transform.setLocal(convert(aiNode.mTransformation()))

        val meshCount = aiNode.mNumMeshes()
        if (meshCount > 0) {

            // model.name = aiNode.mName().dataString()
            // model.transform.set(convert(aiNode.mTransformation()))
            val meshIndices = aiNode.mMeshes()!!
            for (i in 0 until meshCount) {
                val meshIndex = meshIndices[i]
                val mesh = sceneMeshes[meshIndex]
                entity.add(mesh.clone())
            }

            // todo use a normal renderer, if there is no skeleton
            val renderer = AnimRenderer()
            entity.addComponent(renderer)

        }

        val childCount = aiNode.mNumChildren()
        if (childCount > 0) {
            val children = aiNode.mChildren()!!
            for (i in 0 until childCount) {

                val childNode = AINode.create(children[i])
                val childEntity = Entity()
                entity.addChild(childEntity)

                buildScene(aiScene, sceneMeshes, childNode, entity)

            }
        }
    }

    private fun buildScene(
        aiScene: AIScene,
        sceneMeshes: List<FileReference>,
        aiNode: AINode,
        prefab: Prefab,
        path: Path
    ) {

        val name = aiNode.mName().dataString()
        if (!name.isBlank2())
            prefab.add(CSet(path, "name", name))

        val transform = Transform()
        transform.setLocal(convert(aiNode.mTransformation()))
        if (transform.localPosition.length() != 0.0)
            prefab.add(CSet(path, "position", transform.localPosition))
        if (transform.localRotation.w != 1.0)
            prefab.add(CSet(path, "rotation", transform.localRotation))
        if (Vector3d(1.0).sub(transform.localScale).length() != 0.0)
            prefab.add(CSet(path, "scale", transform.localScale))

        val meshCount = aiNode.mNumMeshes()
        if (meshCount > 0) {

            // model.name = aiNode.mName().dataString()
            // model.transform.set(convert(aiNode.mTransformation()))
            val meshIndices = aiNode.mMeshes()!!
            for (i in 0 until meshCount) {
                val mesh = sceneMeshes[meshIndices[i]]
                prefab.add(CAdd(path, 'c', "Mesh", mesh.name, mesh))
            }

            prefab.add(CAdd(path, 'c', "AnimRenderer", "AnimRenderer"))

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
                buildScene(aiScene, sceneMeshes, childNode, prefab, childPath)
            }
        }

    }

    fun buildScene(aiScene: AIScene, sceneMeshes: Array<Mesh>): Entity {
        return buildScene(aiScene, sceneMeshes, aiScene.mRootNode()!!)
    }

    fun buildScene(aiScene: AIScene, sceneMeshes: List<FileReference>): Prefab {
        return buildScene(aiScene, sceneMeshes, aiScene.mRootNode()!!)
    }

    fun loadMeshes(aiScene: AIScene, materials: Array<Material>): Array<Mesh> {
        val numMeshes = aiScene.mNumMeshes()
        val aiMeshes = aiScene.mMeshes()
        return Array(numMeshes) { i ->
            val aiMesh = AIMesh.create(aiMeshes!![i])
            createMesh(aiMesh, materials)
        }
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
                val (name, data) = loadTexture(AITexture.create(textures[it]), it)
                list += parentFolder.createByteChild(name, data)
            }
            list
        } else emptyList()
    }

    fun loadMaterials(aiScene: AIScene, resource: FileReference, texturesDir: FileReference): Array<Material> {
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        val loadedTextures = HashMap<Int, FileReference>()
        return Array(numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![it])
            processMaterial(aiScene, aiMaterial, loadedTextures, resource, texturesDir)
        }
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

        // diffuse
        val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
        if (diffuse != null) prefab.setProperty("diffuseBase", diffuse)
        val diffuseMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DIFFUSE, texturesDir)
        if (diffuseMap != InvalidRef) prefab.setProperty("diffuseMap", diffuseMap)

        // emissive
        val emissive = getColor(aiMaterial, color, AI_MATKEY_COLOR_EMISSIVE)
        if (emissive != null) prefab.setProperty("emissiveBase", Vector3f(emissive.x, emissive.y, emissive.z))
        val emissiveMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_EMISSIVE, texturesDir)
        if (emissiveMap != InvalidRef) prefab.setProperty("emissiveMap", emissiveMap)

        // normal
        val normalMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_NORMALS, texturesDir)
        if (normalMap != InvalidRef) prefab.setProperty("normalMap", normalMap)

        // roughness
        // AI_MATKEY_SHININESS as color, .r: 360, 500, so the exponent?
        val shininessExponent = getFloat(aiMaterial, AI_MATKEY_SHININESS)
        // val shininessStrength = getFloat(aiMaterial, AI_MATKEY_SHININESS_STRENGTH) // always 0.0
        // LOGGER.info("roughness: $shininess x $shininessStrength")
        val roughnessBase = shininessToRoughness(shininessExponent)
        prefab.setProperty("roughnessBase", roughnessBase)

        // val metallic0 = getColor(aiMaterial, color, AI_MATKEY_COLOR_REFLECTIVE) // always null
        val metallic = getFloat(aiMaterial, AI_MATKEY_REFLECTIVITY) // 0.0, rarely 0.5
        prefab.setProperty("metallicBase", metallic)
        // LOGGER.info("metallic: $metallic0 x $metallic")

        // other stuff
        val displacementMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DISPLACEMENT, texturesDir)
        val occlusionMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_LIGHTMAP, texturesDir)
        if (displacementMap != InvalidRef) prefab.setProperty("displacementMap", displacementMap)
        if (occlusionMap != InvalidRef) prefab.setProperty("occlusionMap", occlusionMap)

        // todo metallic & roughness

        return prefab
    }


    fun processMaterial(
        aiScene: AIScene,
        aiMaterial: AIMaterial,
        loadedTextures: HashMap<Int, FileReference>,
        resource: FileReference,
        texturesDir: FileReference
    ): Material {

        val color = AIColor4D.create()
        // val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        // val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        val material = Material()

        // get the name...
        val name = AIString.calloc()
        aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, name)
        material.name = name.dataString()

        // diffuse
        val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
        if (diffuse != null) material.diffuseBase.set(diffuse)
        material.diffuseMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DIFFUSE, resource, texturesDir)

        // emissive
        val emissive = getColor(aiMaterial, color, AI_MATKEY_COLOR_EMISSIVE)
        if (emissive != null) material.emissiveBase = Vector3f(emissive.x, emissive.y, emissive.z)
        material.emissiveMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_EMISSIVE, resource, texturesDir)

        // normal
        material.normalMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_NORMALS, resource, texturesDir)

        // roughness
        // AI_MATKEY_SHININESS as color, .r: 360, 500, so the exponent?
        val shininessExponent = getFloat(aiMaterial, AI_MATKEY_SHININESS)
        // val shininessStrength = getFloat(aiMaterial, AI_MATKEY_SHININESS_STRENGTH) // always 0.0
        // LOGGER.info("roughness: $shininess x $shininessStrength")
        material.roughnessMinMax.set(0f, shininessToRoughness(shininessExponent))

        // val metallic0 = getColor(aiMaterial, color, AI_MATKEY_COLOR_REFLECTIVE) // always null
        val metallic = getFloat(aiMaterial, AI_MATKEY_REFLECTIVITY) // 0.0, sometimes 0.5
        material.metallicMinMax.set(0f, metallic)
        // LOGGER.info("metallic: $metallic0 x $metallic")

        // other stuff
        material.displacementMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DISPLACEMENT, resource, texturesDir)
        material.occlusionMap =
            getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_LIGHTMAP, resource, texturesDir)

        // todo metallic & roughness

        return material

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
        loadedTextures: HashMap<Int, FileReference>,
        type: Int, resource: FileReference,
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
            return loadedTextures.getOrPut(index) {
                val texture = AITexture.create(aiScene.mTextures()!![index])
                val (fileName, data) = loadTexture(texture, index)
                val hash = resource.getParent()!!.absolutePath.hashCode().toUInt().toString(16)
                val res = "$hash/${resource.name}/$fileName.png"
                // todo a) use no hash?, use the local path
                // todo b) register these in some file, or allow this file to be accessed like a zip file (probably best)
                FileReference.register(StaticRef(res, lazy { data }))
            }
        }
        val path1 = path0.replace("//", "/")
        return parentFolder.getChild(path1)
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
        val path1 = path0.replace("//", "/")
        return parentFolder.getChild(path1)
    }

    fun loadTexture(texture: AITexture, index: Int): Pair<String, ByteArray> {
        // ("file name: ${texture.mFilename().dataString()}")
        // val hintBuffer = texture.achFormatHint()
        // ("format hints: ${hintBuffer.toByteArray().joinToString()}, ${texture.achFormatHintString()}")
        // ("${texture.mWidth()} x ${texture.mHeight()}")
        val width = texture.mWidth()
        val height = texture.mHeight()
        val isCompressed = height == 0
        val data = if (isCompressed) {
            LOGGER.info("Loading compressed texture: $index, $width bytes")
            // width is the buffer size in bytes
            // the last bytes will be filled automatically with zeros :)
            bufferToBytes(texture, width)
        } else {
            LOGGER.info("Loading raw texture: $index, $width x $height")
            // if not compressed, get data as raw, and save it to bmp or sth like that
            //  - it would be nice, if we could read the image directly as raw into the gpu
            //  - bmp shouldn't be that bad... also we could try to join these functions into one to be more efficient
            // ARGB8888
            createBMP(width, height, bufferToBytes(texture, width * height * 4))
        }
        // works for png :)
        // raw data still needs to be tested...
        // OS.desktop.getChild("normals.png").writeBytes(data)
        // theoretically we would need to create a temporary file or something like that...
        // or a temporary static reference :)
        val fileName = texture.mFilename().dataString().ifEmpty { index.toString() }
        return fileName to data
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

    fun createMesh(aiMesh: AIMesh, materials: Array<Material>): Mesh {

        val vertexCount = aiMesh.mNumVertices()

        val positions = FloatArray(vertexCount * 3)
        val indices = IntArray(aiMesh.mNumFaces() * 3)
        processPositions(aiMesh, positions)
        processIndices(aiMesh, indices)

        val mesh = Mesh()
        mesh.name = aiMesh.mName().dataString()

        mesh.positions = positions
        mesh.indices = indices

        mesh.normals = processNormals(aiMesh, vertexCount)
        mesh.uvs = processUVs(aiMesh, vertexCount)
        mesh.color0 = processVertexColors(aiMesh, vertexCount)

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx >= 0 && materialIdx < materials.size) {
            mesh.material = materials[materialIdx]
        }

        return mesh

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

        val uvs = processUVs(aiMesh, vertexCount)
        if (uvs != null && uvs.any { it != 0f }) {
            prefab.setProperty("uvs", uvs)
        }

        val color0 = processVertexColors(aiMesh, vertexCount)
        if (color0 != null && color0.any { it != -1 }) {
            prefab.setProperty("color0", color0)
        }

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx in materials.indices) {
            val ref = materials[materialIdx]
            prefab.add(CAdd(Path(), 'm', "Material", ref.nameWithoutExtension, ref))
            // prefab.setProperty("materials", listOf(ref))
        }

        return prefab

    }

    // todo we may have tangent information as well <3

    fun processTangents(aiMesh: AIMesh, dst: FloatArray) {
        val src = aiMesh.mTangents()
        if (src != null) {
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                dst[j++] = value.x()
                dst[j++] = value.y()
                dst[j++] = value.z()
            }
        }
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

    fun processVertexColors(aiMesh: AIMesh, vertexCount: Int): IntArray? {
        val src = aiMesh.mColors(0)
        return if (src != null) {
            val dst = IntArray(vertexCount)
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                val rgba = rgba(value.r(), value.g(), value.b(), value.a())
                dst[j++] = rgba
            }
            dst
        } else null
    }

}