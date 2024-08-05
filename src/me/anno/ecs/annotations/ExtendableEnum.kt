package me.anno.ecs.annotations

import me.anno.language.translation.NameDesc

/**
 * needs a companion object with a property called "values"
 * */
interface ExtendableEnum {
    val nameDesc: NameDesc
    val values: List<ExtendableEnum>
}
