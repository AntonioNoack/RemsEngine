package me.anno.tests.mesh.gltf.reader

import me.anno.Engine
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.anim.SkeletonCache
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFolder
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.gltf.GLTFReader
import me.anno.utils.Clock
import me.anno.utils.OS
import me.anno.utils.assertions.assertNotSame
import org.apache.logging.log4j.LogManager

fun main() {

    LogManager.disableInfoLogs("GFX,WindowManagement,OpenXRSystem,OpenXRUtils,Saveable,ExtensionManager")
    OfficialExtensions.initForTests()

    val clock = Clock("GLTFReader")
    lateinit var folder: InnerFolder
    val src = OS.downloads.getChild("3d/azeria/scene.gltf")

    src.readBytes { bytes, err ->
        err?.printStackTrace()
        GLTFReader(src).readAnyGLTF(bytes!!) { folder1, err2 ->
            err2?.printStackTrace()
            folder = folder1!!
            clock.stop("GLTFReader")
        }
    }

    folder.printTree()

    val byReader =
        (folder.getChild("skeletons/Skeleton.json") as PrefabReadable).readPrefab().newInstance() as Skeleton
    val byReaderBones = JsonStringWriter.toText(byReader.bones, InvalidRef)

    clock.start()
    AnimatedMeshesLoader.readAsFolder(src) { folder1, err2 ->
        err2?.printStackTrace()
        folder = folder1!!
        clock.stop("Assimp")
    }

    folder.printTree()

    val byAssimp = SkeletonCache[folder.getChild("skeletons/Skeleton.json"), false]!!
    val byAssimpBones = JsonStringWriter.toText(byAssimp.bones, InvalidRef)

    assertNotSame(byReader, byAssimp)
    printBones(byReader.bones)
    printBones(byAssimp.bones)
    // assertEquals(byReaderBones, byAssimpBones)

    testSceneWithUI("Skeleton", byReader)

    Engine.requestShutdown()
}

fun printBones(bones: List<Bone>) {
    println(bones.size)
    for (bone in bones) printBone(bone)
}

fun printBone(bone: Bone) {
    println("'${bone.name}': ${bone.parentIndex}, ${bone.bindPose}")
}