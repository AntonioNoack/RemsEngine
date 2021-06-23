package me.anno.mesh.assimp

import me.anno.io.FileReference
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import java.nio.IntBuffer


open class StaticMeshesLoader {

    fun load(resourcePath: String, texturesDir: String?): Array<AssimpMesh> {
        return load(
            resourcePath, texturesDir,
            aiProcess_GenSmoothNormals or aiProcess_JoinIdenticalVertices or aiProcess_Triangulate
                    or aiProcess_FixInfacingNormals or aiProcess_PreTransformVertices
        )
    }

    fun load(resourcePath: String, texturesDir: String?, flags: Int): Array<AssimpMesh> {
        val aiScene: AIScene = aiImportFile(resourcePath, flags) ?: throw Exception("Error loading model")
        val numMaterials = aiScene.mNumMaterials()
        val aiMaterials = aiScene.mMaterials()
        val materials: MutableList<Material> = ArrayList()
        for (i in 0 until numMaterials) {
            val aiMaterial = AIMaterial.create(aiMaterials!![i])
            processMaterial(aiMaterial, materials, texturesDir)
        }
        val numMeshes = aiScene.mNumMeshes()
        val aiMeshes = aiScene.mMeshes()
        return Array(numMeshes) { i ->
            val aiMesh = AIMesh.create(aiMeshes!![i])
            processMesh(aiMesh, materials)
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
        aiMaterial: AIMaterial, materials: MutableList<Material>,
        texturesDir: String?
    ) {

        val color = AIColor4D.create()
        val path = AIString.calloc()
        aiGetMaterialTexture(
            aiMaterial, aiTextureType_DIFFUSE, 0, path, null as IntBuffer?,
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
        val ambient = getColor(aiMaterial, color, AI_MATKEY_COLOR_AMBIENT)
        val diffuse = getColor(aiMaterial, color, AI_MATKEY_COLOR_DIFFUSE)
        val specular = getColor(aiMaterial, color, AI_MATKEY_COLOR_SPECULAR)

        val material = Material(ambient, diffuse, specular, 1.0f)
        material.texture = if (texture == null) null else FileReference(texture)
        materials.add(material)

    }

    fun getColor(aiMaterial: AIMaterial, color: AIColor4D, flag: String): Vector4f {
        val result = aiGetMaterialColor(aiMaterial, flag, aiTextureType_NONE, 0, color)
        return if (result == 0) {
            Vector4f(color.r(), color.g(), color.b(), color.a())
        } else Vector4f(1f)
    }

    fun processMesh(aiMesh: AIMesh, materials: List<Material>): AssimpMesh {

        val vertices = ArrayList<Float>()
        val textures = ArrayList<Float>()
        val normals = ArrayList<Float>()
        val indices = ArrayList<Int>()

        processVertices(aiMesh, vertices)
        processNormals(aiMesh, normals)
        processUVs(aiMesh, textures)
        processIndices(aiMesh, indices)

        // Texture coordinates may not have been populated. We need at least the empty slots
        if (textures.size == 0) {
            val numElements = (vertices.size / 3) * 2
            for (i in 0 until numElements) {
                textures.add(0f)
            }
        }

        val mesh = AssimpMesh(
            vertices.toFloatArray(), textures.toFloatArray(),
            normals.toFloatArray(), indices.toIntArray()
        )

        val materialIdx = aiMesh.mMaterialIndex()
        if (materialIdx >= 0 && materialIdx < materials.size) {
            mesh.material = materials[materialIdx]
        }

        return mesh

    }

    fun processNormals(aiMesh: AIMesh, normals: MutableList<Float>) {
        val aiNormals = aiMesh.mNormals()
        while (aiNormals != null && aiNormals.remaining() > 0) {
            val aiNormal = aiNormals.get()
            normals.add(aiNormal.x())
            normals.add(aiNormal.y())
            normals.add(aiNormal.z())
        }
    }

    fun processUVs(aiMesh: AIMesh, textures: MutableList<Float>) {
        val textCoords = aiMesh.mTextureCoords(0)
        val numTextCoords = textCoords?.remaining() ?: 0
        for (i in 0 until numTextCoords) {
            val textCoord = textCoords!!.get()
            textures.add(textCoord.x())
            textures.add(1 - textCoord.y())
        }
    }

    fun processVertices(aiMesh: AIMesh, vertices: MutableList<Float>) {
        val aiVertices = aiMesh.mVertices()
        while (aiVertices.remaining() > 0) {
            val aiVertex = aiVertices.get()
            vertices.add(aiVertex.x())
            vertices.add(aiVertex.y())
            vertices.add(aiVertex.z())
        }
    }

}