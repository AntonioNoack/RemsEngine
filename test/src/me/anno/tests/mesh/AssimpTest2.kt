package me.anno.tests.mesh

import me.anno.Engine
import me.anno.ecs.components.anim.Skeleton
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.OpenGLBuffer.Companion.bindBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.thumbs.Thumbs
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.assimp.AnimatedMeshesLoader.createNodeCache
import me.anno.mesh.assimp.Bone
import me.anno.mesh.assimp.StaticMeshesLoader
import me.anno.mesh.assimp.findAllBones
import me.anno.utils.Color.a01
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import org.lwjgl.opengl.GL15.*
import java.nio.IntBuffer

// todo sims game with baked, high quality lighting

fun main() {

    HiddenOpenGLContext.createOpenGL()
    ShaderLib.init()
    Thumbs.useCacheFolder = true

    val size = 512

    // done test animation / skeleton
    @Suppress("SpellCheckingInspection")
    val file = getReference(downloads, "3d/taryk/scene.gltf")
    val aiScene = AnimatedMeshesLoader.loadFile(file, StaticMeshesLoader.defaultFlags)
    val rootNode = aiScene.mRootNode()!!

    val boneList = ArrayList<Bone>()
    val boneMap = HashMap<String, Bone>()

    val skeleton = Skeleton()
    skeleton.bones = boneList

    // check what is the result using the animations
    // loadSkeletonFromAnimations(aiScene, rootNode, createNodeCache(rootNode), boneList, boneMap)
    val dst0 = getReference(desktop, "byAnimation.png")
    Thumbs.generateSkeletonFrame(dst0, dst0, skeleton, size) { _, exc -> exc?.printStackTrace() }
    println("by animation: ${boneList.map { it.name }}")




    boneList.clear()
    boneMap.clear()

    findAllBones(aiScene, rootNode, boneList, boneMap)
    val dst1 = getReference(desktop, "byTree.png")
    Thumbs.generateSkeletonFrame(dst1, dst1, skeleton, size) { _, exc -> exc?.printStackTrace() }
    println("by tree, full: ${boneList.map { it.name }}")

    Engine.requestShutdown()

}

@Suppress("unused", "SpellCheckingInspection")
fun walkingTest() {
    // val loader = AnimatedMeshesLoader
    val (_, prefab) = AnimatedMeshesLoader.readAsFolder2(getReference(downloads, "fbx/simple pack anims/Walking.fbx"))
    for (change in prefab.adds) {
        println(change)
    }
}

@Suppress("unused")
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
    buffer2.pointer = glGenBuffers()
    bindBuffer(GL_ARRAY_BUFFER, buffer2.pointer)
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
        ambient.set(0f, 0f, 0f, 1f)
        logger.info("ambient: $ambient")
    }

    val diffuse = Vector4f()
    if (aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, color) != 0) {
        diffuse.set(result.r01(), result.g01(), result.b01(), result.a01())
        logger.info("diffuse: $diffuse")
    }

    // ...

}


