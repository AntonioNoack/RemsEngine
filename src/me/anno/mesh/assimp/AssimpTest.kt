package me.anno.mesh.assimp

import de.javagl.jgltf.model.GltfConstants.GL_ARRAY_BUFFER
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.io.FileReference
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.types.Vectors.print
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.opengl.GL15.*
import java.nio.IntBuffer

// todo custom, faster Kotlin compiler...

// todo sims game with baked, high quality lighting

fun main() {

    // todo if we are lucky, we can use assimp to load all models and play all skeletal animations
    // todo this would really be great <3

    val file = FileReference(OS.documents, "redMonkey.glb")
    val scene = aiImportFile(file.toString(), 0)

    if (scene != null) {

        println(scene)

        val materials = scene.mMaterials()!!
        for (i in 0 until scene.mNumMaterials()) {
            val material = AIMaterial.create(materials[i])
            processMaterial(material)
        }

        val meshes = scene.mMeshes()!!
        for (i in 0 until scene.mNumMeshes()) {
            val mesh = AIMesh.create(meshes[i])
            processMesh(mesh)
        }

        println("${scene.mNumMaterials()} materials + ${scene.mNumMeshes()} meshes")

    } else println("failed to load scene")

}

fun processMesh(mesh: AIMesh) {

    processBuffer(mesh.mVertices())

}

fun processBuffer(buffer: AIVector3D.Buffer): StaticBuffer {

    // todo like Unity, only load stuff in software, if we need it?
    // todo first load into GPU for rendering

    val buffer2 = StaticBuffer(
        listOf(
            Attribute("", 3)
        ), buffer.remaining()
    )
    buffer2.buffer = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, buffer2.buffer)
    nglBufferData(// very efficient upload
        GL_ARRAY_BUFFER, AIVector3D.SIZEOF * buffer.remaining().toLong(),
        buffer.address(), GL_STATIC_DRAW
    )

    return buffer2

}

fun processVertices(mesh: AIMesh, data: MutableList<Float>) {
    val vertices = mesh.mVertices()
    while (vertices.remaining() > 0) {
        val vertex = vertices.get()
        data.add(vertex.x())
        data.add(vertex.y())
        data.add(vertex.z())
    }
}

fun processMaterial(material: AIMaterial) {

    val color = AIColor4D.create()
    val path = AIString.calloc()

    aiGetMaterialTexture(
        material,
        aiTextureType_DIFFUSE,
        0,
        path,
        null as IntBuffer?,
        null,
        null,
        null,
        null,
        null
    )
    val path2 = path.dataString() ?: null
    if (path2 != null && path2.isNotEmpty()) {
        // we have a path :)
        println("texture path: $path2")
    }

    val ambient = Vector4f()
    val result = aiGetMaterialColor(material, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, color)
    if (result == 0) {
        ambient.set(result.r() / 255f, result.g() / 255f, result.b() / 255f, result.a() / 255f)
        println("ambient: ${ambient.print()}")
    }

    val diffuse = Vector4f()
    if (aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color) != 0) {
        diffuse.set(result.r() / 255f, result.g() / 255f, result.b() / 255f, result.a() / 255f)
        println("diffuse: ${diffuse.print()}")
    }

    // ...

}


