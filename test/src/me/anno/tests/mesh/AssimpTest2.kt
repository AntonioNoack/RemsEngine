package me.anno.tests.mesh

import me.anno.Engine
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.Skeleton
import me.anno.engine.OfficialExtensions
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.OpenGLBuffer.Companion.bindBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.jvm.HiddenOpenGLContext
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.Texture2D
import me.anno.graph.hdb.HDBKey
import me.anno.image.raw.GPUImage
import me.anno.image.thumbs.AssetThumbnails
import me.anno.image.thumbs.Thumbs
import me.anno.mesh.assimp.AnimatedMeshesLoader
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
import org.lwjgl.assimp.AIColor4D
import org.lwjgl.assimp.AIMaterial
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIString
import org.lwjgl.assimp.AIVector3D
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_AMBIENT
import org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_DIFFUSE
import org.lwjgl.assimp.Assimp.aiGetMaterialColor
import org.lwjgl.assimp.Assimp.aiGetMaterialTexture
import org.lwjgl.assimp.Assimp.aiImportFile
import org.lwjgl.assimp.Assimp.aiTextureType_DIFFUSE
import org.lwjgl.assimp.Assimp.aiTextureType_NONE
import org.lwjgl.opengl.GL46C.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL46C.GL_STATIC_DRAW
import org.lwjgl.opengl.GL46C.glGenBuffers
import org.lwjgl.opengl.GL46C.nglBufferData
import java.nio.IntBuffer

fun main() {

    OfficialExtensions.initForTests()
    HiddenOpenGLContext.createOpenGL()
    ShaderLib.init()
    Thumbs.useCacheFolder = true

    val size = 512

    // done test animation / skeleton
    @Suppress("SpellCheckingInspection")
    val file = downloads.getChild("3d/taryk/scene.gltf")
    val aiScene = StaticMeshesLoader.loadFile(file, StaticMeshesLoader.defaultFlags).first
    val rootNode = aiScene.mRootNode()!!

    val boneList = ArrayList<Bone>()
    val boneMap = HashMap<String, Bone>()

    val skeleton = Skeleton()
    skeleton.bones = boneList

    // check what is the result using the animations
    // loadSkeletonFromAnimations(aiScene, rootNode, createNodeCache(rootNode), boneList, boneMap)
    val dst0 = desktop.getChild("byAnimation.png")
    AssetThumbnails.generateSkeletonFrame(dst0, HDBKey.InvalidKey, skeleton, size) { result, exc ->
        if (result is Texture2D) GPUImage(result).write(dst0)
        exc?.printStackTrace()
    }
    println("by animation: ${boneList.map { it.name }}")




    boneList.clear()
    boneMap.clear()

    findAllBones(aiScene, rootNode, boneList, boneMap)
    val dst1 = desktop.getChild("byTree.png")
    AssetThumbnails.generateSkeletonFrame(dst1, HDBKey.InvalidKey, skeleton, size) { result, exc ->
        if (result is Texture2D) GPUImage(result).write(dst1)
        exc?.printStackTrace()
    }
    println("by tree, full: ${boneList.map { it.name }}")

    Engine.requestShutdown()
}

@Suppress("unused", "SpellCheckingInspection")
fun walkingTest() {
    // val loader = AnimatedMeshesLoader
    val (_, prefab) = AnimatedMeshesLoader.readAsFolder2(downloads.getChild("fbx/simple pack anims/Walking.fbx"))
    for (change in prefab.adds) {
        println(change)
    }
}

@Suppress("unused")
fun oldTest() {

    val logger = LogManager.getLogger("AssimpTest")

    // if we are lucky, we can use assimp to load all models and play all skeletal animations
    // this would really be great <3

    val file = OS.documents.getChild("redMonkey.glb")
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

    val buffer2 = StaticBuffer("assimp", listOf(Attribute("attr", 3)), buffer.remaining())
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
    if (!path2.isNullOrEmpty()) {
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


