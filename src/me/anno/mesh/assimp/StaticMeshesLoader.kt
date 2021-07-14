package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.mesh.assimp.AssimpTree.convert
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
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
        val hierarchy = buildScene(aiScene, meshes)
        return AnimGameItem(hierarchy, meshes.toList(), emptyList(), emptyMap())
    }

    // todo convert assimp mesh such that it's a normal mesh; because all meshes should be the same to create :)
    fun buildScene(aiScene: AIScene, sceneMeshes: Array<Mesh>, aiNode: AINode): Entity {

        val entity = Entity()
        entity.name = aiNode.mName().dataString()

        val transform = entity.transform
        transform.setLocal(convert(aiNode.mTransformation()))

        val meshCount = aiNode.mNumMeshes()
        if (meshCount > 0) {

            val renderer = MeshRenderer()
            entity.addComponent(renderer)

            // model.name = aiNode.mName().dataString()
            // model.transform.set(convert(aiNode.mTransformation()))
            val meshIndices = aiNode.mMeshes()!!
            for (i in 0 until meshCount) {
                val mesh = sceneMeshes[meshIndices[i]]
                entity.components.add(MeshComponent(mesh))
            }

        }

        val childCount = aiNode.mNumChildren()
        if (childCount > 0) {
            val children = aiNode.mChildren()!!
            for (i in 0 until childCount) {
                val childNode = AINode.create(children[i])
                entity.addChild(buildScene(aiScene, sceneMeshes, childNode))
            }
        }

        return entity

    }

    fun buildScene(aiScene: AIScene, sceneMeshes: Array<Mesh>): Entity {
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
        return Array(numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![it])
            processMaterial(aiMaterial, texturesDir)
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
        aiMaterial: AIMaterial,
        texturesDir: FileReference
    ): Material {

        val color = AIColor4D.create()
        // val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        // val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        val material = Material()

        // diffuse
        val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
        if (diffuse != null) material.diffuseBase.set(diffuse)
        material.diffuseMap = getPath(aiMaterial, aiTextureType_DIFFUSE, texturesDir)

        // emissive
        val emissive = getColor(aiMaterial, color, AI_MATKEY_COLOR_EMISSIVE)
        if (emissive != null) material.emissiveBase = emissive
        material.emissiveMap = getPath(aiMaterial, aiTextureType_EMISSIVE, texturesDir)

        // normal
        material.normalTex = getPath(aiMaterial, aiTextureType_NORMALS, texturesDir)

        // other stuff
        material.displacementMap = getPath(aiMaterial, aiTextureType_DISPLACEMENT, texturesDir)
        material.occlusionMap = getPath(aiMaterial, aiTextureType_LIGHTMAP, texturesDir)

        // todo metallic & roughness


        return material

    }

    fun getPath(aiMaterial: AIMaterial, type: Int, parentFolder: FileReference): FileReference {
        if (parentFolder == InvalidRef) return InvalidRef
        val path = AIString.calloc()
        aiGetMaterialTexture(
            aiMaterial, type, 0, path, null as IntBuffer?,
            null, null, null, null, null
        )
        val path0 = path.dataString() ?: return InvalidRef
        if (path0.isBlank2()) return InvalidRef
        val path1 = path0.replace("//", "/")
        return parentFolder.getChild(path1)
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
        val uvs = FloatArray(vertexCount * 2)
        val normals = FloatArray(vertexCount * 3)
        val indices = IntArray(aiMesh.mNumFaces() * 3)
        val colors = FloatArray(vertexCount * 4)

        processPositions(aiMesh, vertices)
        processNormals(aiMesh, normals)
        processUVs(aiMesh, uvs)
        processIndices(aiMesh, indices)
        processVertexColors(aiMesh, colors)

        val mesh = Mesh()
        mesh.positions = vertices
        mesh.normals = normals
        mesh.uvs = uvs
        mesh.color0 = colors
        mesh.indices = indices

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

    fun processNormals(aiMesh: AIMesh, dst: FloatArray) {
        val src = aiMesh.mNormals()
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

    fun processUVs(aiMesh: AIMesh, dst: FloatArray) {
        val src = aiMesh.mTextureCoords(0)
        if (src != null) {
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                dst[j++] = value.x()
                dst[j++] = 1 - value.y()
            }
        }
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

    fun processVertexColors(aiMesh: AIMesh, dst: FloatArray) {
        val src = aiMesh.mColors(0)
        if (src != null) {
            var j = 0
            while (src.remaining() > 0) {
                val value = src.get()
                dst[j++] = value.r()
                dst[j++] = value.g()
                dst[j++] = value.b()
                dst[j++] = value.a()
            }
        } else {
            dst.fill(1f)
        }
    }

}