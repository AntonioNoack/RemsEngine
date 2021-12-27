package me.anno.utils.test.gfx

import me.anno.Engine
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.cache.SkeletonCache
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.engine.ui.render.ECSShaderLib
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.shader.Renderer
import me.anno.image.ImageCPUCache
import me.anno.image.ImageGPUCache
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.thumbs.Thumbs
import me.anno.io.files.thumbs.Thumbs.generateAssimpMeshFrame
import me.anno.io.files.thumbs.Thumbs.generateEntityFrame
import me.anno.io.files.thumbs.Thumbs.generateMaterialFrame
import me.anno.io.files.thumbs.Thumbs.generateMeshFrame
import me.anno.io.files.thumbs.Thumbs.generateSVGFrame
import me.anno.io.files.thumbs.Thumbs.generateSkeletonFrame
import me.anno.io.files.thumbs.Thumbs.generateVOXMeshFrame
import me.anno.io.files.thumbs.Thumbs.generateVideoFrame
import me.anno.mesh.assimp.Bone
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import java.io.FileNotFoundException
import javax.imageio.ImageIO

fun main() {

    val clock = Clock()

    val size = 128

    DefaultConfig.init()

    /*for (i in 1 until 128) {
        val s = Thumbs.split(i)
        println("$i: ${GFXx2D.getSizeX(s)} x ${GFXx2D.getSizeY(s)}")
    }*/

    // like Rem's CLI instantiate OpenGL
    HiddenOpenGLContext.createOpenGL(size, size)
    ShaderLib.init()
    ECSShaderLib.init()

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

    Thumbs.useCacheFolder = true

    fun FileReference.dst() = desktop.getChild("$nameWithoutExtension.png")
    fun FileReference.dst2() = desktop.getChild("$nameWithoutExtension-2.png")
    fun FileReference.dst3() = desktop.getChild("$nameWithoutExtension-3.png")

    fun testAssimpMeshFrame(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        generateAssimpMeshFrame(file, file.dst(), size) {}
    }

    fun testEntityMeshFrame(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        val entity = PrefabCache.getPrefabPair(file, null)!!.instance as Entity
        generateEntityFrame(file.dst(), size, entity) {}
    }

    fun testSkeletonFrame(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        val skeleton = SkeletonCache[file]!!
        generateSkeletonFrame(file.dst(), skeleton, size) {}
    }

    fun testImage(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        val image = ImageCPUCache.getImage(file, false)!!
        val w = image.width / 4
        val h = image.height / 4
        // test cpu loading
        if (file != file.dst()) file.dst().outputStream().use {
            ImageIO.write(image.createBufferedImage(w, h), "png", it)
        }
        file.dst3().outputStream().use {
            val smaller = image.createBufferedImage(w, h)
            ImageIO.write(smaller, "png", it)
        }
        // also write image to the gpu, and then get it back to test the uploading
        Thumbs.renderToBufferedImage(file, file.dst2(), false, Renderer.colorRenderer, true, {}, w, h) {
            val texture = ImageGPUCache.getImage(file, 10_000, false)!!
            drawTexture(0, 0, w, h, texture, -1, null)
        }
        //val tex2 = Thumbs.getThumbnail(file,size,false)
        //println("texture from thumbs: ${tex2.toString()}")
    }

    fun testFFMPEGImage(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        generateVideoFrame(file, file.dst(), size, {}, 0.0)
    }

    fun testSVG(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        generateSVGFrame(file, file.dst(), size) {}
    }

    testEntityMeshFrame(getReference(documents, "CuteGhost.obj"))

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

    // done metallic was missing
    testAssimpMeshFrame(getReference(downloads, "3d/DamagedHelmet.glb"))


    // that skeleton looks incorrect... why? because it itself is incorrect, others work
    // testSkeletonFrame(getReference(desktop, "Skeleton.json"))

    // this skeleton meanwhile looks correct
    // testSkeletonFrame(getReference(desktop, "WarriorSkeleton.json"))

    // 3ds max files are not supported by assimp, sadly :/
    // testAssimpMeshFrame(getReference(downloads, "3d/Diningtable.max"))

    // todo test the following meshes
    /*testAssimpMeshFrame(getReference(documents, "sphere.obj"))
    testAssimpMeshFrame(getReference(documents, "cube bricks.fbx"))
    testAssimpMeshFrame(getReference(downloads, "3d/robot_kyle_walking.fbx"))
    testAssimpMeshFrame(getReference(downloads, "3d/2CylinderEngine.glb"))
    testAssimpMeshFrame(getReference(downloads, "fbx/free meshes/simple small lowpoly bridge_better.fbx"))*/
    // testEntityMeshFrame(getReference(desktop, "Scene.json"))

    fun testVOXMeshFrame(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        clock.stop("loading ${file.name}")
        generateVOXMeshFrame(file, file.dst(), size) {}
        clock.stop("rendering ${file.name}")
    }

    // testVOXMeshFrame(getReference(downloads, "MagicaVoxel/vox/room.vox"))
    // testVOXMeshFrame(getReference(downloads, "MagicaVoxel/vox/birch2Small31.vox"))

    fun testMeshFrame(file: FileReference) {
        if (!file.exists) throw FileNotFoundException("$file does not exist")
        val mesh = MeshCache[file]!!
        clock.stop("loading ${file.name}")
        generateMeshFrame(file.dst(), size, mesh) {}
        clock.stop("rendering ${file.name}")
    }

    // testMeshFrame(desktop.getChild("Object_0.json")) // from the fox
    // testMeshFrame(desktop.getChild("Object_1.json")) // from the fox, without materials, normals, and animation
    // testMeshFrame(desktop.getChild("Lights.json")) // problematic invisible mesh

    fun testMaterial(file: FileReference) {
        clock.stop("loading ${file.name}")
        generateMaterialFrame(file, desktop.getChild(file.nameWithoutExtension + ".png"), size) {}
        clock.stop("rendering ${file.name}")
    }

    testMaterial(getReference(desktop, "fox_material.json"))

    /*val meshOfSrc = MeshCache[src]!!
    generateMeshFrame(src, desktop.getChild("sphere2.png"), size, meshOfSrc) {}

    val meshRef = waitUntilDefined(true) { loadAssimpStatic(src, null) }
        .assimpModel!!.hierarchy.getComponentInChildren(MeshComponent::class, true)!!
    val meshOfRef = MeshCache[meshRef.mesh]!!
    generateMeshFrame(src, desktop.getChild("sphere3.png"), size, meshOfRef) {}*/

    clock.total("")

    Engine.shutdown()

}