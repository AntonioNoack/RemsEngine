package me.anno.tests.engine.sprites

import me.anno.ecs.components.sprite.SpriteChunk
import me.anno.ecs.components.sprite.SpriteLayer
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.max

class SpriteLayerTests {

    fun getSprite(x: Int, y: Int): Int = max(x + y * 7, -1)

    val xRange = -1..4
    val yRange = -7..5

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

    // todo it would be nice to validate rendering
}