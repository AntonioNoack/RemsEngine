package me.anno.mesh.assimp

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.AssimpTree.convert
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import java.nio.IntBuffer


open class StaticMeshesLoader {

    companion object {
        val defaultFlags = aiProcess_GenSmoothNormals or aiProcess_JoinIdenticalVertices or aiProcess_Triangulate or
                aiProcess_FixInfacingNormals// or aiProcess_PreTransformVertices // <- disables animations
        private val LOGGER = LogManager.getLogger(StaticMeshesLoader::class)
    }

    open fun load(resourcePath: String, texturesDir: String?, flags: Int = defaultFlags): AnimGameItem {
        val aiScene: AIScene = aiImportFile(resourcePath, flags or aiProcess_PreTransformVertices)
            ?: throw Exception("Error loading model")
        val materials = loadMaterials(aiScene, texturesDir)
        val meshes = loadMeshes(aiScene, materials)
        return AnimGameItem(meshes, emptyList(), emptyMap())
    }

    // todo convert assimp mesh such that it's a normal mesh; because all meshes should be the same to create :)
    fun buildScene(aiScene: AIScene, sceneMeshes: Array<AssimpMesh>, aiNode: AINode): Entity {

        val entity = Entity()
        entity.name = aiNode.mName().dataString()

        val transform = entity.transform
        transform.setLocal(convert(aiNode.mTransformation()))

        val meshCount = aiNode.mNumMeshes()
        if (meshCount > 0) {

            val renderer = MeshRenderer()
            entity.addComponent(renderer)

            val model = AssimpModel()
            // model.name = aiNode.mName().dataString()
            // model.transform.set(convert(aiNode.mTransformation()))
            val meshIndices = aiNode.mMeshes()!!
            for (i in 0 until meshCount) {
                val mesh = sceneMeshes[meshIndices[i]]
                model.meshes.add(mesh)
            }

            entity.addComponent(model)

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

    fun buildScene(aiScene: AIScene, sceneMeshes: Array<AssimpMesh>): Entity {
        return buildScene(aiScene, sceneMeshes, aiScene.mRootNode()!!)
    }

    fun loadMeshes(aiScene: AIScene, materials: Array<Material>): Entity {
        val numMeshes = aiScene.mNumMeshes()
        val aiMeshes = aiScene.mMeshes()
        val meshes = Array(numMeshes) { i ->
            val aiMesh = AIMesh.create(aiMeshes!![i])
            processMesh(aiMesh, materials)
        }
        return buildScene(aiScene, meshes)
    }

    fun loadMaterials(aiScene: AIScene, texturesDir: String?): Array<Material> {
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        return Array(numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![it])
            processMaterial(aiMaterial, texturesDir)
        }
    }

    fun processIndices(aiMesh: AIMesh, indices: MutableList<Int>) {
        val numFaces = aiMesh.mNumFaces()
        val aiFaces = aiMesh.mFaces()
        for (i in 0 until numFaces) {
            val aiFace = aiFaces[i]
            val buffer = aiFace.mIndices()
            while (buffer.remaining() > 0) {
                indices.add(buffer.get())
            }
        }
    }

    fun processMaterial(
        aiMaterial: AIMaterial,
        texturesDir: String?
    ): Material {

        val color = AIColor4D.create()
        val texture = getPath(aiMaterial, aiTextureType_DIFFUSE, texturesDir)
        val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
        val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        val material = Material(ambient, diffuse, specular, 1.0f)
        material.diffuseMap = if (texture == null) null else getReference(texture)
        return material

    }

    fun getPath(aiMaterial: AIMaterial, type: Int, texturesDir: String?): String? {
        val path = AIString.calloc()
        aiGetMaterialTexture(
            aiMaterial, type, 0, path, null as IntBuffer?,
            null, null, null, null, null
        )
        val textPath = path.dataString() ?: null
        var texture: String? = null
        if (textPath != null && textPath.isNotEmpty()) {
            var textureFile = ""
            if (texturesDir != null && texturesDir.isNotEmpty()) {
                textureFile += "$texturesDir/"
            }
            textureFile += textPath
            textureFile = textureFile.replace("//", "/")
            texture = textureFile
        }
        return texture
    }

    fun getColor(aiMaterial: AIMaterial, color: AIColor4D, flag: String): Vector4f {
        val result = aiGetMaterialColor(aiMaterial, flag, aiTextureType_NONE, 0, color)
        return if (result == 0) {
            Vector4f(color.r(), color.g(), color.b(), color.a())
        } else Vector4f(1f)
    }

    fun processMesh(aiMesh: AIMesh, materials: Array<Material>): AssimpMesh {

        val vertexCount = aiMesh.mNumVertices()
        val vertices = FloatArray(vertexCount * 3)
        val textures = FloatArray(vertexCount * 2)
        val normals = FloatArray(vertexCount * 3)
        val indices = ArrayList<Int>()
        val colors = FloatArray(vertexCount * 4)

        processVertices(aiMesh, vertices)
        processNormals(aiMesh, normals)
        processUVs(aiMesh, textures)
        processIndices(aiMesh, indices)
        processVertexColors(aiMesh, colors)

        val mesh = AssimpMesh(
            vertices, textures,
            normals, colors,
            indices.toIntArray(),
            null, null
        )

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx >= 0 && materialIdx < materials.size) {
            mesh.material = materials[materialIdx]
        }

        return mesh

    }

    fun processNormals(aiMesh: AIMesh, buffer: FloatArray) {
        var j = 0
        val aiNormals = aiMesh.mNormals()
        if (aiNormals != null) {
            while (aiNormals.remaining() > 0) {
                val aiNormal = aiNormals.get()
                buffer[j++] = aiNormal.x()
                buffer[j++] = aiNormal.y()
                buffer[j++] = aiNormal.z()
            }
        }
    }

    fun processUVs(aiMesh: AIMesh, buffer: FloatArray) {
        val textCoords = aiMesh.mTextureCoords(0)
        if (textCoords != null) {
            var j = 0
            while (textCoords.remaining() > 0) {
                val textCoord = textCoords.get()
                buffer[j++] = textCoord.x()
                buffer[j++] = 1 - textCoord.y()
            }
        }
    }

    fun processVertices(aiMesh: AIMesh, buffer: FloatArray) {
        var j = 0
        val aiVertices = aiMesh.mVertices()
        while (aiVertices.hasRemaining()) {
            val aiVertex = aiVertices.get()
            buffer[j++] = aiVertex.x()
            buffer[j++] = aiVertex.y()
            buffer[j++] = aiVertex.z()
        }
    }

    // todo we may have tangent information as well <3
    fun processVertexColors(aiMesh: AIMesh, buffer: FloatArray) {
        val colors = aiMesh.mColors(0)
        if (colors != null) {
            var j = 0
            while (colors.remaining() > 0) {
                val aiVertex = colors.get()
                buffer[j++] = aiVertex.r()
                buffer[j++] = aiVertex.g()
                buffer[j++] = aiVertex.b()
                buffer[j++] = aiVertex.a()
            }
        } else {
            buffer.fill(1f)
        }
    }

}