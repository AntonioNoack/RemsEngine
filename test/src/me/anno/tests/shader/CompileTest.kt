package me.anno.tests.shader

import me.anno.Engine
import me.anno.animation.LoopingState
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimMeshComponent
import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.BoneByBoneAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.light.CircleLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.EnvironmentMap
import me.anno.ecs.components.light.PlanarReflection
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.light.RectangleLight
import me.anno.ecs.components.light.SpotLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.AutoTileableMaterial
import me.anno.ecs.components.mesh.material.FurMeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.PlanarMaterial
import me.anno.ecs.components.mesh.material.TriplanarMaterial
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.ecs.components.text.TextMeshComponent
import me.anno.ecs.components.text.TextTextureComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.extensions.ExtensionLoader
import me.anno.fonts.Font
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStageImpl.Companion.TRANSPARENT_PASS
import me.anno.jvm.HiddenOpenGLContext
import me.anno.mesh.Shapes
import me.anno.tests.ui.UITests
import me.anno.tests.utils.TestWorld
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.OS.desktop
import org.junit.jupiter.api.Test

/**
 * This tests all built-in render modes for their basic functionality, plus a little text rendering;
 * This can be quite slow the first time you run it, because it will have to compile all shaders (took 41s on my machine).
 * */
class CompileTest {

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
        scene.add(MeshComponent(flatCube))
        scene.add(MeshComponent(flatCube).apply {
            isInstanced = true
        })
        val font = Font("Verdana", 16f)
        scene.add(TextMeshComponent("textMesh", font, AxisAlignment.CENTER))
        scene.add(TextTextureComponent("textureText", font, AxisAlignment.CENTER))
        scene.add(SDFTextComponent("sdfText", font, AxisAlignment.CENTER))
        // add an animated mesh, instanced and non-instanced
        val animatedMesh = flatCube.clone() as Mesh
        val numPos = animatedMesh.positions!!.size / 3
        animatedMesh.boneIndices = ByteArray(numPos * 4) { (it and 1).toByte() }
        animatedMesh.boneWeights = FloatArray(numPos * 4) { if (it.and(3) == 0) 1f else 0f }
        val skeleton = Skeleton()
        skeleton.bones = listOf(Bone(0, -1, "Root"), Bone(1, 0, "Child"))
        val animation = BoneByBoneAnimation()
        animation.frameCount = 1
        animation.boneCount = skeleton.bones.size
        animation.skeleton = skeleton.ref
        animation.rotations =
            FloatArray(4 * animation.boneCount * animation.frameCount) { if (it.and(3) == 3) 1f else 0f }
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
        scene.add(MeshComponent(flatCube, AutoTileableMaterial()))
        scene.add(MeshComponent(flatCube, PlanarMaterial()))
        // todo bug: DecalMaterial is causing issues :/
        // scene.add(MeshComponent(flatCube, DecalMaterial()))
        scene.add(MeshComponent(flatCube, TriplanarMaterial()))
        // needs clone, because it modifies the mesh by setting procedural length
        // todo calling .clone() on flatCube isn't enough to prevent RaycastMeshTest from failing, we need .scaled()???
        scene.add(FurMeshComponent(Shapes.flatCube.scaled(1f).front))
        val testWorld = TestWorld()
        scene.add(testWorld.createRaytracingMesh(0, 0, 0, 8, 8, 8))
        scene.add(testWorld.createRaytracingMeshV2(0, 0, 0, 8, 8, 8))
        scene.add(MeshComponent(flatCube, Material().apply { pipelineStage = TRANSPARENT_PASS }))
        return scene
    }

    @Test
    fun runTest() {
        OfficialExtensions.register()
        ExtensionLoader.load()
        HiddenOpenGLContext.createOpenGL()
        val printResults = false
        val dst = desktop.getChild("CompileTest")
        if (printResults) dst.tryMkdirs()
        val ui = UITests()
        val scene = createTestScene()
        val rv = object : RenderView(PlayMode.EDITING, style) {
            override fun getWorld() = scene
        }
        scene.forAll { entityOrComponent ->
            if (entityOrComponent is OnUpdate) {
                entityOrComponent.onUpdate()
            }
        }
        ui.prepareUI(rv)
        rv.setPosSize(0, 0, UITests.osWindow.width, UITests.osWindow.height)
        val tmp = Framebuffer("tmp", rv.width, rv.height, 1, TargetType.UInt8x4, DepthBufferType.NONE)
        for (mode in RenderMode.values) {
            try {
                rv.renderMode = mode
                if (printResults) {
                    useFrame(tmp) {
                        rv.draw(0, 0, rv.width, rv.height)
                    }
                    val childName = "${mode.nameDesc.englishName.toAllowedFilename()}.png"
                    tmp.getTexture0().write(dst.getChild(childName), true)
                } else {
                    rv.draw(0, 0, rv.width, rv.height)
                }
            } catch (e: Exception) {
                throw Exception(mode.nameDesc.englishName, e)
            }
        }
        Engine.requestShutdown()
    }
}
