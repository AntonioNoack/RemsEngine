package me.anno.tests.gfx

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimRenderer
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.ShaderLib
import me.anno.image.ImageCPUCache
import me.anno.image.ImageGPUCache
import me.anno.image.ImageScale.scaleMax
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.files.thumbs.Thumbs.generateEntityFrame
import me.anno.io.files.thumbs.Thumbs.generateMaterialFrame
import me.anno.io.files.thumbs.Thumbs.generateMeshFrame
import me.anno.io.files.thumbs.Thumbs.generateSVGFrame
import me.anno.io.files.thumbs.Thumbs.generateSkeletonFrame
import me.anno.io.files.thumbs.Thumbs.generateSomething
import me.anno.io.files.thumbs.Thumbs.generateVOXMeshFrame
import me.anno.io.files.thumbs.Thumbs.generateVideoFrame
import me.anno.ecs.components.anim.Bone
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import java.io.FileNotFoundException
import javax.imageio.ImageIO

var size = 128

fun FileReference.dst() = desktop.getChild("$nameWithoutExtension.png")
fun FileReference.dst2() = desktop.getChild("$nameWithoutExtension-2.png")
fun FileReference.dst3() = desktop.getChild("$nameWithoutExtension-3.png")

fun init() {
    Thumbs.useCacheFolder = true
}

fun testAssimpMeshFrame(file: FileReference) {
    init()
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    generateSomething(file, file.dst(), size) { _, exc -> exc?.printStackTrace() }
}

fun testEntityMeshFrame(file: FileReference) {
    init()
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    val entity = PrefabCache.getPrefabInstance(file) as Entity
    generateEntityFrame(file, file.dst(), size, entity) { _, exc -> exc?.printStackTrace() }
}

@Suppress("unused")
fun testSkeletonFrame(file: FileReference) {
    init()
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    val skeleton = SkeletonCache[file]!!
    generateSkeletonFrame(file, file.dst(), skeleton, size) { _, exc -> exc?.printStackTrace() }
}

@Suppress("unused")
fun testImage(file: FileReference) {
    init()
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    val image = ImageCPUCache[file, false]!!
    val (w, h) = scaleMax(image.width, image.height, size)
    // test cpu loading
    if (file != file.dst()) file.dst().outputStream().use {
        ImageIO.write(image.createBufferedImage(w, h, false), "png", it)
    }
    file.dst3().outputStream().use {
        val smaller = image.createBufferedImage(w, h, false)
        ImageIO.write(smaller, "png", it)
    }
    // also write image to the gpu, and then get it back to test the uploading
    Thumbs.renderToImage(file, false, file.dst2(), false, Renderer.copyRenderer, true,
        { _, exc -> exc?.printStackTrace() }, w, h
    ) {
        val texture = ImageGPUCache[file, 10_000, false]!!
        drawTexture(0, 0, w, h, texture, -1, null)
    }
    //val tex2 = Thumbs.getThumbnail(file,size,false)
    //println("texture from thumbs: ${tex2.toString()}")
}

@Suppress("unused")
fun testFFMPEGImage(file: FileReference) {
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    generateVideoFrame(file, file.dst(), size, { _, exc -> exc?.printStackTrace() }, 0.0)
}

@Suppress("unused")
fun testSVG(file: FileReference) {
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    generateSVGFrame(file, file.dst(), size) { _, exc -> exc?.printStackTrace() }
}

@Suppress("unused")
fun testMeshFrame(file: FileReference) {
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    val mesh = MeshCache[file]!!
    generateMeshFrame(file, file.dst(), size, mesh) { _, exc -> exc?.printStackTrace() }
}

fun testMaterial(file: FileReference) {
    generateMaterialFrame(file, desktop.getChild(file.nameWithoutExtension + ".png"), size) { _, exc ->
        exc?.printStackTrace()
    }
}

@Suppress("unused")
fun testVOXMeshFrame(file: FileReference) {
    if (!file.exists) throw FileNotFoundException("$file does not exist")
    generateVOXMeshFrame(file, file.dst(), size) { _, exc -> exc?.printStackTrace() }
}

