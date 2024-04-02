package me.anno.ui.editor.stacked

import me.anno.engine.inspector.Inspectable
import me.anno.language.translation.NameDesc

class Option private constructor(
    val nameDesc: NameDesc,
    val value0: Inspectable?,
    val generator: () -> Inspectable
) {

    constructor(nameDesc: NameDesc, value0: Inspectable) : this(nameDesc, value0, { value0 })
    constructor(nameDesc: NameDesc, generator: () -> Inspectable) : this(nameDesc, null, generator)

    fun getSample(): Inspectable = value0 ?: generator()
}