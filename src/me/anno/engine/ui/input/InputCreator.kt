package me.anno.engine.ui.input

import me.anno.ecs.annotations.Range
import me.anno.engine.inspector.IProperty
import me.anno.language.translation.NameDesc
import me.anno.ui.Panel
import me.anno.ui.Style

fun interface InputCreator {
    fun createInput(
        nameDesc: NameDesc, visibilityKey: String, value: Any?, default: Any?,
        property: IProperty<Any?>, range: Range?, style: Style,
    ): Panel
}