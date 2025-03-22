package me.anno.tests.engine

import me.anno.Engine
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponentsInChildren
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.text.MeshTextComponent
import me.anno.ecs.components.text.SDFTextComponent
import me.anno.ecs.components.text.TextAlignmentY
import me.anno.ecs.components.text.TextComponent
import me.anno.ecs.components.text.TextureTextComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.RenderView1
import me.anno.fonts.Font
import me.anno.gpu.FinalRendering
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.jvm.HiddenOpenGLContext
import me.anno.maths.Maths.sq
import me.anno.ui.base.components.AxisAlignment
import me.anno.utils.Color.r
import me.anno.utils.OS
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertContains
import me.anno.utils.structures.lists.Lists.all2
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.math.sqrt

// todo this is still kind of wonky, especially with Arial
//   we'd also with for a difference < 20, not 60-120
class TextComponentImplTests {

    val text = "Sample" // not actually used
    val font = Font("Verdana", 200f, isBold = true, isItalic = false)

    val h = 1024
    val w = (h * 2.0).toInt()

    /**
     * Render sample text using SDF, Mesh and Texture, and compare them.
     * */
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testImagesAreSimilar() {
        Engine.cancelShutdown()
        OfficialExtensions.initForTests()
        HiddenOpenGLContext.createOpenGL()
        val black = IntImage(w, h, false)
        val white = IntImage(w, h, false).apply { data.fill(-1) }
        val sdf = generateSDFTextComponent()
        val mesh = generateMeshTextComponent()
        val text = generateTextureTextComponent()
        assertContains(getImageDifference(black, sdf), expectedBlackLevels)
        assertContains(getImageDifference(black, mesh), expectedBlackLevels)
        assertContains(getImageDifference(black, text), expectedBlackLevels)
        assertContains(getImageDifference(white, sdf), expectedWhiteLevels)
        assertContains(getImageDifference(white, mesh), expectedWhiteLevels)
        assertContains(getImageDifference(white, text), expectedWhiteLevels)
        assertContains(getImageDifference(sdf, mesh), expectedDifference)
        assertContains(getImageDifference(mesh, text), expectedDifference)
        assertContains(getImageDifference(text, sdf), expectedDifference)
    }

    // these values are very font dependent
    val expectedBlackLevels = 135f..170f
    val expectedWhiteLevels = 190f..230f
    val expectedDifference = 0f..142f // ideally, this would be zero

    fun getImageDifference(a: Image, b: Image): Float {
        var error = 0f
        a.forEachPixel { x, y ->
            error += sq(a.getRGB(x, y).r() - b.getRGB(x, y).r())
        }
        val diff = sqrt(error / (a.width * a.height))
        println("Difference: $diff")
        return diff
    }

    fun generateSDFTextComponent(): Image {
        return testTextComponent(SDFTextComponent(text, font, AxisAlignment.CENTER))
    }

    fun generateMeshTextComponent(): Image {
        return testTextComponent(MeshTextComponent(text, font, AxisAlignment.CENTER))
    }

    fun generateTextureTextComponent(): Image {
        // width depends on size... why??
        // because we're using an SDF-like-shader to improve visual quality
        return testTextComponent(TextureTextComponent(text, font, AxisAlignment.CENTER))
    }

    fun createScene(component: TextComponent): Entity {

        // prepare
        val scene = Entity("Scene")
            .add(SkyboxBase())

        for ((x, alignmentX) in AxisAlignment.entries.withIndex()) {
            if (alignmentX == AxisAlignment.FILL) continue
            for ((y, alignmentY) in TextAlignmentY.entries.withIndex()) {
                val clone = (component as Component).clone() as Component
                clone as TextComponent
                clone.alignmentX = alignmentX
                clone.alignmentY = alignmentY
                clone.text = alignmentX.name
                Entity(scene)
                    .setPosition((x - 1.0) * 2.2, (y - 1.5) * 0.8, 0.0)
                    .add(clone)
            }
        }

        return scene
    }

    private fun createRenderView(scene: Entity): RenderView {
        val rv = RenderView1(PlayMode.PLAYING, scene, style)
        rv.onUpdate()
        rv.calculateSize(w, h)
        rv.setPosSize(0, 0, w, h)
        rv.radius = 2.3f
        rv.orbitRotation.identity()
        rv.updateEditorCameraTransform()
        rv.renderMode = RenderMode.COLOR
        return rv
    }

    fun testTextComponent(component: TextComponent): Image {

        val scene = createScene(component)
        Systems.world = scene
        Systems.onUpdate()

        // render it
        val rv = createRenderView(scene)

        // wait on textures
        when (component) {
            is TextureTextComponent -> {
                val allComponents = scene.getComponentsInChildren(TextComponent::class)
                Sleep.waitUntil(true) {
                    allComponents.all2 { (it as TextureTextComponent).getTexture() != null }
                }
            }
        }

        val fb = Framebuffer("testTextComponent", w, h, TargetType.UInt8x4, DepthBufferType.TEXTURE)

        // loop to render & wait on SDF
        Sleep.waitUntil(true) {
            FinalRendering.runFinalRendering {
                useFrame(fb) {
                    Systems.onUpdate()
                    rv.draw(0, 0, w, h)
                }
                Thread.sleep(10) // give SDF processing queue a little chance
            } == null
        }

        val image = fb.createImage(flipY = true, withAlpha = false)!!
        fb.destroy()
        if (false) {
            val folder = OS.desktop.getChild("TextComponent")
            folder.tryMkdirs()
            image.write(folder.getChild("${(component as Component).className}.png"))
        }
        return image
    }
}