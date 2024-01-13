package me.anno.tests.shader

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.*
import me.anno.ecs.components.light.*
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.AutoTileableShader
import me.anno.ecs.components.shaders.PlanarShader
import me.anno.ecs.components.shaders.Skybox
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.hidden.HiddenOpenGLContext
import me.anno.gpu.pipeline.PipelineStage.Companion.TRANSPARENT_PASS
import me.anno.mesh.Shapes.flatCube
import me.anno.tests.ui.UITests
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.OS.desktop
import org.junit.jupiter.api.Test

// test all important shaders, whether they compile fine
// add a scene with most important functions: static meshes, animated meshes, all light types
fun main() {
    testSceneWithUI("CompilerTest", createTestScene())
}

fun createTestScene(): Entity {
    val scene = Entity()
    scene.add(Skybox())
    scene.add(DirectionalLight())
    scene.add(DirectionalLight().apply {
        shadowMapCascades = 2
        shadowMapResolution = 256
    })
    // all types with their shadows (where supported)
    scene.add(SpotLight())
    scene.add(SpotLight().apply {
        shadowMapCascades = 2
        shadowMapResolution = 256
    })
    scene.add(PointLight())
    scene.add(PointLight().apply {
        shadowMapCascades = 2
        shadowMapResolution = 256
    })
    scene.add(CircleLight())
    scene.add(RectangleLight())
    scene.add(PlanarReflection())
    scene.add(EnvironmentMap())
    scene.add(MeshComponent(flatCube.front))
    scene.add(MeshComponent(flatCube.front).apply {
        isInstanced = true
    })
    // add an animated mesh, instanced and non-instanced
    val animatedMesh = flatCube.front.clone() as Mesh
    val numPos = animatedMesh.positions!!.size / 3
    animatedMesh.boneIndices = ByteArray(numPos * 4) { (it and 1).toByte() }
    animatedMesh.boneWeights = FloatArray(numPos * 4) { if (it.and(3) == 0) 1f else 0f }
    val skeleton = Skeleton()
    skeleton.bones = listOf(Bone(0, -1, "Root"), Bone(1, 0, "Child"))
    val animation = BoneByBoneAnimation()
    animation.frameCount = 1
    animation.boneCount = skeleton.bones.size
    animation.skeleton = skeleton.ref
    animation.rotations = FloatArray(4 * animation.boneCount * animation.frameCount) { if (it.and(3) == 3) 1f else 0f }
    animation.translations = FloatArray(4 * animation.boneCount * animation.frameCount) { it.toFloat() }
    val animState = AnimationState(animation.ref, 1f, 0f, 1f, LoopingState.PLAY_LOOP)
    val offset = Entity(scene)
    offset.setPosition(3.0, 0.0, 0.0)
    offset.add(AnimMeshComponent().apply {
        this.meshFile = animatedMesh.ref
        this.skeleton = skeleton.ref
        this.animations = listOf(animState)
    })
    offset.add(AnimMeshComponent().apply {
        this.meshFile = animatedMesh.ref
        this.skeleton = skeleton.ref
        this.animations = listOf(animState)
        this.isInstanced = true
    })
    scene.add(MeshComponent(flatCube.front).apply {
        materials = listOf(Material().apply {
            shader = AutoTileableShader
        }.ref)
    })
    scene.add(MeshComponent(flatCube.front).apply {
        materials = listOf(Material().apply {
            shader = PlanarShader
        }.ref)
    })
    scene.add(MeshComponent(flatCube.front).apply {
        materials = listOf(Material().apply {
            pipelineStage = TRANSPARENT_PASS
        }.ref)
    })
    return scene
}

/**
 * This tests all built-in render modes for their basic functionality, plus a little text rendering;
 * This can be quite slow the first time you run it, because it will have to compile all shaders (took 41s on my machine).
 * */
class CompileTest {

    // todo FSR2.2 is currently broken... why??

    @Test
    fun runTest() {
        HiddenOpenGLContext.createOpenGL()
        val printResults = false
        val dst = desktop.getChild("CompileTest")
        if (printResults) dst.tryMkdirs()
        val ui = UITests()
        val scene = createTestScene()
        val rv = object : RenderView(PlayMode.EDITING, style) {
            override fun getWorld() = scene
        }
        ui.prepareUI(rv)
        rv.setPosSize(0, 0, ui.osWindow.width, ui.osWindow.height)
        val tmp = Framebuffer("tmp", rv.width, rv.height, 1, 1, false, DepthBufferType.NONE)
        for (mode in RenderMode.values) {
            try {
                rv.renderMode = mode
                if (printResults) {
                    useFrame(tmp) {
                        rv.draw(0, 0, rv.width, rv.height)
                    }
                    val childName = "${mode.name.toAllowedFilename()}.png"
                    tmp.getTexture0().write(dst.getChild(childName), true)
                } else {
                    rv.draw(0, 0, rv.width, rv.height)
                }
            } catch (e: Exception) {
                throw Exception(mode.name, e)
            }
        }
        Engine.requestShutdown()
    }
}
