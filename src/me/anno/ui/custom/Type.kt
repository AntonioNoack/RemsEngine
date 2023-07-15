package me.anno.ui.custom

import me.anno.ui.Panel

/**
 * Panel type for CustomContainer: Library = Array<Type>.
 * */
class Type(val displayName: String, val constructor: () -> Panel) {
    private val iName = lazy { constructor().javaClass.simpleName }
    val internalName: String get() = iName.value
}