package me.anno.tests.engine.sprites

import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.sprite.SpriteChunk
import me.anno.ecs.components.sprite.SpriteLayer
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView1
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.texture.TextureCache
import me.anno.image.raw.IntImage
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.jvm.HiddenOpenGLContext
import me.anno.utils.Color.black
import me.anno.utils.Color.toVecRGB
import me.anno.utils.OS
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2i
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.math.max

class SpriteLayerTests {

    fun getSprite(x: Int, y: Int): Int = max(x * 3 + y + 15, -1)

    val xRange = -6..4
    val yRange = -6..5

    @BeforeEach
    fun init() {
        registerCustomClass(SpriteLayer::class)
        registerCustomClass(SpriteChunk::class)
    }

    fun createLayer(): SpriteLayer {
        val layer = SpriteLayer()
        for (y in yRange) {
            for (x in xRange) {
                layer.setSprite(x, y, getSprite(x, y))
            }
        }
        return layer
    }

    @Test
    fun testSetting() {
        val layer = createLayer()
        for (y in yRange) {
            for (x in xRange) {
                assertEquals(getSprite(x, y), layer.getSprite(x, y))
            }
        }
    }

    @Test
    fun testSerialization() {
        val layer = createLayer()
        val asString = JsonStringWriter.toText(layer, InvalidRef)
        val clone = JsonStringReader.readFirst(asString, InvalidRef, SpriteLayer::class)
        for (y in yRange) {
            for (x in xRange) {
                assertEquals(getSprite(x, y), clone.getSprite(x, y))
            }
        }
    }

    @Test
    fun testCloning() {
        val layer = createLayer()
        val clone = layer.clone() as SpriteLayer
        for (y in yRange) {
            for (x in xRange) {
                assertEquals(getSprite(x, y), clone.getSprite(x, y))
            }
        }
    }

    fun createLayerTextureRef(): IntImage {
        val tex = IntImage(7, 7, false)
        tex.forEachPixel { x, y ->
            tex.setRGB(x, y, getSprite(x, y) * 0x010305)
        }
        return tex
    }

    /**
     * validate rendering: create a SpriteLayer, render its color,
     * and verify everything is rendered as expected
     * */
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testRendering() {
        HiddenOpenGLContext.createOpenGL()
        OfficialExtensions.initForTests()

        val layerTexture = createLayerTextureRef()
        val layerTextureRef = layerTexture.ref
        Sleep.waitUntilDefined(true) { // ensure texture atlas is created
            TextureCache.getTextureArray(layerTextureRef, Vector2i(7, 7))
        }

        val layer = createLayer()
        layer.material.diffuseMap = layerTextureRef
        layer.material.numTiles.set(7, 7)

        val scene = Entity("Scene")
            .add(SkyboxBase().apply { 0x003355.toVecRGB(skyColor).mul(3f) })
            .add(layer)

        assertTrue(layer.invalidChunks.isNotEmpty())
        Systems.world = scene
        Systems.onUpdate() // ensure chunks are valid
        assertTrue(layer.invalidChunks.isEmpty())

        val rv = RenderView1(PlayMode.PLAYING, scene, style)
        val cells = 14
        val sizePerCell = 8
        val size = cells * sizePerCell
        rv.onUpdate()
        rv.calculateSize(size, size)
        rv.setPosSize(0, 0, size, size)
        rv.radius = cells * 0.5f
        rv.orbitRotation.identity()
        rv.updateEditorCameraTransform()
        rv.renderMode = RenderMode.COLOR
        val fb = Framebuffer("spriteTest", size, size, TargetType.UInt8x4, DepthBufferType.TEXTURE)
        for (i in 0 until 3) useFrame(fb) { // todo why are three frames necessary???
            rv.draw(0, 0, size, size)
        }
        val asImage = fb.getTexture0()
            .createImage(flipY = false, withAlpha = false)
        if (false) asImage.write(OS.desktop.getChild("sprites.png"))
        val expectedImage = IntImage(asImage.width, asImage.height, true)
        var wrongPixels = 0
        for (yi in 0 until cells) {
            for (xi in 0 until cells) {
                val sx = (xi - cells / 2)
                val sy = (yi - cells / 2)
                val spriteId = getSprite(sx, sy)
                val expectedColor =
                    if (spriteId < 0 || sx !in xRange || sy !in yRange) black
                    else layerTexture.getRGB(spriteId % 7, spriteId / 7)
                for (yii in 0 until sizePerCell) {
                    for (xii in 0 until sizePerCell) {
                        val x = xi * sizePerCell + xii
                        val y = size - 1 - (yi * sizePerCell + yii)
                        val actualColor = asImage.getRGB(x, y)
                        expectedImage.setRGB(x, y, expectedColor)
                        if (expectedColor != actualColor) {
                            wrongPixels++
                        }
                    }
                }
            }
        }
        assertEquals(0, wrongPixels)
    }
}