fun main() {

    val clock = Clock()

    /*for (i in 1 until 128) {
        val s = Thumbs.split(i)
        println("$i: ${GFXx2D.getSizeX(s)} x ${GFXx2D.getSizeY(s)}")
    }*/

    // like Rem's CLI instantiate OpenGL
    HiddenOpenGLContext.createOpenGL(size / 4, size / 4)
    ShaderLib.init()

    clock.stop("Init-Stuff")

    registerCustomClass(Prefab())
    registerCustomClass(CSet())
    registerCustomClass(CAdd())

    registerCustomClass(Entity())

    registerCustomClass(Mesh())
    registerCustomClass(Material())
    registerCustomClass(MeshComponent())

    registerCustomClass(Bone())
    registerCustomClass(Skeleton())
    registerCustomClass(AnimRenderer())
    registerCustomClass(ImportedAnimation())
    registerCustomClass(BoneByBoneAnimation())

    clock.stop("Registry")

    testEntityMeshFrame(getReference(documents, "CuteGhost.obj"))

    testAssimpMeshFrame(getReference(downloads, "3d/DamagedHelmet.glb"))

    // testSVG(getReference(downloads, "2d/tiger.svg"))

    // testImage(getReference(downloads, "2d/qwantani_1k.hdr"))

    // testFFMPEGImage(getReference(pictures, "Anime/70697252_p4_master1200.webp"))

    // testImage(desktop.getChild("2d/gi_flag.tga"))

    // not a mesh...
    // testAssimpMeshFrame(getReference(downloads, "ogldev-source/Content/dragon.mtl"))

    // val icoSample = getReference(pictures, "fav128.ico")
    // println(ImageCPUCache.getImage(icoSample, false))

    // testImage(getReference(pictures, "pic.jpg"))

    // testImage(getReference(downloads, "2d/cityBackground4k.jpg"))
    // testImage(getReference(downloads, "2d/cityBackground4k.jpg/r.png"))
    // testImage(getReference(desktop, "2d/t odo rendering in opengl.png"))
    // testImage(getReference(desktop, "2d/t odo rendering.jpg"))

    /*testImage(getReference(downloads, "3d/DamagedHelmet.glb/textures/0.jpg"))
    testImage(getReference(downloads, "3d/DamagedHelmet.glb/textures/1.jpg"))
    testImage(getReference(downloads, "3d/DamagedHelmet.glb/textures/2.jpg"))
    testImage(getReference(downloads, "3d/DamagedHelmet.glb/textures/3.jpg"))
    testImage(getReference(downloads, "3d/DamagedHelmet.glb/textures/0.jpg/r.png"))
    testImage(getReference(downloads, "3d/DamagedHelmet.glb/textures/0.jpg/g.png"))
    testImage(getReference(downloads, "3d/DamagedHelmet.glb/textures/0.jpg/b.png"))*/


    // that skeleton looks incorrect... why? because it itself is incorrect, others work
    // testSkeletonFrame(getReference(desktop, "Skeleton.json"))

    // this skeleton meanwhile looks correct
    // testSkeletonFrame(getReference(desktop, "WarriorSkeleton.json"))

    // 3ds max files are not supported by assimp, sadly :/
    // testAssimpMeshFrame(getReference(downloads, "3d/Diningtable.max"))

    /*testAssimpMeshFrame(getReference(documents, "sphere.obj"))
    testAssimpMeshFrame(getReference(documents, "cube bricks.fbx"))
    testAssimpMeshFrame(getReference(downloads, "3d/robot_kyle_walking.fbx"))
    testAssimpMeshFrame(getReference(downloads, "3d/2CylinderEngine.glb"))
    testAssimpMeshFrame(getReference(downloads, "fbx/free meshes/simple small lowpoly bridge_better.fbx"))*/
    // testEntityMeshFrame(getReference(desktop, "Scene.json"))

    // testVOXMeshFrame(getReference(downloads, "MagicaVoxel/vox/room.vox"))
    // testVOXMeshFrame(getReference(downloads, "MagicaVoxel/vox/birch2Small31.vox"))


    // testMeshFrame(desktop.getChild("Object_0.json")) // from the fox
    // testMeshFrame(desktop.getChild("Object_1.json")) // from the fox, without materials, normals and animation
    // testMeshFrame(desktop.getChild("Lights.json")) // problematic invisible mesh


    testMaterial(getReference(desktop, "fox_material.json"))

    /*val meshOfSrc = MeshCache[src]!!
    generateMeshFrame(src, desktop.getChild("sphere2.png"), size, meshOfSrc) {}

    val meshRef = waitUntilDefined(true) { loadAssimpStatic(src, null) }
        .assimpModel!!.hierarchy.getComponentInChildren(MeshComponent::class, true)!!
    val meshOfRef = MeshCache[meshRef.mesh]!!
    generateMeshFrame(src, desktop.getChild("sphere3.png"), size, meshOfRef) {}*/

    clock.total("")

    Engine.requestShutdown()

}