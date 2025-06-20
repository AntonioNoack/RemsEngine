package me.anno.games.creeperworld

import me.anno.image.ImageCache
import me.anno.language.translation.NameDesc
import me.anno.utils.OS.res
import me.anno.utils.structures.lists.Lists.createArrayList

object RockTypes {

    val skyColor = ImageCache[res.getChild("textures/Clouds.jpg")].waitFor()!!

    val hardness = RockProperty(0.5f, 0f)
    val dissolved = RockProperty(0f, 0f)

    // original stone image by kues1 (https://www.freepik.com/free-photo/stone-texture_1129966.htm),
    // rock and sky are derived from that
    val stoneTexture = ImageCache[res.getChild("textures/Stone.png")].waitFor()!!
    val stone = RockType(1, NameDesc("Stone"), mapOf(hardness to 0.7f), stoneTexture)
    val rockTexture = ImageCache[res.getChild("textures/Rock.png")].waitFor()!!
    val rock = RockType(2, NameDesc("Rock"), mapOf(hardness to 0.9f), rockTexture)
    val clay = RockType(3, NameDesc("Clay"), mapOf(hardness to 0.1f), 0x889999)

    // todo gravity for sand
    val sand = RockType(4, NameDesc("Sand"), mapOf(hardness to 0f), 0xffeeaa)

    val rockTypes = listOf(stone, rock, clay, sand)
    val rockById = rockTypes.associateBy(RockType::id)
    val rockTextures = createArrayList(rockTypes.maxOf(RockType::id) + 1) { id ->
        rockById[id]?.texture ?: skyColor
    }
}