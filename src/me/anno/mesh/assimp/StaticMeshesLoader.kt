package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.Prefab
import me.anno.image.bmp.BMPWriter.createBMP
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.StaticRef
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
import java.nio.ByteBuffer
import java.nio.IntBuffer


open class StaticMeshesLoader {

    companion object {

        val defaultFlags = aiProcess_GenSmoothNormals or aiProcess_JoinIdenticalVertices or aiProcess_Triangulate or
                aiProcess_FixInfacingNormals or aiProcess_GlobalScale

        // or aiProcess_PreTransformVertices // <- disables animations
        private val LOGGER = LogManager.getLogger(StaticMeshesLoader::class)

    }

    fun fileToBuffer(resourcePath: FileReference): ByteBuffer {
        val bytes = resourcePath.inputStream().readBytes()
        val byteBuffer = MemoryUtil.memAlloc(bytes.size)
        byteBuffer.put(bytes)
        byteBuffer.flip()
        return byteBuffer
    }

    fun loadFile(file: FileReference, flags: Int): AIScene {
        return if (file is FileFileRef) {
            aiImportFile(file.absolutePath, flags)
        } else {
            aiImportFileFromMemory(fileToBuffer(file), flags, file.extension)
        } ?: throw Exception("Error loading model, ${aiGetErrorString()}")
    }

    fun load(file: FileReference) = load(file, file.getParent() ?: InvalidRef, defaultFlags)

    open fun load(file: FileReference, resources: FileReference, flags: Int = defaultFlags): AnimGameItem {
        val aiScene = loadFile(file, flags or aiProcess_PreTransformVertices)
        val materials = loadMaterials(aiScene, resources)
        val meshes = loadMeshes(aiScene, materials)
        val (hierarchy, hierarchyPrefab) = buildScene(aiScene, meshes)
        return AnimGameItem(hierarchy, hierarchyPrefab, meshes.toList(), emptyList(), emptyMap())
    }

    // todo convert assimp mesh such that it's a normal mesh; because all meshes should be the same to create :)
    private fun buildScene(aiScene: AIScene, sceneMeshes: Array<Mesh>, aiNode: AINode): Pair<Entity, Prefab> {

        val entity = Entity()
        val prefab = Prefab("Entity")

        buildScene(aiScene, sceneMeshes, aiNode, entity, prefab, Path())

        return Pair(entity, prefab)

    }

    private fun buildScene(
        aiScene: AIScene,
        sceneMeshes: Array<Mesh>,
        aiNode: AINode,
        entity: Entity,
        prefab: Prefab,
        path: Path
    ) {

        entity.name = aiNode.mName().dataString()
        if (!entity.name.isBlank2())
            prefab.add(CSet(path, "name", entity.name))

        val transform = entity.transform
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
                entity.add(MeshComponent(mesh))
                val meshComponent = CAdd(path, 'c', "MeshComponent", mesh.name)
                prefab.add(meshComponent)
                prefab.add(CSet(meshComponent.getChildPath(i), "mesh", mesh))
            }

            val renderer = AnimRenderer()
            entity.addComponent(renderer)
            prefab.add(CAdd(path, 'c', "AnimRenderer", "AnimRenderer"))

        }

        val childCount = aiNode.mNumChildren()
        if (childCount > 0) {
            val children = aiNode.mChildren()!!
            for (i in 0 until childCount) {

                val childNode = AINode.create(children[i])
                val childEntity = Entity()
                entity.addChild(childEntity)

                val name = childNode.mName().dataString()
                val add = CAdd(path, 'e', "Entity", name)
                prefab.add(add)
                val childPath = add.getChildPath(i)
                buildScene(aiScene, sceneMeshes, childNode, entity, prefab, childPath)

            }
        }

    }

    fun buildScene(aiScene: AIScene, sceneMeshes: Array<Mesh>): Pair<Entity, Prefab> {
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

    fun loadMaterials(aiScene: AIScene, texturesDir: FileReference): Array<Material> {
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        val loadedTextures = HashMap<Int, FileReference>()
        return Array(numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![it])
            processMaterial(aiScene, aiMaterial, loadedTextures, texturesDir)
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

    fun processMaterial(
        aiScene: AIScene,
        aiMaterial: AIMaterial,
        loadedTextures: HashMap<Int, FileReference>,
        texturesDir: FileReference
    ): Material {

        val color = AIColor4D.create()
        // val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        // val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        val material = Material()

        // diffuse
        val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
        if (diffuse != null) material.diffuseBase.set(diffuse)
        material.diffuseMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DIFFUSE, texturesDir)

        // emissive
        val emissive = getColor(aiMaterial, color, AI_MATKEY_COLOR_EMISSIVE)
        if (emissive != null) material.emissiveBase = Vector3f(emissive.x, emissive.y, emissive.z)
        material.emissiveMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_EMISSIVE, texturesDir)

        // normal
        material.normalMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_NORMALS, texturesDir)

        // roughness
        // AI_MATKEY_SHININESS as color, .r: 360, 500, so the exponent?
        val shininessExponent = getFloat(aiMaterial, AI_MATKEY_SHININESS)
        // val shininessStrength = getFloat(aiMaterial, AI_MATKEY_SHININESS_STRENGTH) // always 0.0
        // LOGGER.info("roughness: $shininess x $shininessStrength")
        material.roughnessBase = shininessToRoughness(shininessExponent)

        val metallic0 = getColor(aiMaterial, color, AI_MATKEY_COLOR_REFLECTIVE) // always null
        val metallic = getFloat(aiMaterial, AI_MATKEY_REFLECTIVITY) // 0.0
        LOGGER.info("metallic: $metallic0 x $metallic")

        // other stuff
        material.displacementMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_DISPLACEMENT, texturesDir)
        material.occlusionMap = getPath(aiScene, aiMaterial, loadedTextures, aiTextureType_LIGHTMAP, texturesDir)

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
        type: Int, parentFolder: FileReference
    ): FileReference {
        if (parentFolder == InvalidRef) return InvalidRef
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
                FileReference.register(StaticRef("${System.nanoTime()}-${Math.random()}.png", lazy { data }))
            }
        }
        val path1 = path0.replace("//", "/")
        return parentFolder.getChild(path1)
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

        val vertices = FloatArray(vertexCount * 3)
        val indices = IntArray(aiMesh.mNumFaces() * 3)
        processPositions(aiMesh, vertices)
        processIndices(aiMesh, indices)

        val mesh = Mesh()
        mesh.positions = vertices
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