package me.anno.ui.custom

import me.anno.ui.Panel

/**
 * Panel type for CustomContainer: Library = Array<Type>.
 * */
class CustomPanelType(val displayName: String, val generator: () -> Panel) {
    val internalName: String by lazy {
        generator()::class.simpleName ?: "?"
    }
}