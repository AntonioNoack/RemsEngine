package me.anno.ui.editor.stacked

import me.anno.studio.Inspectable

class Option private constructor(
    val title: String,
    val description: String,
    val value0: Inspectable?,
    val generator: () -> Inspectable
) {

    constructor(title: String, tooltipText: String, value0: Inspectable) : this(
        title, tooltipText, value0,
        { value0 }
    )

    constructor(title: String, tooltipText: String, generator: () -> Inspectable) : this(
        title, tooltipText, null, generator
    )

}