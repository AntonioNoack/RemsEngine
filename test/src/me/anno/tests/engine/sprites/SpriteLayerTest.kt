package me.anno.tests.engine.sprites

import me.anno.ecs.Entity
import me.anno.ecs.components.sprite.SpriteLayer
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.pictures
import me.anno.utils.assertions.assertEquals

fun buildSampleWorld(sprites: SpriteLayer) {
    val charToId = mapOf(
        'D' to 5 * 16 + 2,
        'G' to 4 * 16 + 2,
        'L' to 11 * 16 + 13,
        'B' to 15 * 16 + 0
    )
    val visualMap = """
             B
            BBB 
           BBBBB
           BBBBB
             L
             L
           GGGGGG         GGG
        GGGDDDDDDGGGGGGGGGDDDGGGG
        DDDDDDDDDDDDDDDDDDDDDDDDD
        DDDDDDDDDDDDDDDDDDDDDDDDD
    """.trimIndent()
    val lines = visualMap.lines()
    for ((li, line) in lines.withIndex()) {
        val y = lines.lastIndex - li // text goes down with higher index, but our world grows upwards -> reverse y
        for ((x, char) in line.withIndex()) {
            val id = charToId[char] ?: continue
            sprites.setSprite(x, y, id)
        }
    }
}

fun main() {
    OfficialExtensions.initForTests()
    val scene = Entity()
    val textureFile = pictures.getChild("Textures/poloviiinkin.png")
    val mainLayer = SpriteLayer().apply {
        material.diffuseMap = textureFile
        material.numTiles.set(16, 32)
    }
    buildSampleWorld(mainLayer)
    Entity("Main", scene)
        .add(mainLayer)
    val skyLayer = SpriteLayer().apply {
        material.diffuseMap = textureFile
        material.numTiles.set(16, 32)
    }
    val skySprite = 19 * 16 + 2
    for (y in -1 until 5) {
        for (x in -2 until 3) {
            skyLayer.setSprite(x, y, skySprite)
            assertEquals(skySprite, skyLayer.getSprite(x, y))
        }
    }
    Entity("Sky", scene)
        .add(skyLayer)
        .setPosition(0.0, 0.0, -8.0)
        .setScale(8.0)
    testSceneWithUI("Sprites", scene)
}