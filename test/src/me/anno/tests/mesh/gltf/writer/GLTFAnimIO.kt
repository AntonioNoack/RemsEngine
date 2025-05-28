package me.anno.tests.mesh.gltf.writer

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentInChildren
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.DefaultAssets
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.inner.InnerFolder
import me.anno.maths.Maths.TAUf
import me.anno.mesh.gltf.GLTFReader
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS
import org.joml.Matrix4x3f

val bonePositions = listOf(
    Matrix4x3f(),
    Matrix4x3f().translate(0f, 2.1f, 0f),
    Matrix4x3f().translate(0f, 4.2f, 0f),
    Matrix4x3f().translate(0f, 6.3f, 0f),
)

fun main() {

    ECSRegistry.init()

    // create mesh with skeletal animation
    val mesh = object : MeshJoiner<IndexedValue<Matrix4x3f>>(false, true, true) {
        override fun getBoneIndex(element: IndexedValue<Matrix4x3f>): Byte {
            return element.index.toByte()
        }

        override fun getMesh(element: IndexedValue<Matrix4x3f>): Mesh {
            return DefaultAssets.flatCube
        }

        override fun getTransform(element: IndexedValue<Matrix4x3f>, dst: Matrix4x3f) {
            dst.set(element.value)
        }
    }.join(bonePositions.withIndex().toList())
    mesh.name = "AnimIOMesh"

    val skeleton = Skeleton()
    skeleton.bones = bonePositions.mapIndexed { idx, mat ->
        val bone = Bone(idx, idx - 1, "Bone")
        bone.setBindPose(mat)
        bone
    }
    mesh.skeleton = skeleton.ref
    skeleton.name = "AnimIOSkel"

    val animation = ImportedAnimation()
    animation.skeleton = skeleton.ref
    animation.duration = 3f
    animation.frames = (0 until 9).map { fi ->
        skeleton.bones.indices.map { bi ->
            Matrix4x3f()
                .rotateX(bi * TAUf / skeleton.bones.size)
                .rotateY(fi * TAUf / 9f)
        }
    }
    animation.name = "AnimIOAnim"

    val animComp = AnimMeshComponent()
    animComp.name = "AnimIO"
    animComp.meshFile = mesh.ref
    animComp.animations = listOf(AnimationState(animation.ref))

    if (false) testSceneWithUI("Mesh", animComp)

    // write GLTF
    lateinit var bytes: ByteArray
    GLTFWriter().write(animComp) { bytes1, err ->
        err?.printStackTrace()
        bytes = bytes1!!
    }

    val src = OS.desktop.getChild("AnimIO.glb")
    src.writeBytes(bytes)

    // read GLTF
    lateinit var folder: InnerFolder
    GLTFReader(src).readAnyGLTF(bytes) { folder1, err ->
        err?.printStackTrace()
        folder = folder1!!
    }

    val instance = PrefabCache.getPrefabSampleInstance(folder.getChild("Scene.json")) as Entity
    println(instance)
    val animComp2 = instance.getComponentInChildren(AnimMeshComponent::class)!!
    println(animComp2)

    // todo where is the soft swinging coming from???
    testSceneWithUI("AnimIO/2", instance)
}