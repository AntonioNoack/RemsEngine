package me.anno.recast

import org.recast4j.recast.AreaModification

@Suppress("unused")
object DefaultAreaModifications {
    const val TYPE_MASK = 0x07
    const val TYPE_GROUND = 0x1
    const val TYPE_WATER = 0x2
    const val TYPE_ROAD = 0x3
    const val TYPE_DOOR = 0x4
    const val TYPE_GRASS = 0x5
    const val TYPE_JUMP = 0x6

    // todo how does this work, and what would be good defaults, that would be usable (nearly) everywhere?
    val GROUND = AreaModification(TYPE_GROUND, TYPE_MASK)
    val WATER = AreaModification(TYPE_WATER, TYPE_MASK)
    val ROAD = AreaModification(TYPE_ROAD, TYPE_MASK)
    val GRASS = AreaModification(TYPE_GRASS, TYPE_MASK)
    val DOOR = AreaModification(TYPE_DOOR, TYPE_DOOR)
    val JUMP = AreaModification(TYPE_JUMP, TYPE_JUMP)
    const val FLAG_WALK = 0x01 // Ability to walk (ground, grass, road)
    const val FLAG_SWIM = 0x02 // Ability to swim (water).
    const val FLAG_DOOR = 0x04 // Ability to move through doors.
    const val FLAG_JUMP = 0x08 // Ability to jump.
    const val FLAG_DISABLED = 0x10 // Disabled polygon
    const val FLAG_ALL = 0xffff // All abilities.
}
