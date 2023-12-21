package me.anno.tests.game.creeperworld

import me.anno.language.translation.NameDesc

class RockType(
    val id: Int, val name: NameDesc,
    val properties: Map<RockProperty, Float>,
    val color: Int
)