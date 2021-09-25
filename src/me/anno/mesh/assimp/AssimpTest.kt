package me.anno.mesh.assimp

import me.anno.ecs.components.anim.Skeleton
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.ShaderLib
import me.anno.gpu.TextureLib
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.Buffer.Companion.bindBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.assimp.AnimHierarchy.loadSkeletonFromAnimations
import me.anno.mesh.assimp.AnimatedMeshesLoader.createNodeCache
import me.anno.io.files.thumbs.Thumbs
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.types.Vectors.print
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.opengl.GL15.*
import java.nio.IntBuffer

// todo sims game with baked, high quality lighting

fun main() {

    HiddenOpenGLContext.createOpenGL()
    TextureLib.init()
    ShaderLib.init()
    ECSShaderLib.init()
    Thumbs.useCacheFolder = true

    val size = 512

    // todo test animation / skeleton
    val file = getReference(downloads, "3d/taryk/scene.gltf")
    val aiScene = AnimatedMeshesLoader.loadFile(file, StaticMeshesLoader.defaultFlags)
    val rootNode = aiScene.mRootNode()!!

    val boneList = ArrayList<Bone>()
    val boneMap = HashMap<String, Bone>()

    val skeleton = Skeleton()
    skeleton.bones = boneList

    // check what is the result using the animations
    loadSkeletonFromAnimations(aiScene, rootNode, createNodeCache(rootNode), boneList, boneMap)
    Thumbs.generateSkeletonFrame(getReference(desktop, "byAnimation.png"), skeleton, size) {}
    println("by animation: ${boneList.map { it.name }}")




    boneList.clear()
    boneMap.clear()

    SkeletonAnimAndBones.loadSkeletonFromAnimationsAndBones(aiScene, rootNode, boneList, boneMap)
    Thumbs.generateSkeletonFrame(getReference(desktop, "byTree.png"), skeleton, size) {}
    println("by tree, full: ${boneList.map { it.name }}")


}

fun walkingTest() {
    val loader = AnimatedMeshesLoader
    val (_, prefab) = loader.readAsFolder2(getReference(downloads, "fbx/simple pack anims/Walking.fbx"))
    for (change in prefab.adds) {
        println(change)
    }
}

fun oldTest() {

    val logger = LogManager.getLogger("AssimpTest")

    // if we are lucky, we can use assimp to load all models and play all skeletal animations
    // this would really be great <3

    val file = getReference(OS.documents, "redMonkey.glb")
    val scene = aiImportFile(file.toString(), 0)

    if (scene != null) {

        logger.info(scene)

        val materials = scene.mMaterials()!!
        for (i in 0 until scene.mNumMaterials()) {
            val material = AIMaterial.create(materials[i])
            processMaterial(material)
        }

        val meshes = scene.mMeshes()!!
        for (i in 0 until scene.mNumMeshes()) {
            val mesh = AIMesh.create(meshes[i])
            createMeshComponent(mesh)
        }

        logger.info("${scene.mNumMaterials()} materials + ${scene.mNumMeshes()} meshes")

    } else logger.info("failed to load scene")

}

fun createMeshComponent(mesh: AIMesh) {
    processBuffer(mesh.mVertices())
}

fun processBuffer(buffer: AIVector3D.Buffer): StaticBuffer {

    // like Unity, only load stuff in software, if we need it?
    // first load into GPU for rendering

    val buffer2 = StaticBuffer(
        listOf(
            Attribute("", 3)
        ), buffer.remaining()
    )
    buffer2.buffer = glGenBuffers()
    bindBuffer(GL_ARRAY_BUFFER, buffer2.buffer)
    nglBufferData(// very efficient upload
        GL_ARRAY_BUFFER, AIVector3D.SIZEOF * buffer.remaining().toLong(),
        buffer.address(), GL_STATIC_DRAW
    )

    return buffer2

}

fun processMaterial(material: AIMaterial) {

    val logger = LogManager.getLogger("AssimpTest/Material")

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
        logger.info("texture path: $path2")
    }

    val ambient = Vector4f()
    val result = aiGetMaterialColor(material, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE, 0, color)
    if (result == 0) {
        ambient.set(result.r() / 255f, result.g() / 255f, result.b() / 255f, result.a() / 255f)
        logger.info("ambient: ${ambient.print()}")
    }

    val diffuse = Vector4f()
    if (aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color) != 0) {
        diffuse.set(result.r() / 255f, result.g() / 255f, result.b() / 255f, result.a() / 255f)
        logger.info("diffuse: ${diffuse.print()}")
    }

    // ...

}


